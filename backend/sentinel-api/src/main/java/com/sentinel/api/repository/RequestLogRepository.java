package com.sentinel.api.repository;

import com.sentinel.api.model.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    List<RequestLog> findByApiKeyId(Long apiKeyId);

    List<RequestLog> findByApiKeyIdOrderByTimestampDesc(Long apiKeyId);

    Page<RequestLog> findByApiKeyIdOrderByTimestampDesc(Long apiKeyId, Pageable pageable);

    List<RequestLog> findByApiKeyIdAndTimestampBetweenOrderByTimestampDesc(Long apiKeyId, Instant from, Instant to);

    List<RequestLog> findByRequestId(String requestId);

    @Query("SELECT r FROM RequestLog r WHERE r.applicationId = :applicationId OR (r.applicationId IS NULL AND r.apiKeyId IN (SELECT k.id FROM ApiKey k WHERE k.applicationId = :applicationId)) ORDER BY r.timestamp DESC")
    Page<RequestLog> findByApplicationId(@Param("applicationId") Long applicationId, Pageable pageable);

    @Query("SELECT r FROM RequestLog r WHERE r.applicationId = :applicationId OR (r.applicationId IS NULL AND r.apiKeyId IN (SELECT k.id FROM ApiKey k WHERE k.applicationId = :applicationId)) ORDER BY r.timestamp DESC")
    List<RequestLog> findAllByApplicationIdOrderByTimestampDesc(@Param("applicationId") Long applicationId);

    @Query("SELECT r FROM RequestLog r WHERE (r.applicationId = :applicationId OR (r.applicationId IS NULL AND r.apiKeyId IN (SELECT k.id FROM ApiKey k WHERE k.applicationId = :applicationId))) AND r.timestamp >= :from AND r.timestamp <= :to ORDER BY r.timestamp DESC")
    List<RequestLog> findByApplicationIdAndTimestampBetween(
        @Param("applicationId") Long applicationId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );

    Page<RequestLog> findByEndpointIdOrderByTimestampDesc(Long endpointId, Pageable pageable);

    List<RequestLog> findByEndpointIdOrderByTimestampDesc(Long endpointId);

    List<RequestLog> findByEndpointIdAndTimestampBetweenOrderByTimestampDesc(Long endpointId, Instant from, Instant to);

    default List<RequestLog> findByEndpointIdAndTimestampBetween(Long endpointId, Instant from, Instant to) {
        return findByEndpointIdAndTimestampBetweenOrderByTimestampDesc(endpointId, from, to);
    }

    long countByEndpointId(Long endpointId);
}
