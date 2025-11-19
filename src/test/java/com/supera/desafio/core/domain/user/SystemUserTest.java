package com.supera.desafio.core.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SystemUserTest {

    @Test
    void shouldSetAndGetFields() {
        SystemUser user = new SystemUser();
        user.setId(10L);
        user.setFullName("User Test");
        user.setEmail("user@test.com");
        user.setPassword("secret");
        user.setDepartment(DepartmentType.TI);
        user.setActive(true);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("User Test", user.getFullName());
        assertEquals("user@test.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(DepartmentType.TI, user.getDepartment());
        assertTrue(user.isActive());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }
}
