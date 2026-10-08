package com.resumeanalyzer.auth.dto;

import com.resumeanalyzer.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
