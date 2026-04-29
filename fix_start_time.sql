-- 修复 yy_member_subscription 表的 start_time 字段，允许 NULL
ALTER TABLE yy_member_subscription MODIFY COLUMN start_time DATETIME NULL COMMENT '订阅开始时间';

-- 同时检查 end_time 是否也有同样问题
ALTER TABLE yy_member_subscription MODIFY COLUMN end_time DATETIME NULL COMMENT '订阅结束时间';
