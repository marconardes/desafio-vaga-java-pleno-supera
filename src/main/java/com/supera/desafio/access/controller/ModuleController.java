package com.supera.desafio.access.controller;

import com.supera.desafio.access.dto.ModuleDto;
import com.supera.desafio.access.service.ModuleQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleQueryService service;

    public ModuleController(ModuleQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ModuleDto>> list() {
        return ResponseEntity.ok(service.listAll());
    }
}
