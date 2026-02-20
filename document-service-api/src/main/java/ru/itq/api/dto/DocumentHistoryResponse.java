package ru.itq.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentHistoryResponse {

    private Long id;
    private Long documentId;
    private String performedBy;
    private HistoryAction action;
    private String comment;
    private LocalDateTime createdAt;
}
