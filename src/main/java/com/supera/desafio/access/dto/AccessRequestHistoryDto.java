package com.supera.desafio.access.dto;

import com.supera.desafio.core.domain.history.AccessRequestHistoryEventType;
import java.time.Instant;

public record AccessRequestHistoryDto(Instant createdAt, AccessRequestHistoryEventType eventType, String description) {
}
