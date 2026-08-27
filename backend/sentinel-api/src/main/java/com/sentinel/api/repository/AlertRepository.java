package com.sentinel.api.repository;

import com.sentinel.api.model.Alert;
import com.sentinel.api.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByApplicationIdOrderByTriggeredAtDesc(Long applicationId);

    List<Alert> findAllByApplicationIdAndStatusOrderByTriggeredAtDesc(Long applicationId, AlertStatus status);

    Optional<Alert> findFirstByAlertRuleIdAndStatus(Long alertRuleId, AlertStatus status);

    long countByApplicationIdAndStatus(Long applicationId, AlertStatus status);

    Page<Alert> findByApplicationIdInOrderByTriggeredAtDesc(List<Long> applicationIds, Pageable pageable);

    List<Alert> findTop10ByApplicationIdInAndStatusOrderByTriggeredAtDesc(List<Long> applicationIds, AlertStatus status);

    void deleteAllByApplicationId(Long applicationId);
}
