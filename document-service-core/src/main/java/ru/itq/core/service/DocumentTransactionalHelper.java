package ru.itq.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itq.api.dto.BatchOperationResult;
import ru.itq.api.dto.DocumentStatus;
import ru.itq.api.dto.HistoryAction;
import ru.itq.core.domain.entity.Document;
import ru.itq.core.domain.entity.DocumentHistory;
import ru.itq.core.domain.exception.RegistryException;
import ru.itq.core.persistence.DocumentHistoryRepository;
import ru.itq.core.persistence.DocumentRepository;

import java.time.LocalDateTime;

/**
 * Separate bean so that REQUIRES_NEW propagation works correctly
 * (Spring proxy-based AOP requires cross-bean calls for new transactions).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTransactionalHelper {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final ApprovalRegistryService approvalRegistryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchOperationResult submitSingle(Long id, String initiator) {
        try {
            Document document = documentRepository.findById(id).orElse(null);
            if (document == null) {
                return BatchOperationResult.notFound(id);
            }
            if (document.getStatus() != DocumentStatus.DRAFT) {
                return BatchOperationResult.conflict(id,
                        "Expected DRAFT, got " + document.getStatus());
            }

            document.setStatus(DocumentStatus.SUBMITTED);
            documentRepository.save(document);

            DocumentHistory history = DocumentHistory.builder()
                    .documentId(id)
                    .performedBy(initiator)
                    .action(HistoryAction.SUBMIT)
                    .comment("Submitted by " + initiator)
                    .build();
            documentHistoryRepository.save(history);

            log.debug("Document id={} submitted by {}", id, initiator);
            return BatchOperationResult.success(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            return BatchOperationResult.conflict(id, "Concurrent modification detected");
        } catch (Exception e) {
            log.error("Error submitting document id={}: {}", id, e.getMessage(), e);
            return BatchOperationResult.conflict(id, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchOperationResult approveSingle(Long id, String initiator) {
        try {
            Document document = documentRepository.findById(id).orElse(null);
            if (document == null) {
                return BatchOperationResult.notFound(id);
            }
            if (document.getStatus() != DocumentStatus.SUBMITTED) {
                return BatchOperationResult.conflict(id,
                        "Expected SUBMITTED, got " + document.getStatus());
            }

            document.setStatus(DocumentStatus.APPROVED);
            documentRepository.save(document);

            DocumentHistory history = DocumentHistory.builder()
                    .documentId(id)
                    .performedBy(initiator)
                    .action(HistoryAction.APPROVE)
                    .comment("Approved by " + initiator)
                    .build();
            documentHistoryRepository.save(history);

            approvalRegistryService.createEntry(id, initiator, LocalDateTime.now());

            log.debug("Document id={} approved by {}", id, initiator);
            return BatchOperationResult.success(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            return BatchOperationResult.conflict(id, "Concurrent modification detected");
        } catch (RegistryException e) {
            log.error("Registry error approving document id={}: {}", id, e.getMessage());
            return BatchOperationResult.registryError(id, e.getMessage());
        } catch (Exception e) {
            log.error("Error approving document id={}: {}", id, e.getMessage(), e);
            return BatchOperationResult.registryError(id, e.getMessage());
        }
    }
}
