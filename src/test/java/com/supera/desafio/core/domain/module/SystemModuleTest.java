package com.supera.desafio.core.domain.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.supera.desafio.core.domain.user.DepartmentType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SystemModuleTest {

    @Test
    void shouldHandleDepartmentsAndIncompatibilities() {
        SystemModule portal = new SystemModule();
        portal.setId(1L);
        portal.setCode("PORTAL");
        portal.setName("Portal");
        portal.setDescription("desc");
        portal.setActive(true);
        portal.setCreatedAt(Instant.now());
        portal.setUpdatedAt(Instant.now());
        portal.getAllowedDepartments().add(DepartmentType.TI);

        SystemModule relatorios = new SystemModule();
        relatorios.setId(2L);
        relatorios.setCode("REL");
        portal.getIncompatibleModules().add(relatorios);

        assertEquals(1, portal.getAllowedDepartments().size());
        assertEquals(1, portal.getIncompatibleModules().size());
        assertEquals("PORTAL", portal.getCode());
        assertTrue(portal.isActive());
    }
}
