package ru.itq.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationRequest {

    @NotEmpty(message = "IDs list must not be empty")
    @Size(max = 1000, message = "IDs list must not exceed 1000 elements")
    private List<Long> ids;

    @NotBlank(message = "Initiator must not be blank")
    private String initiator;
}
