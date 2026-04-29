-- 会员订阅订单表新增支付时间字段
ALTER TABLE `yy_member_subscription` 
ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '支付时间' AFTER `pay_expire_time`;
