package com.sentinel.api.repository;

import com.sentinel.api.model.ApplicationMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ApplicationMetricRepository extends JpaRepository<ApplicationMetric, Long> {

    List<ApplicationMetric> findByApplicationIdOrderByRecordedAtDesc(Long applicationId);

    List<ApplicationMetric> findByApplicationIdAndRecordedAtBetweenOrderByRecordedAtDesc(
        Long applicationId,
        Instant from,
        Instant to
    );
}
