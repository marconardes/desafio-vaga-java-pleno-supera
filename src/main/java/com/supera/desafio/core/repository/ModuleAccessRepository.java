package com.supera.desafio.core.repository;

import com.supera.desafio.core.domain.module.ModuleAccess;
import com.supera.desafio.core.domain.module.ModuleAccessStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleAccessRepository extends JpaRepository<ModuleAccess, Long> {

    long countByUserIdAndStatus(Long userId, ModuleAccessStatus status);

    boolean existsByUserIdAndModuleIdAndStatus(Long userId, Long moduleId, ModuleAccessStatus status);

    List<ModuleAccess> findByUserIdAndStatus(Long userId, ModuleAccessStatus status);

    Optional<ModuleAccess> findByUserIdAndModuleIdAndStatus(Long userId, Long moduleId, ModuleAccessStatus status);
}
