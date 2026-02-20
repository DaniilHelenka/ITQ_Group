package ru.itq.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itq.api.dto.*;
import ru.itq.core.domain.entity.Document;
import ru.itq.core.domain.entity.DocumentHistory;
import ru.itq.core.domain.exception.DocumentNotFoundException;
import ru.itq.core.persistence.DocumentHistoryRepository;
import ru.itq.core.persistence.DocumentRepository;
import ru.itq.core.persistence.DocumentSpecifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final DocumentTransactionalHelper txHelper;

    @Override
    @Transactional
    public DocumentResponse create(String author, String title, String initiator) {
        Long seqNum = documentRepository.getNextDocumentNumber();
        String documentNumber = String.format("DOC-%07d", seqNum);

        Document document = Document.builder()
                .documentNumber(documentNumber)
                .author(author)
                .title(title)
                .status(DocumentStatus.DRAFT)
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document created: number={}, author={}, initiator={}", documentNumber, author, initiator);
        return toResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        List<DocumentHistory> history = documentHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(id);
        return toResponse(document, history);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getByIds(List<Long> ids, Pageable pageable) {
        return documentRepository.findAllByIdIn(ids, pageable)
                .map(doc -> toResponse(doc, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> search(DocumentStatus status, String author,
                                         LocalDateTime dateFrom, LocalDateTime dateTo,
                                         Pageable pageable) {
        Specification<Document> spec = Specification
                .where(DocumentSpecifications.hasStatus(status))
                .and(DocumentSpecifications.hasAuthor(author))
                .and(DocumentSpecifications.createdAfter(dateFrom))
                .and(DocumentSpecifications.createdBefore(dateTo));

        return documentRepository.findAll(spec, pageable)
                .map(doc -> toResponse(doc, null));
    }

    @Override
    public List<BatchOperationResult> submitBatch(List<Long> ids, String initiator) {
        long start = System.currentTimeMillis();
        List<BatchOperationResult> results = new ArrayList<>();

        for (Long id : ids) {
            results.add(txHelper.submitSingle(id, initiator));
        }

        long elapsed = System.currentTimeMillis() - start;
        long successCount = results.stream().filter(r -> "success".equals(r.getResult())).count();
        log.info("Batch SUBMIT completed: total={}, success={}, elapsed={}ms", ids.size(), successCount, elapsed);
        return results;
    }

    @Override
    public List<BatchOperationResult> approveBatch(List<Long> ids, String initiator) {
        long start = System.currentTimeMillis();
        List<BatchOperationResult> results = new ArrayList<>();

        for (Long id : ids) {
            results.add(txHelper.approveSingle(id, initiator));
        }

        long elapsed = System.currentTimeMillis() - start;
        long successCount = results.stream().filter(r -> "success".equals(r.getResult())).count();
        long conflictCount = results.stream().filter(r -> "conflict".equals(r.getResult())).count();
        long errorCount = results.stream().filter(r -> "registry_error".equals(r.getResult())).count();
        log.info("Batch APPROVE completed: total={}, success={}, conflict={}, registryErrors={}, elapsed={}ms",
                ids.size(), successCount, conflictCount, errorCount, elapsed);
        return results;
    }

    @Override
    public BatchOperationResult approveSingle(Long id, String initiator) {
        return txHelper.approveSingle(id, initiator);
    }

    @Override
    @Transactional
    public List<Long> getDocumentIdsForSubmit(int batchSize) {
        return documentRepository.findBatchForProcessing(DocumentStatus.DRAFT.name(), batchSize)
                .stream()
                .map(Document::getId)
                .toList();
    }

    @Override
    @Transactional
    public List<Long> getDocumentIdsForApprove(int batchSize) {
        return documentRepository.findBatchForProcessing(DocumentStatus.SUBMITTED.name(), batchSize)
                .stream()
                .map(Document::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(DocumentStatus status) {
        return documentRepository.countByStatus(status);
    }

    private DocumentResponse toResponse(Document doc, List<DocumentHistory> history) {
        DocumentResponse.DocumentResponseBuilder builder = DocumentResponse.builder()
                .id(doc.getId())
                .documentNumber(doc.getDocumentNumber())
                .author(doc.getAuthor())
                .title(doc.getTitle())
                .status(doc.getStatus())
                .version(doc.getVersion())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt());

        if (history != null) {
            builder.history(history.stream().map(this::toHistoryResponse).toList());
        }

        return builder.build();
    }

    private DocumentHistoryResponse toHistoryResponse(DocumentHistory h) {
        return DocumentHistoryResponse.builder()
                .id(h.getId())
                .documentId(h.getDocumentId())
                .performedBy(h.getPerformedBy())
                .action(h.getAction())
                .comment(h.getComment())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
