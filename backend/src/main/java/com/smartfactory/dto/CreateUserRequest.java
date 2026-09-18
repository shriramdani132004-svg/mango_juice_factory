package com.smartfactory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100)
    String displayName,

    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Role is required")
    String role
) {}
