package ru.itq.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itq.core.domain.entity.ApprovalRegistry;
import ru.itq.core.domain.exception.RegistryException;
import ru.itq.core.persistence.ApprovalRegistryRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRegistryServiceImpl implements ApprovalRegistryService {

    private final ApprovalRegistryRepository approvalRegistryRepository;

    @Override
    public ApprovalRegistry createEntry(Long documentId, String approvedBy, LocalDateTime approvedAt) {
        if (approvalRegistryRepository.existsByDocumentId(documentId)) {
            throw new RegistryException("Approval registry entry already exists for document id=" + documentId);
        }

        try {
            ApprovalRegistry entry = ApprovalRegistry.builder()
                    .documentId(documentId)
                    .approvedBy(approvedBy)
                    .approvedAt(approvedAt)
                    .build();
            ApprovalRegistry saved = approvalRegistryRepository.save(entry);
            log.info("Approval registry entry created for document id={}, approvedBy={}", documentId, approvedBy);
            return saved;
        } catch (Exception e) {
            throw new RegistryException("Failed to create approval registry entry for document id=" + documentId, e);
        }
    }
}
