-- ============================================================
-- ChatterApp schema  –  MySQL 8, InnoDB, utf8mb4
-- Loaded automatically on startup (spring.sql.init.mode=always)
-- Uses CREATE TABLE IF NOT EXISTS so data is never dropped.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    is_online     BOOLEAN      NOT NULL DEFAULT FALSE,
    last_seen     DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_one_id BIGINT NOT NULL,   -- always the smaller user id
    user_two_id BIGINT NOT NULL,   -- always the larger user id
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_one_id, user_two_id),
    FOREIGN KEY (user_one_id) REFERENCES users(id),
    FOREIGN KEY (user_two_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT   NOT NULL,
    sender_id       BIGINT   NOT NULL,
    content         TEXT     NOT NULL,
    sent_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    is_read         BOOLEAN  NOT NULL DEFAULT FALSE,
    is_notified     BOOLEAN  NOT NULL DEFAULT FALSE,
    INDEX idx_conv_time (conversation_id, sent_at),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (sender_id)       REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
