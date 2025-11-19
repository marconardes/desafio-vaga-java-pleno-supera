package com.supera.desafio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.security.model.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUser() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "a@a.com", "User", DepartmentType.TI);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        assertEquals(user, service.requireUser());
    }

    @Test
    void shouldThrowWhenNotAuthenticated() {
        assertThrows(IllegalStateException.class, service::requireUser);
    }

    @Test
    void shouldThrowWhenPrincipalIsNotAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", null));
        assertThrows(IllegalStateException.class, service::requireUser);
    }
}
