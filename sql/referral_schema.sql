-- 推荐奖励配置
CREATE TABLE yy_referral_config (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  tier_id       BIGINT           COMMENT '关联yy_member_tier表的ID，NULL=全等级通用',
  reward_type   TINYINT          COMMENT '1=固定金额 2=百分比',
  direct_reward DECIMAL(10,2)    COMMENT '直推奖励(金额或百分比)',
  indirect_reward DECIMAL(10,2)  COMMENT '间推奖励(金额或百分比)',
  is_active     TINYINT DEFAULT 1 COMMENT '0=禁用 1=启用',
  remark        VARCHAR(200)     COMMENT '备注',
  create_time   DATETIME,
  update_time   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐奖励配置';

-- 推荐奖励记录
CREATE TABLE yy_referral_reward (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  referrer_id    BIGINT           COMMENT '推荐人用户ID',
  referred_id    BIGINT           COMMENT '被推荐人用户ID',
  referral_depth TINYINT          COMMENT '1=直推 2+=间推层级',
  trigger_type   TINYINT          COMMENT '1=会员充值 2=续费',
  trigger_amount DECIMAL(10,2)    COMMENT '触发充值的订单金额',
  reward_amount  DECIMAL(10,2)    COMMENT '奖励金额',
  calculated_by  TINYINT          COMMENT '计算方式: 1=固定金额 2=百分比',
  status         TINYINT DEFAULT 0 COMMENT '0=待发放 1=已发放 2=已取消',
  config_id      BIGINT           COMMENT '使用的奖励配置ID',
  pay_order_id   BIGINT           COMMENT '关联的支付订单/订阅ID',
  remark         VARCHAR(200),
  create_time    DATETIME,
  update_time    DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐奖励记录';

-- 给 yy_user 加索引推荐人邀请码
CREATE INDEX idx_yy_user_referrer_code ON yy_user(referrer_code);
CREATE INDEX idx_yy_user_invite_code ON yy_user(invite_code);
