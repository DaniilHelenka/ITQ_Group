package ru.itq.core.service;

import ru.itq.core.domain.entity.ApprovalRegistry;

import java.time.LocalDateTime;

public interface ApprovalRegistryService {

    ApprovalRegistry createEntry(Long documentId, String approvedBy, LocalDateTime approvedAt);
}
