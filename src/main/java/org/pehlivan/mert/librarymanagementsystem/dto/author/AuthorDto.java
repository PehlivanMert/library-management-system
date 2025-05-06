package org.pehlivan.mert.librarymanagementsystem.dto.author;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDto {
    @NotBlank(message = "Author name cannot be blank")
    private String name;

    @NotBlank(message = "Author surname cannot be blank")
    private String surname;
} 