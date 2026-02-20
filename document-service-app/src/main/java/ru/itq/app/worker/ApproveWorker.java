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
public class ApproveWorker {

    private final DocumentService documentService;
    private final WorkerProperties workerProperties;

    @Scheduled(fixedDelayString = "${app.worker.approve-delay:30000}")
    public void processApproveBatch() {
        long start = System.currentTimeMillis();

        List<Long> ids = documentService.getDocumentIdsForApprove(workerProperties.getBatchSize());

        if (ids.isEmpty()) {
            return;
        }

        long remaining = documentService.countByStatus(DocumentStatus.SUBMITTED) - ids.size();
        log.info("APPROVE-worker: processing batch of {} documents, ~{} SUBMITTED remaining",
                ids.size(), Math.max(0, remaining));

        List<BatchOperationResult> results = documentService.approveBatch(ids, "APPROVE-worker");

        long successCount = results.stream().filter(r -> "success".equals(r.getResult())).count();
        long elapsed = System.currentTimeMillis() - start;

        log.info("APPROVE-worker: batch completed — processed={}, success={}, elapsed={}ms",
                ids.size(), successCount, elapsed);
    }
}
