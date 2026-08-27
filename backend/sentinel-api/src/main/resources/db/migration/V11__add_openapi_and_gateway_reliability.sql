-- V11: OpenAPI Documentation and Gateway Reliability Configuration

ALTER TABLE api_endpoints
    ADD COLUMN documentation_status VARCHAR(40) NOT NULL DEFAULT 'DISCOVERED',
    ADD COLUMN summary VARCHAR(255) NULL,
    ADD COLUMN description TEXT NULL,
    ADD COLUMN parameters_json LONGTEXT NULL,
    ADD COLUMN request_body_schema_json LONGTEXT NULL,
    ADD COLUMN responses_json LONGTEXT NULL,
    ADD COLUMN is_deprecated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE api_policies
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN retry_delay_ms INT NOT NULL DEFAULT 100,
    ADD COLUMN retry_non_idempotent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN circuit_breaker_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN circuit_failure_threshold INT NOT NULL DEFAULT 5,
    ADD COLUMN circuit_recovery_timeout_seconds INT NOT NULL DEFAULT 15;

CREATE INDEX idx_req_log_key_timestamp ON request_logs (api_key_id, timestamp);
