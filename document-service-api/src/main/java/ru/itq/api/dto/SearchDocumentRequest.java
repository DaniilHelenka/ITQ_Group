package ru.itq.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDocumentRequest {

    private DocumentStatus status;
    private String author;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
