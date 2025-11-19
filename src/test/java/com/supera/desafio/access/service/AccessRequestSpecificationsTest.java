package com.supera.desafio.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supera.desafio.core.domain.request.AccessRequest;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class AccessRequestSpecificationsTest {

    @SuppressWarnings("unchecked")
    private final Root<AccessRequest> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @Test
    void belongsToSpecShouldUseRequesterPath() {
        Path<Object> requesterPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("requester")).thenReturn(requesterPath);
        when(requesterPath.get("id")).thenReturn(idPath);
        when(cb.equal(idPath, 1L)).thenReturn(predicate);
        assertEquals(predicate, AccessRequestSpecifications.belongsTo(1L).toPredicate(root, query, cb));
    }

    @Test
    void hasStatusSpecShouldBuildPredicate() {
        Path<Object> statusPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, AccessRequestStatus.ATIVO)).thenReturn(predicate);
        assertEquals(predicate, AccessRequestSpecifications.hasStatus(AccessRequestStatus.ATIVO).toPredicate(root, query, cb));
    }

    @Test
    void urgentSpecShouldBuildPredicate() {
        Path<Object> urgentPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("urgent")).thenReturn(urgentPath);
        when(cb.equal(urgentPath, true)).thenReturn(predicate);
        assertEquals(predicate, AccessRequestSpecifications.urgent(true).toPredicate(root, query, cb));
    }

    @Test
    void betweenDatesSpecShouldBuildPredicate() {
        @SuppressWarnings("unchecked")
        Path<Instant> requestedPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Instant start = Instant.now();
        Instant end = start.plusSeconds(10);
        when(root.get("requestedAt")).thenReturn((Path) requestedPath);
        when(cb.between(requestedPath, start, end)).thenReturn(predicate);
        assertEquals(predicate, AccessRequestSpecifications.betweenDates(start, end).toPredicate(root, query, cb));
    }

    @Test
    void matchesTextSpecShouldUseJoin() {
        @SuppressWarnings("unchecked")
        Join<AccessRequest, Object> modulesJoin = mock(Join.class);
        @SuppressWarnings("unchecked")
        Path<String> protocolPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<String> namePath = mock(Path.class);
        Predicate predicate1 = mock(Predicate.class);
        Predicate predicate2 = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(root.get("protocol")).thenReturn((Path) protocolPath);
        when(cb.lower(protocolPath)).thenReturn(protocolPath);
        when(cb.like(protocolPath, "%term%")).thenReturn(predicate1);
        when(root.join(org.mockito.ArgumentMatchers.eq("modules"), org.mockito.ArgumentMatchers.eq(JoinType.LEFT))).thenReturn((Join) modulesJoin);
        when(modulesJoin.get("name")).thenReturn((Path) namePath);
        when(cb.lower(namePath)).thenReturn(namePath);
        when(cb.like(namePath, "%term%")).thenReturn(predicate2);
        when(cb.or(predicate1, predicate2)).thenReturn(combined);

        Predicate predicate = AccessRequestSpecifications.matchesText("term").toPredicate(root, query, cb);
        verify(query).distinct(true);
        assertEquals(combined, predicate);
    }
}
