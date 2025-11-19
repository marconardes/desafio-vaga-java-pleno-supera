package com.supera.desafio.access.dto;

import com.supera.desafio.core.domain.request.AccessRequestStatus;
import java.time.Instant;
import java.util.List;

public record AccessRequestResponse(
        Long id,
        String protocol,
        AccessRequestStatus status,
        boolean urgent,
        Instant requestedAt,
        Instant expiresAt,
        String justification,
        String deniedReason,
        List<ModuleDto> modules,
        List<AccessRequestHistoryDto> history
) {
}
