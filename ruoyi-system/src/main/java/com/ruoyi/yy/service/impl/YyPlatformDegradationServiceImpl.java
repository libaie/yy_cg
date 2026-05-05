package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import com.ruoyi.yy.service.IYyPlatformDegradationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 平台降级回退 Service 实现
 *
 * 当实时融合失败时，从快照表读取历史缓存数据作为兜底。
 */
@Service
public class YyPlatformDegradationServiceImpl implements IYyPlatformDegradationService {

    private static final Logger log = LoggerFactory.getLogger(YyPlatformDegradationServiceImpl.class);

    @Autowired
    private YyProductSnapshotMapper productSnapshotMapper;

    @Override
    public List<YyProductSnapshot> getCachedByPlatform(String platformCode, int limit) {
        List<YyProductSnapshot> snapshots = productSnapshotMapper.selectLatestByPlatform(platformCode, limit);
        log.debug("降级查询 platform={}, limit={}, 返回 {} 条", platformCode, limit,
            snapshots != null ? snapshots.size() : 0);
        return snapshots != null ? snapshots : Collections.emptyList();
    }
}
