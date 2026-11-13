-- ============================================================
-- V2__fix_and_prepare.sql
-- 目标库：MySQL 5.5.40（当前开发库）
--
-- 说明：
--   Spring Boot 3.5 默认管理的 Flyway 10.x 官方最低支持 MySQL 5.7，
--   无法在 5.5 上运行。本阶段用"版本化 SQL + schema_version 手工记账"，
--   脚本命名沿用 Flyway 规范（V2__xxx.sql），切换到 8.0 后可直接纳入 Flyway。
--
-- 执行前必须有备份：backup/huixuetong_5.5_20261108.sql
-- ============================================================

-- 0. 版本记账表（无 Flyway 时的替代品）
CREATE TABLE IF NOT EXISTS schema_version (
    version     VARCHAR(20)  NOT NULL,
    description VARCHAR(200) NOT NULL,
    applied_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手工迁移版本记录';

-- 1. 字符集统一为 utf8mb4（否则 emoji 写入报 1366）
--    只为仍有 utf8 的表执行，已 utf8mb4 的跳过
ALTER TABLE clazz               CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE dept                CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE emp                 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE emp_expr            CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE emp_log             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE student             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE hxt_ai_trace        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE hxt_knowledge_chunk CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE hxt_user_ai_profile CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 2. 清理孤儿考勤（clazz_id 指向不存在的班级；无外键，历史脏数据）
DELETE a FROM attendance a LEFT JOIN clazz c ON a.clazz_id = c.id WHERE c.id IS NULL;

-- 3. 补索引（已核对现有索引，避免重复）：
--    attendance 已有 idx_clazz_date，score 已有 uk_exam_student，notice 已有 idx_type_time
CREATE INDEX idx_student_clazz   ON student(clazz_id);
CREATE INDEX idx_pay_status_date ON payment(status, payment_date);

-- 4. 角色（默认 TEACHER；管理员账号显式指定，否则登录后没人能进后台）
ALTER TABLE emp ADD COLUMN role   VARCHAR(20) NOT NULL DEFAULT 'TEACHER';
ALTER TABLE emp ADD COLUMN status TINYINT     NOT NULL DEFAULT 1;
UPDATE emp SET role = 'ADMIN' WHERE username = 'shinaian';

-- 5. 学生账号字段（密码在 B4 用 BCrypt 初始化，此处只建结构）
ALTER TABLE student ADD COLUMN username VARCHAR(50)  DEFAULT NULL;
ALTER TABLE student ADD COLUMN password VARCHAR(100) DEFAULT NULL;
ALTER TABLE student ADD COLUMN status   TINYINT      NOT NULL DEFAULT 1;
CREATE UNIQUE INDEX uk_student_username ON student(username);

-- 6. 学业预警（B8 使用，此处预建，避免后续再改库）
CREATE TABLE hxt_warning_rule (
  id             INT          NOT NULL AUTO_INCREMENT,
  code           VARCHAR(50)  NOT NULL COMMENT 'SCORE_FAIL / ATTEND_LOW / PAY_OVERDUE / VIOLATION',
  name           VARCHAR(100) NOT NULL,
  warn_type      VARCHAR(20)  NOT NULL,
  warn_level     TINYINT      NOT NULL DEFAULT 2,
  condition_json VARCHAR(1000) NOT NULL,
  period_type    VARCHAR(20)  NOT NULL DEFAULT 'DAY',
  enabled        TINYINT      NOT NULL DEFAULT 1,
  version        INT          NOT NULL DEFAULT 1,
  update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则';

CREATE TABLE hxt_warning (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  student_id   INT          NOT NULL,
  clazz_id     INT          DEFAULT NULL,
  rule_code    VARCHAR(50)  NOT NULL,
  warn_type    VARCHAR(20)  NOT NULL,
  warn_level   TINYINT      NOT NULL DEFAULT 2,
  reason       VARCHAR(500) NOT NULL COMMENT '规则生成的事实描述（非模型生成）',
  advice       VARCHAR(500) DEFAULT NULL COMMENT 'LLM 生成的建议（可空）',
  metric_json  VARCHAR(1000) DEFAULT NULL COMMENT '命中时的指标快照',
  dedup_key    VARCHAR(120) NOT NULL COMMENT 'studentId:ruleCode:period',
  status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理 2已忽略',
  handler_id   INT          DEFAULT NULL,
  handle_note  VARCHAR(500) DEFAULT NULL,
  handle_time  TIMESTAMP    NULL DEFAULT NULL,
  create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dedup (dedup_key),
  KEY idx_student_status (student_id, status),
  KEY idx_clazz_status (clazz_id, status),
  KEY idx_type_level (warn_type, warn_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警';

-- 7. 站内信
CREATE TABLE hxt_message (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  receiver_id   INT           NOT NULL,
  receiver_role VARCHAR(20)   NOT NULL,
  biz_type      VARCHAR(30)   NOT NULL COMMENT 'WARNING / NOTICE / SYSTEM',
  biz_id        BIGINT        DEFAULT NULL,
  title         VARCHAR(200)  NOT NULL,
  content       VARCHAR(1000) NOT NULL,
  read_flag     TINYINT       NOT NULL DEFAULT 0,
  create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_receiver (receiver_id, read_flag, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

-- 8. 记账
INSERT INTO schema_version (version, description) VALUES ('V2', 'fix_and_prepare: charset, orphan, index, role, warning, message');
