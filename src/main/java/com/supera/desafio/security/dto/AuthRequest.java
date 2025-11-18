package com.supera.desafio.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @Email(message = "Informe um e-mail válido") @NotBlank(message = "E-mail é obrigatório") String email,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 6, max = 60, message = "Senha inválida") String password
) {
}
