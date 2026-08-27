-- Migration V14: Create AI Conversations and Test History Tables
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    metadata_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_conversation_user_id (user_id),
    INDEX idx_conversation_application_id (application_id),
    INDEX idx_conversation_updated_at (updated_at),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    metadata_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_message_conversation_id (conversation_id),
    INDEX idx_message_created_at (created_at),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
