package com.supera.desafio.core.repository;

import com.supera.desafio.core.domain.user.SystemUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {

    Optional<SystemUser> findByEmailIgnoreCase(String email);
}
