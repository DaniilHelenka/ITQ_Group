package ru.itq.core.persistence;

import org.springframework.data.jpa.domain.Specification;
import ru.itq.api.dto.DocumentStatus;
import ru.itq.core.domain.entity.Document;

import java.time.LocalDateTime;

public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    public static Specification<Document> hasStatus(DocumentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Document> hasAuthor(String author) {
        return (root, query, cb) -> author == null || author.isBlank()
                ? null
                : cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    public static Specification<Document> createdAfter(LocalDateTime dateFrom) {
        return (root, query, cb) -> dateFrom == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
    }

    public static Specification<Document> createdBefore(LocalDateTime dateTo) {
        return (root, query, cb) -> dateTo == null
                ? null
                : cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
    }
}
