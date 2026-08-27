package com.sentinel.api.repository;

import com.sentinel.api.model.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    List<ApiEndpoint> findByApplicationId(Long applicationId);

    List<ApiEndpoint> findByApplicationIdOrderByLastSeenAtDesc(Long applicationId);

    List<ApiEndpoint> findByApplicationIdIn(List<Long> applicationIds);

    Optional<ApiEndpoint> findByApplicationIdAndMethodAndNormalizedPath(Long applicationId, String method, String normalizedPath);

    Optional<ApiEndpoint> findByIdAndApplicationId(Long id, Long applicationId);

    long countByApplicationId(Long applicationId);
}
