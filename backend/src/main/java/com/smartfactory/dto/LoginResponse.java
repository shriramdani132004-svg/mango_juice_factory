package com.smartfactory.dto;

public record LoginResponse(
    String token,
    String tokenType,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String username,
        String displayName,
        String role,
        Boolean active
    ) {}
}
