package com.supera.desafio.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.supera.desafio.access.dto.ModuleDto;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.core.repository.SystemModuleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleQueryServiceTest {

    private final SystemModuleRepository repository = mock(SystemModuleRepository.class);
    private final ModuleQueryService service = new ModuleQueryService(repository);

    @Test
    void shouldMapModules() {
        SystemModule module = new SystemModule();
        module.setId(1L);
        module.setCode("PORTAL");
        module.setName("Portal");
        module.setDescription("desc");
        module.setActive(true);
        module.getAllowedDepartments().add(DepartmentType.TI);
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(module));

        List<ModuleDto> modules = service.listAll();
        assertEquals(1, modules.size());
        assertEquals("Portal", modules.get(0).name());
    }
}
