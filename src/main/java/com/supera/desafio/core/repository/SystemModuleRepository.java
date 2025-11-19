package com.supera.desafio.core.repository;

import com.supera.desafio.core.domain.module.SystemModule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemModuleRepository extends JpaRepository<SystemModule, Long> {

    List<SystemModule> findByActiveTrueOrderByNameAsc();

    Optional<SystemModule> findByCode(String code);
}
