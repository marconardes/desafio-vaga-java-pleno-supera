package com.supera.desafio.access.service;

import com.supera.desafio.access.dto.ModuleDto;
import com.supera.desafio.core.repository.SystemModuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModuleQueryService {

    private final SystemModuleRepository moduleRepository;

    public ModuleQueryService(SystemModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public List<ModuleDto> listAll() {
        return moduleRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(module -> new ModuleDto(module.getId(), module.getCode(), module.getName(), module.getDescription(), module.isActive()))
                .toList();
    }
}
