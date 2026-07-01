package com.enlaza.security.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
