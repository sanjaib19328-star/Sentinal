-- Migration V13: Universal Upstream Authentication
-- Supports: NONE, API_KEY_HEADER, API_KEY_QUERY, BEARER_TOKEN, BASIC_AUTH, CUSTOM_HEADER

ALTER TABLE applications
    ADD COLUMN upstream_auth_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    ADD COLUMN upstream_auth_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN upstream_auth_config_encrypted TEXT NULL;

CREATE INDEX idx_applications_upstream_auth ON applications (upstream_auth_type, upstream_auth_enabled);
