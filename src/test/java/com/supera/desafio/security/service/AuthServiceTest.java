package com.supera.desafio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.core.domain.user.SystemUser;
import com.supera.desafio.core.repository.SystemUserRepository;
import com.supera.desafio.security.dto.AuthRequest;
import com.supera.desafio.security.dto.AuthResponse;
import com.supera.desafio.security.model.AuthenticatedUser;
import com.supera.desafio.security.support.InvalidCredentialsException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private SystemUserRepository repository;
    private PasswordEncoder encoder;
    private JwtTokenService tokenService;
    private AuthService service;

    @BeforeEach
    void setup() {
        repository = mock(SystemUserRepository.class);
        encoder = mock(PasswordEncoder.class);
        tokenService = mock(JwtTokenService.class);
        service = new AuthService(repository, encoder, tokenService);
    }

    @Test
    void shouldAuthenticateUser() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setEmail("user@corp.com");
        user.setFullName("User");
        user.setDepartment(DepartmentType.TI);
        user.setPassword("hash");
        when(repository.findByEmailIgnoreCase(eq("user@corp.com"))).thenReturn(Optional.of(user));
        when(encoder.matches("secret", "hash")).thenReturn(true);
        when(tokenService.generateToken(org.mockito.ArgumentMatchers.any(AuthenticatedUser.class))).thenReturn("token");
        when(tokenService.expirationSeconds()).thenReturn(900L);

        AuthResponse response = service.authenticate(new AuthRequest("user@corp.com", "secret"));
        assertEquals("token", response.accessToken());
    }

    @Test
    void shouldFailWhenUserNotFound() {
        when(repository.findByEmailIgnoreCase(eq("missing@corp.com"))).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(new AuthRequest("missing@corp.com", "123")));
    }

    @Test
    void shouldFailWhenPasswordInvalid() {
        SystemUser user = new SystemUser();
        user.setEmail("user@corp.com");
        user.setPassword("hash");
        when(repository.findByEmailIgnoreCase(eq("user@corp.com"))).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(new AuthRequest("user@corp.com", "wrong")));
    }
}
