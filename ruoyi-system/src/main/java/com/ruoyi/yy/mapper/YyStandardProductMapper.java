package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyStandardProduct;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 标准商品 Mapper
 */
public interface YyStandardProductMapper {

    YyStandardProduct selectYyStandardProductById(Long id);

    YyStandardProduct selectBySkuId(@Param("sourcePlatform") String sourcePlatform,
                                    @Param("skuId") String skuId);

    List<YyStandardProduct> selectYyStandardProductList(YyStandardProduct yyStandardProduct);

    List<YyStandardProduct> selectByFusionGroupId(Long fusionGroupId);

    int insertYyStandardProduct(YyStandardProduct yyStandardProduct);

    int updateYyStandardProduct(YyStandardProduct yyStandardProduct);

    /**
     * 插入或更新（按 source_platform + sku_id 去重）
     */
    int upsertYyStandardProduct(YyStandardProduct yyStandardProduct);

    int deleteYyStandardProductByIds(Long[] ids);

    int deleteYyStandardProductById(Long id);

    // ========== 比价查询（增量扩展） ==========

    /**
     * 根据SKU ID查询各平台价格（比价核心）
     * @param skuId SKU ID
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectAllPlatformsBySkuId(@Param("skuId") String skuId);

    /**
     * 根据融合键查询各平台价格
     * @param fusionKey 融合键
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectByFusionKey(@Param("fusionKey") String fusionKey);

    /**
     * 根据通用名查询各平台价格
     * @param commonName 通用名
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectByCommonName(@Param("commonName") String commonName);

    /**
     * 批量查询SKU在各平台的价格
     * @param skuIds SKU ID列表
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectBatchBySkuIds(@Param("skuIds") List<String> skuIds);

    /**
     * 查询有活动的商品
     * @param platformCode 平台编码（可选）
     * @return 有活动的商品列表
     */
    List<YyStandardProduct> selectProductsWithActivity(@Param("platformCode") String platformCode);

    /**
     * 统计各平台商品数量
     * @return 平台统计结果
     */
    List<Map<String, Object>> selectPlatformStats();
}
