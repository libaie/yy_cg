package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import com.ruoyi.yy.service.IYyProductSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 平台商品快照 Service 实现
 *
 * 负责快照的持久化，并在快照写入后双写至价格历史时间序列表。
 */
@Service
public class YyProductSnapshotServiceImpl implements IYyProductSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(YyProductSnapshotServiceImpl.class);

    @Autowired
    private YyProductSnapshotMapper productSnapshotMapper;

    @Autowired(required = false)
    private YyPriceSnapshotServiceImpl priceSnapshotService;

    @Override
    public void saveSnapshot(YyProductSnapshot snapshot) {
        productSnapshotMapper.batchInsertYyProductSnapshot(Collections.singletonList(snapshot));
    }

    @Override
    public void saveBatch(List<YyProductSnapshot> snapshots) {
        if (snapshots != null && !snapshots.isEmpty()) {
            productSnapshotMapper.batchInsertYyProductSnapshot(snapshots);
        }
    }

    /**
     * 从快照双写价格历史记录。
     * 委托给 YyPriceSnapshotServiceImpl，失败时静默降级，不阻断主流程。
     */
    @Override
    public void appendPriceHistory(YyProductSnapshot snapshot) {
        if (priceSnapshotService == null) {
            return;
        }
        try {
            priceSnapshotService.appendPriceHistory(snapshot);
        } catch (Exception e) {
            log.warn("[PriceHistory] 双写失败，降级跳过: sku={}", snapshot.getSkuId(), e);
        }
    }
}
