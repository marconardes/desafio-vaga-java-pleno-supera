package com.supera.desafio.core.repository;

import com.supera.desafio.core.domain.request.AccessRequest;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long>, JpaSpecificationExecutor<AccessRequest> {

    Optional<AccessRequest> findByIdAndRequesterId(Long id, Long requesterId);

    boolean existsByRequesterIdAndStatusAndModules_Id(Long requesterId, AccessRequestStatus status, Long moduleId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(ar.protocol, 14, 4) AS integer)), 0) FROM AccessRequest ar WHERE ar.protocol LIKE CONCAT('SOL-', :datePrefix, '%')")
    int findDailySequence(@Param("datePrefix") String datePrefix);
}
