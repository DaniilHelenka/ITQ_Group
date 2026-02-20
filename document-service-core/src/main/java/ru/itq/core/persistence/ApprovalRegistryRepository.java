package ru.itq.core.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itq.core.domain.entity.ApprovalRegistry;

import java.util.Optional;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {

    Optional<ApprovalRegistry> findByDocumentId(Long documentId);

    boolean existsByDocumentId(Long documentId);
}
