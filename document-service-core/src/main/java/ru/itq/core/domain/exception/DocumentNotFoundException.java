package ru.itq.core.domain.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Document not found: id=" + id);
    }
}
