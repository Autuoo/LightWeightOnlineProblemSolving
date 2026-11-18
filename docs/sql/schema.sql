-- ============================================================
-- 轻量在线判题系统 数据库脚本 v0.2（含 AI 助教模块）
-- DB: MySQL 8.0 / InnoDB / utf8mb4
-- 说明：
--   1. 本期使用逻辑外键（不建 FK 约束），由应用层保证一致性，
--      便于测试数据清理与后续分库。若课程要求强约束可自行补充。
--   2. 第 6 节 ai_hint 为 AI 助教模块，可通过应用开关 OJ_AI_ENABLED 停用；
--      停用后该表仅保留历史数据，不影响主链路。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `oj`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `oj`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(32)     NOT NULL                COMMENT '登录名',
  `password_hash` VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 哈希',
  `nickname`      VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '显示名',
  `role`          VARCHAR(16)     NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/ADMIN',
  `status`        TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  `avatar_url`    VARCHAR(255)    NULL                    COMMENT '头像相对路径，如 /uploads/avatars/u1.png',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ------------------------------------------------------------
-- 2. 题目表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `problem` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`           VARCHAR(128)    NOT NULL                COMMENT '题目标题',
  `description`     MEDIUMTEXT      NOT NULL                COMMENT 'Markdown 题面',
  `samples`         JSON            NULL                    COMMENT '样例：[{input,output}]',
  `difficulty`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1 简单 2 中等 3 困难',
  `time_limit_ms`   INT             NOT NULL DEFAULT 1000   COMMENT '基准时间限制',
  `memory_limit_mb` INT             NOT NULL DEFAULT 256    COMMENT '内存限制',
  `visible`         TINYINT         NOT NULL DEFAULT 1      COMMENT '1 可见 0 隐藏（软删除）',
  `created_by`      BIGINT UNSIGNED NOT NULL                COMMENT '创建者 user.id',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_problem_visible` (`visible`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目';

-- ------------------------------------------------------------
-- 3. 测试点表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `test_case` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `problem_id`      BIGINT UNSIGNED NOT NULL,
  `order_no`        INT             NOT NULL                COMMENT '测试点序号，从 1 开始',
  `input`           MEDIUMTEXT      NOT NULL                COMMENT '标准输入',
  `expected_output` MEDIUMTEXT      NOT NULL                COMMENT '期望输出',
  `score`           INT             NOT NULL DEFAULT 0      COMMENT '该点分值',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_order` (`problem_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试点';

-- ------------------------------------------------------------
-- 4. 提交记录表
--    注意：幂等由 Service 层按 (user_id, problem_id, code_hash, created_at)
--    在时间窗口内查询实现，不使用唯一索引，以支持"重新评测"。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `submission` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NOT NULL,
  `problem_id`    BIGINT UNSIGNED NOT NULL,
  `language`      VARCHAR(16)     NOT NULL                COMMENT 'PYTHON3/CPP17/JAVA17',
  `code`          MEDIUMTEXT      NOT NULL                COMMENT '用户提交的源码',
  `code_hash`     CHAR(64)        NOT NULL                COMMENT 'SHA-256(language+code)',
  `status`        VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                                                          COMMENT 'PENDING/COMPILING/RUNNING/AC/WA/TLE/MLE/RE/CE/REJECTED',
  `score`         INT             NOT NULL DEFAULT 0      COMMENT '得分',
  `exec_time_ms`  INT             NULL                    COMMENT '最大单点耗时',
  `memory_kb`     INT             NULL                    COMMENT '最大单点内存',
  `compile_output` TEXT           NULL                    COMMENT '编译输出（截断至 8KB）',
  `error_message` VARCHAR(512)    NULL                    COMMENT '错误说明',
  `finished_at`   DATETIME(3)     NULL                    COMMENT '进入终态时间',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_sub_idempotent`   (`user_id`, `problem_id`, `code_hash`, `created_at`),
  KEY `idx_sub_user_created` (`user_id`, `created_at`),
  KEY `idx_sub_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录';

-- ------------------------------------------------------------
-- 5. 逐点评测结果表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `submission_case_result` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `case_id`       BIGINT UNSIGNED NOT NULL,
  `status`        VARCHAR(16)     NOT NULL                COMMENT 'AC/WA/TLE/MLE/RE',
  `exec_time_ms`  INT             NULL,
  `memory_kb`     INT             NULL,
  `message`       VARCHAR(512)    NULL                    COMMENT '失败详情（截断的 diff 摘要）',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_sub` (`submission_id`, `case_id`),
  KEY `idx_case_submission` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐点评测结果';

-- ------------------------------------------------------------
-- 6. AI 助教问答记录（M10，可通过 OJ_AI_ENABLED 停用）
--    用途：额度统计、调用轨迹留档、eval 回归排查
--    不落库：完整 prompt、模型原始响应（含成本与用户代码全文）
--    注意：tool_calls 只记录工具名/成功与否/耗时，不含任何数据内容
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_hint` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submission_id`     BIGINT UNSIGNED NOT NULL              COMMENT '关联 submission.id',
  `user_id`           BIGINT UNSIGNED NOT NULL              COMMENT '提问用户',
  `question`          VARCHAR(500)    NOT NULL              COMMENT '学生提问（截断）',
  `level`             TINYINT         NOT NULL DEFAULT 1    COMMENT '提示层级：1 思路 2 定位 3 检查点',
  `answer`            MEDIUMTEXT      NULL                  COMMENT '助教回答（仅最终文本）',
  `tool_calls`        JSON            NULL                  COMMENT '[{tool,ok,elapsedMs}] 调用轨迹，不含数据内容',
  `model`             VARCHAR(64)     NULL                  COMMENT '模型标识，如 deepseek-chat',
  `prompt_tokens`     INT             NOT NULL DEFAULT 0,
  `completion_tokens` INT             NOT NULL DEFAULT 0,
  `step_count`        TINYINT         NOT NULL DEFAULT 0    COMMENT '实际循环步数',
  `status`            VARCHAR(16)     NOT NULL DEFAULT 'OK'
                                                            COMMENT 'OK/FAILED/TIMEOUT/STEP_LIMIT/QUOTA_EXCEEDED',
  `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_hint_user_created` (`user_id`, `created_at`),
  KEY `idx_hint_submission`   (`submission_id`),
  KEY `idx_hint_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助教问答记录';

-- ------------------------------------------------------------
-- 6.1 用户级 AI 凭据（每用户独立配置，API Key 加密存储）
--     未配置的用户不能使用 AI 助教
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_ai_config` (
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID，对应 user.id',
  `enabled`     TINYINT         NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
  `base_url`    VARCHAR(512)    NOT NULL DEFAULT ''     COMMENT 'OpenAI 兼容端点',
  `api_key_enc` VARCHAR(1024)   NOT NULL DEFAULT ''     COMMENT 'AES-GCM 加密后的 API Key',
  `model`       VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '模型标识',
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户级 AI 凭据';

-- ------------------------------------------------------------
-- 7. 初始化数据
--    下方 password_hash 为 BCrypt(cost=10) 的占位示例值，不对应任何明文密码。
--    首次部署必须用程序生成真实哈希后替换，否则无法登录。
--    建议实现一个 CommandLineRunner，读取环境变量 OJ_ADMIN_PASSWORD 生成并更新。
-- ------------------------------------------------------------
INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`)
SELECT 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin');

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`)
SELECT 'stu001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '示例学生', 'STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'stu001');

INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  'A+B Problem',
  '## 题目描述\n输入两个整数 a 和 b，输出它们的和。\n\n## 输入格式\n一行两个整数，以空格分隔。\n\n## 输出格式\n一个整数。',
  JSON_ARRAY(JSON_OBJECT('input', '1 2', 'output', '3')),
  1, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = 'A+B Problem');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, '1 2\n', '3\n', 50
FROM `problem` p
WHERE p.`title` = 'A+B Problem'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, '100 -37\n', '63\n', 50
FROM `problem` p
WHERE p.`title` = 'A+B Problem'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

-- ------------------------------------------------------------
-- 7.1 示例题库扩展：5 道基础题（每题 5 个测试点，每个 20 分）
-- ------------------------------------------------------------
INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  '数列求和',
  '## 题目描述\n给定 n 个整数，求它们的和。\n\n## 输入格式\n第一行一个整数 n（1 ≤ n ≤ 100000）。\n第二行 n 个整数，以空格分隔，绝对值不超过 10^9。\n\n## 输出格式\n输出一个整数，表示这 n 个数的和。',
  JSON_ARRAY(JSON_OBJECT('input', '3\n1 2 3\n', 'output', '6\n')),
  1, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '数列求和');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, '1\n5\n', '5\n', 20 FROM `problem` p
WHERE p.`title` = '数列求和'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, '3\n1 2 3\n', '6\n', 20 FROM `problem` p
WHERE p.`title` = '数列求和'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 3, '5\n-1 -2 -3 -4 -5\n', '-15\n', 20 FROM `problem` p
WHERE p.`title` = '数列求和'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 3);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 4, '4\n0 0 0 0\n', '0\n', 20 FROM `problem` p
WHERE p.`title` = '数列求和'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 4);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 5, '3\n1000000000 1000000000 1000000000\n', '3000000000\n', 20 FROM `problem` p
WHERE p.`title` = '数列求和'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 5);

INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  '求最大值',
  '## 题目描述\n给定 n 个整数，输出其中的最大值。\n\n## 输入格式\n第一行一个整数 n（1 ≤ n ≤ 100000）。\n第二行 n 个整数，以空格分隔，绝对值不超过 10^9。\n\n## 输出格式\n输出一个整数，表示这 n 个数中的最大值。',
  JSON_ARRAY(JSON_OBJECT('input', '5\n3 1 4 1 5\n', 'output', '5\n')),
  1, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '求最大值');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, '1\n42\n', '42\n', 20 FROM `problem` p
WHERE p.`title` = '求最大值'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, '5\n3 1 4 1 5\n', '5\n', 20 FROM `problem` p
WHERE p.`title` = '求最大值'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 3, '4\n-5 -2 -9 -7\n', '-2\n', 20 FROM `problem` p
WHERE p.`title` = '求最大值'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 3);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 4, '3\n0 0 0\n', '0\n', 20 FROM `problem` p
WHERE p.`title` = '求最大值'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 4);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 5, '2\n-1000000000 1000000000\n', '1000000000\n', 20 FROM `problem` p
WHERE p.`title` = '求最大值'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 5);

INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  '回文串判断',
  '## 题目描述\n给定一个只包含小写字母的字符串，判断它是否为回文串。回文串指正着读和倒着读完全相同的字符串。\n\n## 输入格式\n一行一个字符串，长度不超过 1000，不含空格。\n\n## 输出格式\n如果是回文串输出 YES，否则输出 NO。',
  JSON_ARRAY(JSON_OBJECT('input', 'level\n', 'output', 'YES\n')),
  2, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '回文串判断');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, 'level\n', 'YES\n', 20 FROM `problem` p
WHERE p.`title` = '回文串判断'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, 'abc\n', 'NO\n', 20 FROM `problem` p
WHERE p.`title` = '回文串判断'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 3, 'a\n', 'YES\n', 20 FROM `problem` p
WHERE p.`title` = '回文串判断'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 3);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 4, 'abba\n', 'YES\n', 20 FROM `problem` p
WHERE p.`title` = '回文串判断'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 4);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 5, 'abca\n', 'NO\n', 20 FROM `problem` p
WHERE p.`title` = '回文串判断'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 5);

INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  '斐波那契数列',
  '## 题目描述\n斐波那契数列定义：f(1)=1，f(2)=1，当 n≥3 时 f(n)=f(n-1)+f(n-2)。求第 n 项。\n\n## 输入格式\n一个整数 n（1 ≤ n ≤ 50）。\n\n## 输出格式\n输出 f(n)。结果可能超过 32 位整数范围，请使用 64 位整数。',
  JSON_ARRAY(JSON_OBJECT('input', '10\n', 'output', '55\n')),
  2, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '斐波那契数列');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, '1\n', '1\n', 20 FROM `problem` p
WHERE p.`title` = '斐波那契数列'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, '2\n', '1\n', 20 FROM `problem` p
WHERE p.`title` = '斐波那契数列'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 3, '10\n', '55\n', 20 FROM `problem` p
WHERE p.`title` = '斐波那契数列'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 3);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 4, '30\n', '832040\n', 20 FROM `problem` p
WHERE p.`title` = '斐波那契数列'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 4);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 5, '50\n', '12586269025\n', 20 FROM `problem` p
WHERE p.`title` = '斐波那契数列'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 5);

INSERT INTO `problem` (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`, `visible`, `created_by`)
SELECT
  '排序去重',
  '## 题目描述\n给定 n 个整数，去掉重复的数后按从小到大排序输出。\n\n## 输入格式\n第一行一个整数 n（1 ≤ n ≤ 1000）。\n第二行 n 个整数，以空格分隔，绝对值不超过 10^9。\n\n## 输出格式\n一行，输出去重并升序排列后的整数，以空格分隔。',
  JSON_ARRAY(JSON_OBJECT('input', '5\n3 1 2 3 1\n', 'output', '1 2 3\n')),
  2, 1000, 256, 1,
  (SELECT `id` FROM `user` WHERE `username` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '排序去重');

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 1, '5\n3 1 2 3 1\n', '1 2 3\n', 20 FROM `problem` p
WHERE p.`title` = '排序去重'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 1);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 2, '1\n7\n', '7\n', 20 FROM `problem` p
WHERE p.`title` = '排序去重'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 2);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 3, '4\n-1 -1 -2 0\n', '-2 -1 0\n', 20 FROM `problem` p
WHERE p.`title` = '排序去重'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 3);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 4, '3\n1000000000 -1000000000 0\n', '-1000000000 0 1000000000\n', 20 FROM `problem` p
WHERE p.`title` = '排序去重'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 4);

INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)
SELECT p.`id`, 5, '2\n5 5\n', '5\n', 20 FROM `problem` p
WHERE p.`title` = '排序去重'
  AND NOT EXISTS (SELECT 1 FROM `test_case` t WHERE t.`problem_id` = p.`id` AND t.`order_no` = 5);

-- ------------------------------------------------------------
-- 8. AI 助手相关配置（应用配置项，非数据库存储，此处仅作说明）
--   OJ_AI_ENABLED=true
--   OJ_SECRET_KEY=               # 用户 API Key 的 AES-GCM 加密密钥，部署必须替换
--   提示：AI 端点/模型/Key 由每个用户在自己的「个人设置」页配置，不在此处填写
--   OJ_AI_MODEL=deepseek-chat
--   OJ_AI_MAX_STEPS=3
--   OJ_AI_TIMEOUT_SECONDS=30
--   OJ_AI_DAILY_USER_QUOTA=100
--   OJ_AI_DAILY_GLOBAL_QUOTA=500
--   OJ_AI_MIN_INTERVAL_SECONDS=0   # 0 表示不限制最小间隔
-- ------------------------------------------------------------