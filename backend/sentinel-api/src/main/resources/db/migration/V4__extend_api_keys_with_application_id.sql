ALTER TABLE api_keys
    ADD COLUMN application_id BIGINT NULL;

ALTER TABLE api_keys
    ADD CONSTRAINT fk_api_keys_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;

CREATE INDEX idx_api_keys_application_id ON api_keys (application_id);
