CREATE TABLE `site_users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `display_name` VARCHAR(40) NOT NULL,
    `username` VARCHAR(30) NOT NULL,
    `email` VARCHAR(120) NOT NULL,
    `password_hash` VARCHAR(100) NOT NULL,
    `role` VARCHAR(16) NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `ai_daily_limit` INT NOT NULL DEFAULT 20,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_site_users_username` (`username`),
    UNIQUE KEY `uk_site_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `community_questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `detail` VARCHAR(1000) NOT NULL,
    `tag` VARCHAR(40) NOT NULL,
    `author` VARCHAR(40) NOT NULL,
    `author_user_id` BIGINT NULL,
    `moderation_status` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `likes` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_community_questions_author_user` (`author_user_id`),
    CONSTRAINT `fk_community_questions_author_user`
        FOREIGN KEY (`author_user_id`) REFERENCES `site_users` (`id`)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `community_replies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `author` VARCHAR(40) NOT NULL,
    `author_user_id` BIGINT NULL,
    `author_role` VARCHAR(16) NOT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_community_replies_question` (`question_id`),
    KEY `idx_community_replies_author_user` (`author_user_id`),
    CONSTRAINT `fk_community_replies_question`
        FOREIGN KEY (`question_id`) REFERENCES `community_questions` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_community_replies_author_user`
        FOREIGN KEY (`author_user_id`) REFERENCES `site_users` (`id`)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(80) NOT NULL DEFAULT '新对话',
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ai_conversation_user_updated` (`user_id`, `updated_at`),
    CONSTRAINT `fk_ai_conversation_user`
        FOREIGN KEY (`user_id`) REFERENCES `site_users` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ai_message_conversation_created` (`conversation_id`, `created_at`),
    CONSTRAINT `fk_ai_message_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_daily_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `usage_date` DATE NOT NULL,
    `used_count` INT NOT NULL DEFAULT 0,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_daily_usage_user_date` (`user_id`, `usage_date`),
    CONSTRAINT `fk_ai_daily_usage_user`
        FOREIGN KEY (`user_id`) REFERENCES `site_users` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
