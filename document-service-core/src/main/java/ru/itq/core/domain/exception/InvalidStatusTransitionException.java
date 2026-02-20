package ru.itq.core.domain.exception;

import ru.itq.api.dto.DocumentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Long documentId, DocumentStatus current, DocumentStatus target) {
        super("Invalid status transition for document id=" + documentId
                + ": " + current + " -> " + target);
    }
}
