package org.pehlivan.mert.librarymanagementsystem.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationNotification {
    private String email;
    private String username;
} 