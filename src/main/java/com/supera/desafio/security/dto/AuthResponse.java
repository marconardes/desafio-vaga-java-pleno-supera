package com.supera.desafio.security.dto;

import com.supera.desafio.security.model.AuthenticatedUser;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUser user
) {
}
