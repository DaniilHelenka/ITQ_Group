package ru.itq.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrentApproveTestResponse {

    private int successCount;
    private int conflictCount;
    private int errorCount;
    private DocumentStatus finalStatus;
}
