package com.sentinel.api.repository;

import com.sentinel.api.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findAllByApplicationId(Long applicationId);

    Optional<AlertRule> findByIdAndApplicationId(Long id, Long applicationId);

    List<AlertRule> findAllByEnabledTrue();

    void deleteAllByApplicationId(Long applicationId);
}
