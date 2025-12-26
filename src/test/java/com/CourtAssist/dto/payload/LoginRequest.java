package com.CourtAssist.dto.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    // Getters and setters
    @NotBlank(message = "Username, email or mobile number is required")
    private String username; // This field now accepts username, email, or mobile

    @NotBlank(message = "Password is required")
    private String password;

}