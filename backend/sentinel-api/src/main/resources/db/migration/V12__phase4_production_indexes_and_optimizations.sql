-- V12: Production Index Optimizations for Global Observability and Query Analytics

CREATE INDEX idx_req_log_api_key_timestamp ON request_logs (api_key_id, timestamp);
CREATE INDEX idx_req_log_status_timestamp ON request_logs (status_code, timestamp);
CREATE INDEX idx_req_log_app_status_ts ON request_logs (application_id, status_code, timestamp);
CREATE INDEX idx_api_endpoints_status ON api_endpoints (documentation_status);
