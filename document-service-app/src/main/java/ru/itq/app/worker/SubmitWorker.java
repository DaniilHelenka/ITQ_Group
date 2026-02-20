package ru.itq.app.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itq.api.dto.BatchOperationResult;
import ru.itq.api.dto.DocumentStatus;
import ru.itq.app.config.WorkerProperties;
import ru.itq.core.service.DocumentService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitWorker {

    private final DocumentService documentService;
    private final WorkerProperties workerProperties;

    @Scheduled(fixedDelayString = "${app.worker.submit-delay:30000}")
    public void processSubmitBatch() {
        long start = System.currentTimeMillis();

        List<Long> ids = documentService.getDocumentIdsForSubmit(workerProperties.getBatchSize());

        if (ids.isEmpty()) {
            return;
        }

        long remaining = documentService.countByStatus(DocumentStatus.DRAFT) - ids.size();
        log.info("SUBMIT-worker: processing batch of {} documents, ~{} DRAFT remaining",
                ids.size(), Math.max(0, remaining));

        List<BatchOperationResult> results = documentService.submitBatch(ids, "SUBMIT-worker");

        long successCount = results.stream().filter(r -> "success".equals(r.getResult())).count();
        long elapsed = System.currentTimeMillis() - start;

        log.info("SUBMIT-worker: batch completed — processed={}, success={}, elapsed={}ms",
                ids.size(), successCount, elapsed);
    }
}
