-- V8: API Policies, Multi-level Rate Limits & Quotas

CREATE TABLE IF NOT EXISTS api_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    api_endpoint_id BIGINT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit INT NOT NULL DEFAULT 60,
    rate_window_seconds INT NOT NULL DEFAULT 60,
    quota_limit INT NULL,
    quota_window_seconds INT NULL,
    timeout_ms INT NOT NULL DEFAULT 5000,
    max_request_body_bytes BIGINT NULL,
    max_response_body_bytes BIGINT NULL,
    allowed_methods VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_policy_app_endpoint (application_id, api_endpoint_id),
    CONSTRAINT fk_policy_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_policy_endpoint FOREIGN KEY (api_endpoint_id) REFERENCES api_endpoints (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
