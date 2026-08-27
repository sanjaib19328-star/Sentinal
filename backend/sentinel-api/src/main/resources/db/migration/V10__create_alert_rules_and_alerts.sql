-- V10: Alert Rules and Triggered Alerts

CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    api_endpoint_id BIGINT NULL,
    type VARCHAR(50) NOT NULL,
    threshold DOUBLE NOT NULL,
    evaluation_window_seconds INT NOT NULL DEFAULT 300,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_alert_rules_app (application_id),
    CONSTRAINT fk_alert_rules_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rules_endpoint FOREIGN KEY (api_endpoint_id) REFERENCES api_endpoints (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_rule_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    api_endpoint_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    severity VARCHAR(30) NOT NULL DEFAULT 'WARNING',
    message VARCHAR(500) NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    INDEX idx_alerts_app_status (application_id, status),
    INDEX idx_alerts_rule (alert_rule_id),
    CONSTRAINT fk_alerts_rule FOREIGN KEY (alert_rule_id) REFERENCES alert_rules (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_endpoint FOREIGN KEY (api_endpoint_id) REFERENCES api_endpoints (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
