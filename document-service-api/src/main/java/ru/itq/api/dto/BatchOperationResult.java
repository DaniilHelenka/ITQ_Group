package ru.itq.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {

    private Long id;
    private String result;
    private String message;

    public static BatchOperationResult success(Long id) {
        return BatchOperationResult.builder().id(id).result("success").build();
    }

    public static BatchOperationResult conflict(Long id, String message) {
        return BatchOperationResult.builder().id(id).result("conflict").message(message).build();
    }

    public static BatchOperationResult notFound(Long id) {
        return BatchOperationResult.builder().id(id).result("not_found").message("Document not found").build();
    }

    public static BatchOperationResult registryError(Long id, String message) {
        return BatchOperationResult.builder().id(id).result("registry_error").message(message).build();
    }
}
