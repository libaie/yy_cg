-- ============================================
-- 推荐系统 - 菜单 & 权限 SQL
-- 执行前请确认父级菜单 component 路径正确
-- ============================================

-- 1. 先查询你现有 yy 模块的父菜单 ID（假设"医药助手"或类似的一级/二级菜单）
-- 请根据实际情况修改 parent_id
-- 这里假设 parent_id = 2000，请替换为实际的 yy 模块菜单 ID

SET @parentId = (SELECT menu_id FROM sys_menu WHERE perms = 'yy:client:access' OR menu_name = '医药助手' OR component LIKE '%yy%' LIMIT 1);
-- 如果上面查不到，可以手动设为已知的父菜单 ID，例如：
-- SET @parentId = 2000;

-- ============================================
-- 推荐奖励配置（菜单）
-- ============================================
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, 
    is_frame, is_cache, menu_type, visible, status, 
    perms, icon, create_by, create_time, remark
) VALUES (
    '推荐奖励配置', @parentId, 6, 'referralConfig', 'yy/referralConfig/index',
    1, 0, 'C', '0', '0', 
    'yy:config:list', 'tree', 'admin', NOW(), '推荐奖励配置管理'
);

-- 获取上面插入的菜单 ID
SET @configMenuId = LAST_INSERT_ID();

-- 推荐配置 - 按钮权限
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, 
    is_frame, menu_type, visible, status, perms, 
    create_by, create_time
) VALUES 
('推荐配置查询', @configMenuId, 1, '#', '', 1, 'F', '0', '0', 'yy:config:query', 'admin', NOW()),
('推荐配置新增', @configMenuId, 2, '#', '', 1, 'F', '0', '0', 'yy:config:add', 'admin', NOW()),
('推荐配置修改', @configMenuId, 3, '#', '', 1, 'F', '0', '0', 'yy:config:edit', 'admin', NOW()),
('推荐配置删除', @configMenuId, 4, '#', '', 1, 'F', '0', '0', 'yy:config:remove', 'admin', NOW()),
('推荐配置导出', @configMenuId, 5, '#', '', 1, 'F', '0', '0', 'yy:config:export', 'admin', NOW());

-- ============================================
-- 推荐奖励记录（菜单）
-- ============================================
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, 
    is_frame, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
) VALUES (
    '推荐奖励记录', @parentId, 7, 'referralReward', 'yy/referralReward/index',
    1, 'C', '0', '0', 'yy:reward:list', 'money', 'admin', NOW(), '推荐奖励流水查询'
);

SET @rewardMenuId = LAST_INSERT_ID();

-- 推荐奖励 - 按钮权限
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, 
    is_frame, menu_type, visible, status, perms, 
    create_by, create_time
) VALUES 
('奖励记录查询', @rewardMenuId, 1, '#', '', 1, 'F', '0', '0', 'yy:reward:query', 'admin', NOW());
