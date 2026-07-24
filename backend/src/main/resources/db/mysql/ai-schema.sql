-- AGEON AI persistence schema for MySQL 8.x.
-- The existing authentication table in this project is named site_users.

SET @ai_daily_limit_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'site_users'
      AND COLUMN_NAME = 'ai_daily_limit'
);

SET @add_ai_daily_limit_sql = IF(
    @ai_daily_limit_exists = 0,
    'ALTER TABLE `site_users` ADD COLUMN `ai_daily_limit` INT NOT NULL DEFAULT 20 AFTER `status`',
    'SELECT 1'
);

PREPARE add_ai_daily_limit_statement FROM @add_ai_daily_limit_sql;
EXECUTE add_ai_daily_limit_statement;
DEALLOCATE PREPARE add_ai_daily_limit_statement;

CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(80) NOT NULL DEFAULT '新对话',
    `created_at` TIMESTAMP(6) NOT NULL,
    `updated_at` TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ai_conversation_user_updated` (`user_id`, `updated_at`),
    CONSTRAINT `fk_ai_conversation_user`
        FOREIGN KEY (`user_id`) REFERENCES `site_users` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ai_message_conversation_created` (`conversation_id`, `created_at`),
    CONSTRAINT `fk_ai_message_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `chk_ai_message_role`
        CHECK (`role` IN ('USER', 'ASSISTANT')),
    CONSTRAINT `chk_ai_message_status`
        CHECK (`status` IN ('COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_daily_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `usage_date` DATE NOT NULL,
    `used_count` INT NOT NULL DEFAULT 0,
    `updated_at` TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_daily_usage_user_date` (`user_id`, `usage_date`),
    CONSTRAINT `fk_ai_daily_usage_user`
        FOREIGN KEY (`user_id`) REFERENCES `site_users` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `chk_ai_daily_usage_non_negative`
        CHECK (`used_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
