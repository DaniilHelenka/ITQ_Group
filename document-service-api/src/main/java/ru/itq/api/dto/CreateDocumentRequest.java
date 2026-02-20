package ru.itq.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotBlank(message = "Author must not be blank")
    private String author;

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotBlank(message = "Initiator must not be blank")
    private String initiator;
}
