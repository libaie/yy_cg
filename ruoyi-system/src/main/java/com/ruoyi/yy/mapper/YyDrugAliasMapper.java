package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyDrugAlias;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 药品平台别名Mapper接口
 */
public interface YyDrugAliasMapper {

    /**
     * 按平台+SKU查询缓存映射
     */
    YyDrugAlias selectYyDrugAliasByPlatformSku(
        @Param("platformCode") String platformCode,
        @Param("skuId") String skuId
    );

    int insertYyDrugAlias(YyDrugAlias record);

    int updateYyDrugAlias(YyDrugAlias record);

    int updateLastVerifiedAt(@Param("id") Long id, @Param("verifiedAt") Date verifiedAt);

    int deleteYyDrugAliasById(@Param("id") Long id);

    int updateConfidence(
        @Param("id") Long id,
        @Param("confidence") BigDecimal confidence,
        @Param("matchMethod") String matchMethod
    );
}
