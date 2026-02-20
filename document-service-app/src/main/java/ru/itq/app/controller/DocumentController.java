package ru.itq.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itq.api.dto.*;
import ru.itq.core.service.DocumentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse response = documentService.create(
                request.getAuthor(), request.getTitle(), request.getInitiator());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getByIds(
            @RequestParam List<Long> ids,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.getByIds(ids, pageable));
    }

    @PostMapping("/submit")
    public ResponseEntity<List<BatchOperationResult>> submit(@Valid @RequestBody BatchOperationRequest request) {
        List<BatchOperationResult> results = documentService.submitBatch(request.getIds(), request.getInitiator());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/approve")
    public ResponseEntity<List<BatchOperationResult>> approve(@Valid @RequestBody BatchOperationRequest request) {
        List<BatchOperationResult> results = documentService.approveBatch(request.getIds(), request.getInitiator());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DocumentResponse>> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.search(status, author, dateFrom, dateTo, pageable));
    }

    @PostMapping("/concurrent-approve-test")
    public ResponseEntity<ConcurrentApproveTestResponse> concurrentApproveTest(
            @Valid @RequestBody ConcurrentApproveTestRequest request) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(request.getThreads());
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        CountDownLatch latch = new CountDownLatch(request.getAttempts());

        for (int i = 0; i < request.getAttempts(); i++) {
            final int attempt = i;
            executor.submit(() -> {
                try {
                    String initiator = "concurrent-tester-" + attempt;
                    BatchOperationResult result = documentService.approveSingle(
                            request.getDocumentId(), initiator);
                    switch (result.getResult()) {
                        case "success" -> successCount.incrementAndGet();
                        case "conflict" -> conflictCount.incrementAndGet();
                        default -> errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        DocumentResponse finalDoc = documentService.getById(request.getDocumentId());

        ConcurrentApproveTestResponse response = ConcurrentApproveTestResponse.builder()
                .successCount(successCount.get())
                .conflictCount(conflictCount.get())
                .errorCount(errorCount.get())
                .finalStatus(finalDoc.getStatus())
                .build();

        log.info("Concurrent approve test: success={}, conflict={}, error={}, finalStatus={}",
                response.getSuccessCount(), response.getConflictCount(),
                response.getErrorCount(), response.getFinalStatus());

        return ResponseEntity.ok(response);
    }
}
