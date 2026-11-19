-- 比赛模块增量迁移脚本（在已有 oj 库上执行一次）
-- 新库可直接执行 schema.sql；本脚本用于已有数据的演示库。
USE `oj`;

CREATE TABLE IF NOT EXISTS `contest` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(128)    NOT NULL                COMMENT '比赛标题',
  `description`   TEXT            NULL                    COMMENT '比赛说明',
  `mode`          VARCHAR(16)     NOT NULL DEFAULT 'ICPC' COMMENT 'ICPC/ICPC 罚时制；OI 预留',
  `start_at`      DATETIME(3)     NOT NULL                COMMENT '开始时间',
  `end_at`        DATETIME(3)     NOT NULL                COMMENT '结束时间',
  `freeze_at`     DATETIME(3)     NULL                    COMMENT '封榜时间，NULL 表示不封榜',
  `join_type`     VARCHAR(16)     NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PASSWORD',
  `password_hash` VARCHAR(100)    NULL                    COMMENT '报名密码 BCrypt',
  `published`     TINYINT         NOT NULL DEFAULT 0      COMMENT '1 已发布 0 草稿',
  `created_by`    BIGINT UNSIGNED NOT NULL                COMMENT '创建者 user.id',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_contest_window` (`published`, `start_at`, `end_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛';

CREATE TABLE IF NOT EXISTS `contest_problem` (
  `contest_id` BIGINT UNSIGNED NOT NULL,
  `problem_id` BIGINT UNSIGNED NOT NULL,
  `alias`      VARCHAR(8)      NOT NULL                COMMENT '题目编号，如 A/B/C',
  `order_no`   INT             NOT NULL                COMMENT '展示顺序',
  `score`      INT             NULL                    COMMENT 'OI 模式分值覆盖；ICPC 不使用',
  PRIMARY KEY (`contest_id`, `problem_id`),
  UNIQUE KEY `uk_contest_order` (`contest_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛题目映射';

CREATE TABLE IF NOT EXISTS `contest_participant` (
  `contest_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`       BIGINT UNSIGNED NOT NULL,
  `registered_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`contest_id`, `user_id`),
  KEY `idx_contest_user` (`user_id`, `contest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛报名记录';

-- submission.contest_id（若列不存在）
SET @has_contest_id = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'oj' AND TABLE_NAME = 'submission' AND COLUMN_NAME = 'contest_id'
);
SET @sql = IF(
  @has_contest_id = 0,
  'ALTER TABLE `submission` ADD COLUMN `contest_id` BIGINT UNSIGNED NULL COMMENT ''比赛 ID；练习提交为 NULL'' AFTER `problem_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- submission 比赛索引（若不存在）
SET @has_contest_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'oj' AND TABLE_NAME = 'submission' AND INDEX_NAME = 'idx_sub_contest'
);
SET @sql = IF(
  @has_contest_index = 0,
  'ALTER TABLE `submission` ADD INDEX `idx_sub_contest` (`contest_id`, `created_at`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;