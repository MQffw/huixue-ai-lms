-- ============================================================
-- Tlias 扩展表（AI Agent Phase 2+ 重构）
-- MySQL 5.5 兼容版（不使用 DATETIME DEFAULT CURRENT_TIMESTAMP）
-- ============================================================

-- 知识库文档表（已有则跳过）
CREATE TABLE IF NOT EXISTS tlias_knowledge_doc (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    file_name    VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  NOT NULL DEFAULT 'faq',
    chunk_count  INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/READY/FAILED',
    file_path    VARCHAR(500) DEFAULT NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='知识库文档主表';

-- 知识库文档分块表（Phase 2 RAG 核心）
CREATE TABLE IF NOT EXISTS tlias_knowledge_chunk (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    doc_id       BIGINT       NOT NULL COMMENT '关联 tlias_knowledge_doc.id',
    chunk_index  INT          NOT NULL DEFAULT 0 COMMENT '文档内的第几块',
    content      TEXT         NOT NULL COMMENT 'chunk 文本内容（500-800字）',
    token_count  INT          DEFAULT 0 COMMENT '估算token数',
    -- Embedding 暂用 JSON 列存储（过渡方案，后续切 Redis Stack）
    embedding_json TEXT        DEFAULT NULL COMMENT '向量数组 JSON（FLOAT[]）',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_doc_chunk (doc_id, chunk_index),
    INDEX idx_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='知识库文档分块表';

-- AI 全链路追踪表（Phase 4.1 Observability）
CREATE TABLE IF NOT EXISTS tlias_ai_trace (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    trace_id     VARCHAR(64)  NOT NULL COMMENT 'UUID，一次请求一个',
    user_id      INT          DEFAULT 0,
    session_id   VARCHAR(100) DEFAULT NULL,
    question     VARCHAR(500) NOT NULL COMMENT '用户原始问题',
    intent       VARCHAR(30)  DEFAULT NULL COMMENT '路由到的意图枚举',
    model_type   VARCHAR(20)  DEFAULT NULL COMMENT '实际使用的主模型',
    fallback     TINYINT(1)   DEFAULT 0 COMMENT '是否使用了降级模型',
    tool_calls   INT          DEFAULT 0 COMMENT '工具调用次数',
    rag_hit      TINYINT(1)   DEFAULT 0 COMMENT '是否命中知识库',
    answer_chars INT          DEFAULT 0 COMMENT '最终回答字数',
    latency_ms   INT          DEFAULT 0 COMMENT '端到端耗时(毫秒）',
    status       VARCHAR(20)  DEFAULT 'SUCCESS' COMMENT 'SUCCESS/ERROR/FALLBACK',
    error_msg    VARCHAR(500) DEFAULT NULL,
    detail_json  TEXT         DEFAULT NULL COMMENT '完整上下文（Intent / ToolNames / RAG scores / PromptSnapshot）',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_trace (trace_id),
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_intent (intent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='AI请求全链路追踪';

-- 用户画像表（Phase 3.1 UserProfile）
CREATE TABLE IF NOT EXISTS tlias_user_ai_profile (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         INT          NOT NULL COMMENT 'emp.id',
    favorite_tools  TEXT         DEFAULT NULL COMMENT '最常使用的工具 {"countStudentsByDegree": 5, ...}',
    common_intents  TEXT         DEFAULT NULL COMMENT '意图分布 {"DATA_STATS": 12, "CHAT": 3}',
    tags            VARCHAR(500) DEFAULT NULL COMMENT '学习标签，逗号分隔（如"就业,考勤"）',
    last_active_at  VARCHAR(30)  DEFAULT NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='AI助手用户画像';

-- Token 用量统计表（已有则跳过）
CREATE TABLE IF NOT EXISTS tlias_token_usage (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           INT          NOT NULL DEFAULT 0,
    session_id        VARCHAR(100) DEFAULT NULL,
    model_name        VARCHAR(50)  DEFAULT 'unknown',
    prompt_tokens     INT          DEFAULT 0,
    completion_tokens INT          DEFAULT 0,
    total_tokens      INT          DEFAULT 0,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Token 用量统计';

-- 工具调用日志表（已有则跳过）
CREATE TABLE IF NOT EXISTS tlias_tool_call_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL DEFAULT 0,
    session_id   VARCHAR(100) DEFAULT NULL,
    tool_name    VARCHAR(100) NOT NULL,
    tool_args    TEXT         DEFAULT NULL,
    success      TINYINT(1)   DEFAULT 1,
    duration_ms  INT          DEFAULT 0,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_tool (tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工具调用日志';
