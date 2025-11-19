package com.supera.desafio.core.domain.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.supera.desafio.core.domain.request.AccessRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccessRequestHistoryTest {

    @Test
    void shouldStoreHistoryFields() {
        AccessRequestHistory history = new AccessRequestHistory();
        history.setId(5L);
        AccessRequest request = new AccessRequest();
        history.setRequest(request);
        history.setEventType(AccessRequestHistoryEventType.APROVADO);
        history.setDescription("desc");
        Instant now = Instant.now();
        history.setCreatedAt(now);

        assertEquals(5L, history.getId());
        assertEquals(request, history.getRequest());
        assertEquals(AccessRequestHistoryEventType.APROVADO, history.getEventType());
        assertEquals("desc", history.getDescription());
        assertEquals(now, history.getCreatedAt());
    }
}
