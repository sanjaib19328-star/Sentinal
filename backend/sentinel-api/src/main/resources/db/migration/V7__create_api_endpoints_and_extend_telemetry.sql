-- V7: API Endpoints Discovery, Telemetry Extension, and API Key Revocation

CREATE TABLE IF NOT EXISTS api_endpoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    method VARCHAR(10) NOT NULL,
    normalized_path VARCHAR(255) NOT NULL,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    INDEX idx_api_endpoints_app_id (application_id),
    UNIQUE KEY uk_app_method_path (application_id, method, normalized_path),
    CONSTRAINT fk_api_endpoints_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Extend request_logs with application_id, endpoint_id, normalized_path
ALTER TABLE request_logs
    ADD COLUMN application_id BIGINT NULL,
    ADD COLUMN endpoint_id BIGINT NULL,
    ADD COLUMN normalized_path VARCHAR(255) NULL;

-- Backfill application_id in existing request_logs from api_keys if available
UPDATE request_logs r
JOIN api_keys k ON r.api_key_id = k.id
SET r.application_id = k.application_id
WHERE r.api_key_id IS NOT NULL AND k.application_id IS NOT NULL;

-- Add indexes and foreign keys for request_logs
ALTER TABLE request_logs
    ADD CONSTRAINT fk_req_log_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_req_log_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints (id) ON DELETE SET NULL;

CREATE INDEX idx_req_log_app_timestamp ON request_logs (application_id, timestamp);
CREATE INDEX idx_req_log_endpoint_timestamp ON request_logs (endpoint_id, timestamp);
CREATE INDEX idx_req_log_app_norm_path ON request_logs (application_id, normalized_path);

-- Extend api_keys with revoked_at timestamp
ALTER TABLE api_keys
    ADD COLUMN revoked_at TIMESTAMP NULL;
