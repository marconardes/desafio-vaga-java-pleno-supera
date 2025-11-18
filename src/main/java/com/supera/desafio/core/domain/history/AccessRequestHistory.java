package com.supera.desafio.core.domain.history;

import com.supera.desafio.core.domain.request.AccessRequest;
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
@Table(name = "access_request_history")
public class AccessRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private AccessRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessRequestHistoryEventType eventType;

    @Column(nullable = false, length = 256)
    private String description;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
