package com.supera.desafio.core.domain.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.supera.desafio.core.domain.user.SystemUser;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ModuleAccessTest {

    @Test
    void shouldSetAndGetFields() {
        ModuleAccess access = new ModuleAccess();
        SystemUser user = new SystemUser();
        SystemModule module = new SystemModule();
        access.setId(1L);
        access.setUser(user);
        access.setModule(module);
        access.setStatus(ModuleAccessStatus.REVOGADO);
        Instant now = Instant.now();
        access.setGrantedAt(now);
        access.setExpiresAt(now.plusSeconds(100));
        access.setRevokedAt(now);

        assertEquals(1L, access.getId());
        assertEquals(user, access.getUser());
        assertEquals(module, access.getModule());
        assertEquals(ModuleAccessStatus.REVOGADO, access.getStatus());
        assertEquals(now, access.getGrantedAt());
        assertEquals(now.plusSeconds(100), access.getExpiresAt());
        assertEquals(now, access.getRevokedAt());
    }
}
