-- 秋招投递追踪（apply tracker）v2

CREATE TABLE `job_applications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `company` VARCHAR(60) NOT NULL,
    `position` VARCHAR(80) NOT NULL,
    `city` VARCHAR(40) NULL,
    `company_type` VARCHAR(20) NULL,
    `channel` VARCHAR(20) NULL,
    `status` VARCHAR(20) NOT NULL,
    `source_url` VARCHAR(500) NULL,
    `source_title` VARCHAR(200) NULL,
    `source_logo_url` VARCHAR(500) NULL,
    `source_error` VARCHAR(200) NULL,
    `source_fetched_at` DATETIME(6) NULL,
    `deadline_at` DATE NULL,
    `applied_at` DATE NOT NULL,
    `note` VARCHAR(1000) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_applications_owner` (`user_id`, `company`, `position`),
    KEY `idx_job_applications_user_status` (`user_id`, `status`),
    KEY `idx_job_applications_user_applied` (`user_id`, `applied_at`),
    KEY `idx_job_applications_user_deadline` (`user_id`, `deadline_at`),
    CONSTRAINT `fk_job_applications_user`
        FOREIGN KEY (`user_id`) REFERENCES `site_users` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `job_application_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT NOT NULL,
    `from_status` VARCHAR(20) NULL,
    `to_status` VARCHAR(20) NOT NULL,
    `occurred_at` DATETIME(6) NOT NULL,
    `note` VARCHAR(300) NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_job_application_events_app_time` (`application_id`, `occurred_at`),
    CONSTRAINT `fk_job_application_events_application`
        FOREIGN KEY (`application_id`) REFERENCES `job_applications` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
