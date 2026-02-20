package ru.itq.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itq.api.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentService {

    DocumentResponse create(String author, String title, String initiator);

    DocumentResponse getById(Long id);

    Page<DocumentResponse> getByIds(List<Long> ids, Pageable pageable);

    Page<DocumentResponse> search(DocumentStatus status, String author,
                                  LocalDateTime dateFrom, LocalDateTime dateTo,
                                  Pageable pageable);

    List<BatchOperationResult> submitBatch(List<Long> ids, String initiator);

    List<BatchOperationResult> approveBatch(List<Long> ids, String initiator);

    BatchOperationResult approveSingle(Long id, String initiator);

    List<Long> getDocumentIdsForSubmit(int batchSize);

    List<Long> getDocumentIdsForApprove(int batchSize);

    long countByStatus(DocumentStatus status);
}
