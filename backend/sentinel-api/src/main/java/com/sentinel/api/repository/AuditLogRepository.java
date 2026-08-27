package com.sentinel.api.repository;

import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByUserIdAndApplicationId(Long userId, Long applicationId, Pageable pageable);

    Page<AuditLog> findByUserIdAndAction(Long userId, AuditAction action, Pageable pageable);

    Page<AuditLog> findByUserIdAndApplicationIdAndAction(Long userId, Long applicationId, AuditAction action, Pageable pageable);

    List<AuditLog> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
