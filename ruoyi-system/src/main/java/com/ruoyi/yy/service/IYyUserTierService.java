package com.ruoyi.yy.service;

/**
 * 用户会员等级查询服务
 */
public interface IYyUserTierService {
    /**
     * 获取用户当前会员等级
     * @param userId 用户ID
     * @return 0=普通 1=黄金 2=铂金 3=钻石
     */
    int getUserTierLevel(Long userId);
}
