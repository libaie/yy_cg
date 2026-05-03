package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyProductSnapshot;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

/**
 * 平台商品快照Mapper接口
 */
public interface YyProductSnapshotMapper {

    YyProductSnapshot selectYyProductSnapshotById(Long id);

    YyProductSnapshot selectYyProductSnapshotByPlatformSku(
        @Param("platformCode") String platformCode,
        @Param("skuId") String skuId
    );

    List<YyProductSnapshot> selectYyProductSnapshotByDrugId(@Param("drugId") Long drugId);

    int insertYyProductSnapshot(YyProductSnapshot record);

    int updateYyProductSnapshotDrugBinding(
        @Param("id") Long id,
        @Param("drugId") Long drugId,
        @Param("fusionConfidence") BigDecimal fusionConfidence
    );

    void batchInsertYyProductSnapshot(@Param("list") List<YyProductSnapshot> list);

    List<YyProductSnapshot> selectYyProductSnapshotByPlatformAndSkuIds(
        @Param("platform") String platform,
        @Param("skuIds") List<String> skuIds
    );
}
