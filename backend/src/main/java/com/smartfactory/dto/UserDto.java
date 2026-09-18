package com.smartfactory.dto;

public record UserDto(
    Long id,
    String username,
    String displayName,
    String email,
    String role,
    Boolean active,
    String createdAt
) {}
