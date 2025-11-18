package com.supera.desafio.core.domain.module;

import com.supera.desafio.core.domain.user.SystemUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "module_accesses")
public class ModuleAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private SystemUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id")
    private SystemModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuleAccessStatus status = ModuleAccessStatus.ATIVO;

    @Column(nullable = false)
    private Instant grantedAt = Instant.now();

    private Instant expiresAt;

    private Instant revokedAt;
}
