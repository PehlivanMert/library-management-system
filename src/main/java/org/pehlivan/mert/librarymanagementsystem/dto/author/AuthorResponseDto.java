package org.pehlivan.mert.librarymanagementsystem.dto.author;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponseDto {
    private String name;
    private String surname;
} 