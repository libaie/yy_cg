package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyProductSnapshot;

import java.util.List;

/**
 * 平台商品快照 Service 接口
 *
 * 负责快照的持久化与价格历史双写。
 */
public interface IYyProductSnapshotService {

    /**
     * 保存单条快照
     *
     * @param snapshot 商品快照
     */
    void saveSnapshot(YyProductSnapshot snapshot);

    /**
     * 批量保存快照
     *
     * @param snapshots 商品快照列表
     */
    void saveBatch(List<YyProductSnapshot> snapshots);

    /**
     * 从快照双写价格历史记录（try-catch 静默降级）
     *
     * @param snapshot 商品快照
     */
    void appendPriceHistory(YyProductSnapshot snapshot);
}
