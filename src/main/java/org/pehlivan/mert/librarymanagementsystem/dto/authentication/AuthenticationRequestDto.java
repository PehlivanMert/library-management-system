package org.pehlivan.mert.librarymanagementsystem.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequestDto {

    @NotBlank(message = "Email can't be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password can't be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$", message = "Password must contain at least one letter and one number")
    private String password;
}
