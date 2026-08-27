CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    base_url VARCHAR(255) NOT NULL,
    connection_mode VARCHAR(50) NOT NULL DEFAULT 'OBSERVATION',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    health_status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_application_owner_id (owner_id),
    CONSTRAINT fk_applications_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
