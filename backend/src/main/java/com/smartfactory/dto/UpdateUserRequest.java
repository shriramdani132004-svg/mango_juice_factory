package com.smartfactory.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
    String displayName,
    String email,
    String role,
    Boolean active
) {}
