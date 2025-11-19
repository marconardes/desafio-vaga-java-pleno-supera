package com.supera.desafio.core.domain.request;

import com.supera.desafio.core.domain.history.AccessRequestHistory;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.core.domain.user.SystemUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String protocol;

    @Column(nullable = false, length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessRequestStatus status;

    @Column(nullable = false)
    private boolean urgent;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    private Instant expiresAt;

    @Column(length = 255)
    private String deniedReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentType requesterDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private SystemUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_request_id")
    private AccessRequest parentRequest;

    @ManyToMany
    @JoinTable(name = "access_request_modules",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id"))
    private Set<SystemModule> modules = new LinkedHashSet<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AccessRequestHistory> historyEntries = new LinkedHashSet<>();
}
