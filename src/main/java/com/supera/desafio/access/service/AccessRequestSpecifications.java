package com.supera.desafio.access.service;

import com.supera.desafio.core.domain.request.AccessRequest;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class AccessRequestSpecifications {

    private AccessRequestSpecifications() {
    }

    public static Specification<AccessRequest> belongsTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), userId);
    }

    public static Specification<AccessRequest> hasStatus(AccessRequestStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<AccessRequest> urgent(Boolean urgent) {
        return (root, query, cb) -> cb.equal(root.get("urgent"), urgent);
    }

    public static Specification<AccessRequest> betweenDates(Instant start, Instant end) {
        return (root, query, cb) -> cb.between(root.get("requestedAt"), start, end);
    }

    public static Specification<AccessRequest> matchesText(String term) {
        return (root, query, cb) -> {
            var modulesJoin = root.join("modules", JoinType.LEFT);
            query.distinct(true);
            String pattern = "%" + term.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("protocol")), pattern),
                    cb.like(cb.lower(modulesJoin.get("name")), pattern));
        };
    }
}
