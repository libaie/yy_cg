CREATE TABLE IF NOT EXISTS yy_ai_prompt_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    scene VARCHAR(50) NOT NULL COMMENT '场景: match/advisor/evaluator/search/cleaner',
    system_prompt TEXT NOT NULL COMMENT '系统提示词',
    user_prompt_template TEXT NOT NULL COMMENT '用户提示词模板(含占位符)',
    model VARCHAR(50) DEFAULT 'qwen-turbo' COMMENT '使用的模型',
    temperature DECIMAL(3,2) DEFAULT 0.1 COMMENT '温度参数',
    max_tokens INT DEFAULT 1000 COMMENT '最大token数',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Prompt模板表';
