-- V9: Management Plane Audit Logs

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT NULL,
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id VARCHAR(100) NULL,
    metadata TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_audit_user_created (user_id, created_at),
    INDEX idx_audit_app_created (application_id, created_at),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
