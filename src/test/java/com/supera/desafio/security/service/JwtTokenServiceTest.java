package com.supera.desafio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.security.config.JwtProperties;
import com.supera.desafio.security.model.AuthenticatedUser;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateAndParseTokenWithShortSecret() {
        JwtTokenService service = new JwtTokenService(new JwtProperties("short-secret", 15));
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@corp.com", "User", DepartmentType.TI);
        String token = service.generateToken(user);
        assertNotNull(token);
        AuthenticatedUser parsed = service.parse(token);
        assertEquals(user.id(), parsed.id());
        assertEquals(user.department(), parsed.department());
    }

    @Test
    void shouldGenerateAndParseTokenWithLongSecret() {
        JwtTokenService service = new JwtTokenService(new JwtProperties("this-is-a-very-long-secret-value-that-exceeds-thirty-two-bytes", 15));
        AuthenticatedUser user = new AuthenticatedUser(2L, "other@corp.com", "Another", DepartmentType.FINANCEIRO);
        String token = service.generateToken(user);
        AuthenticatedUser parsed = service.parse(token);
        assertEquals(user.email(), parsed.email());
    }
}
