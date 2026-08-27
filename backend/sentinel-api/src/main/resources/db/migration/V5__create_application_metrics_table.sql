CREATE TABLE IF NOT EXISTS application_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value DOUBLE NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    INDEX idx_app_metrics_app_id (application_id),
    INDEX idx_app_metrics_recorded_at (recorded_at),
    CONSTRAINT fk_application_metrics_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
