package com.flowforge.dto.auth;

import com.flowforge.dto.user.UserResponse;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {
}
