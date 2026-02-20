package ru.itq.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {

    private Long id;
    private String documentNumber;
    private String author;
    private String title;
    private DocumentStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentHistoryResponse> history;
}
