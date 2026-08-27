CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    INDEX idx_api_key_hash (key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS request_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    api_key_id BIGINT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status_code INT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    latency_ms BIGINT NOT NULL,
    client_ip VARCHAR(45) NULL,
    INDEX idx_req_log_request_id (request_id),
    INDEX idx_req_log_api_key_id (api_key_id),
    INDEX idx_req_log_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
