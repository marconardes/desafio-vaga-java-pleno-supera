package com.supera.desafio.core.domain.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.supera.desafio.core.domain.history.AccessRequestHistory;
import com.supera.desafio.core.domain.history.AccessRequestHistoryEventType;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.core.domain.user.SystemUser;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccessRequestTest {

    @Test
    void shouldHandleModulesAndHistory() {
        AccessRequest request = new AccessRequest();
        request.setId(1L);
        request.setProtocol("SOL-20250101-0001");
        request.setJustification("Justificativa");
        request.setStatus(AccessRequestStatus.ATIVO);
        request.setUrgent(true);
        request.setRequestedAt(Instant.now());
        request.setExpiresAt(Instant.now().plusSeconds(100));
        request.setDeniedReason("none");
        request.setRequesterDepartment(DepartmentType.TI);
        SystemUser requester = new SystemUser();
        request.setRequester(requester);
        AccessRequest parent = new AccessRequest();
        request.setParentRequest(parent);
        SystemModule module = new SystemModule();
        request.getModules().add(module);
        AccessRequestHistory history = new AccessRequestHistory();
        history.setEventType(AccessRequestHistoryEventType.CRIADO);
        history.setRequest(request);
        request.getHistoryEntries().add(history);

        assertEquals("SOL-20250101-0001", request.getProtocol());
        assertTrue(request.isUrgent());
        assertEquals(1, request.getModules().size());
        assertEquals(1, request.getHistoryEntries().size());
    }
}
