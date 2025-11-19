package com.supera.desafio.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessRequestCancelRequest(
        @NotBlank(message = "Motivo é obrigatório")
        @Size(min = 10, max = 200, message = "Motivo deve ter entre 10 e 200 caracteres")
        String reason
) {
}
