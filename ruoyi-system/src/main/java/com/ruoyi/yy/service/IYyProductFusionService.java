package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyFusionResult;
import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.dto.MappedProductDTO;

/**
 * 商品融合服务
 *
 * 负责融合键生成、融合分组管理、标准商品入库以及融合分组的聚合信息更新。
 *
 * @author fdAgent
 */
public interface IYyProductFusionService {

    /**
     * 对一个商品快照执行融合匹配
     *
     * @param snapshot 商品快照
     * @return 融合结果
     */
    YyFusionResult fuse(YyProductSnapshot snapshot);

    /**
     * 获取或创建融合分组，并 upsert 标准商品记录
     *
     * @param dto          已映射的商品 DTO
     * @param platformCode 平台编码
     * @return 融合分组
     */
    YyProductFusionGroup getOrCreateGroup(MappedProductDTO dto, String platformCode);

    /**
     * 更新融合分组的聚合信息（platform_count、min_price、total_stock）
     *
     * @param groupId 融合分组 ID
     */
    void updateAggregation(Long groupId);
}
