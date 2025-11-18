package com.supera.desafio.security.service;

import com.supera.desafio.core.domain.user.SystemUser;
import com.supera.desafio.core.repository.SystemUserRepository;
import com.supera.desafio.security.dto.AuthRequest;
import com.supera.desafio.security.dto.AuthResponse;
import com.supera.desafio.security.model.AuthenticatedUser;
import com.supera.desafio.security.support.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(SystemUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthResponse authenticate(AuthRequest request) {
        SystemUser user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(SystemUser::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Usuário ou senha inválidos");
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getDepartment());
        String token = jwtTokenService.generateToken(principal);
        return new AuthResponse(token, "Bearer", jwtTokenService.expirationSeconds(), principal);
    }
}
