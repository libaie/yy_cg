package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyProductSnapshot;

import java.util.List;

/**
 * 平台降级回退 Service 接口
 *
 * 当实时融合失败时，从快照表读取历史缓存数据作为兜底。
 */
public interface IYyPlatformDegradationService {

    /**
     * 查询某平台最近采集的快照缓存，用于降级回退
     *
     * @param platformCode 平台 code
     * @param limit        最大返回条数
     * @return 快照列表
     */
    List<YyProductSnapshot> getCachedByPlatform(String platformCode, int limit);
}
