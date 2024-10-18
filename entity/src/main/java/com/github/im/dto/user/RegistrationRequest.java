package com.github.im.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String phoneNumber; // Optional
}
