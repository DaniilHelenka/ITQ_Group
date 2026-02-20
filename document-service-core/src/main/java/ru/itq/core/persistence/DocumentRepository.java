package ru.itq.core.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itq.api.dto.DocumentStatus;
import ru.itq.core.domain.entity.Document;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findAllByIdIn(List<Long> ids);

    Page<Document> findAllByIdIn(List<Long> ids, Pageable pageable);

    @Query(value = "SELECT nextval('document_number_seq')", nativeQuery = true)
    Long getNextDocumentNumber();

    @Query(value = """
            SELECT * FROM document
            WHERE status = :status
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Document> findBatchForProcessing(@Param("status") String status, @Param("limit") int limit);

    long countByStatus(DocumentStatus status);
}
