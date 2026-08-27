-- Phase 4 Observation and Telemetry Schema Optimizations
CREATE INDEX idx_req_log_api_key_time ON request_logs (api_key_id, timestamp);
