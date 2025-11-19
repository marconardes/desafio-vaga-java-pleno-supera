package com.supera.desafio.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AccessRequestCreateRequest(
        @NotEmpty(message = "Informe ao menos um módulo")
        @Size(min = 1, max = 3, message = "Selecione entre 1 e 3 módulos")
        List<Long> moduleIds,

        @NotBlank(message = "Justificativa é obrigatória")
        @Size(min = 20, max = 500, message = "Justificativa deve ter entre 20 e 500 caracteres")
        String justification,

        boolean urgent
) {
}
