package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.vo.PriceComparisonVO;

import java.util.List;
import java.util.Map;

/**
 * 标准商品 Service
 */
public interface IYyStandardProductService {

    YyStandardProduct selectYyStandardProductById(Long id);

    List<YyStandardProduct> selectYyStandardProductList(YyStandardProduct yyStandardProduct);

    List<YyStandardProduct> selectByFusionGroupId(Long fusionGroupId);

    int insertYyStandardProduct(YyStandardProduct yyStandardProduct);

    int updateYyStandardProduct(YyStandardProduct yyStandardProduct);

    int deleteYyStandardProductByIds(Long[] ids);

    int deleteYyStandardProductById(Long id);

    // ========== 比价功能（增量扩展） ==========

    /**
     * 根据SKU ID查询商品在各平台的价格（比价核心）
     * @param skuId SKU ID
     * @param sourcePlatform 来源平台（可选，为空则查所有平台）
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectBySkuId(String skuId, String sourcePlatform);

    /**
     * 根据融合键查询同一药品在各平台的价格
     * @param fusionKey 融合键
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectByFusionKey(String fusionKey);

    /**
     * 根据通用名查询药品在各平台的价格
     * @param commonName 通用名
     * @return 各平台价格列表
     */
    List<YyStandardProduct> selectByCommonName(String commonName);

    /**
     * 获取商品比价结果（按到手价排序）
     * @param productId 商品ID或SKU ID
     * @param customerType 客户业态
     * @return 比价结果列表
     */
    List<PriceComparisonVO> comparePrices(String productId, String customerType);

    /**
     * 批量商品比价
     * @param productIds 商品ID列表
     * @param customerType 客户业态
     * @return key=productId, value=比价结果列表
     */
    Map<String, List<PriceComparisonVO>> batchComparePrices(List<String> productIds, String customerType);

    /**
     * 获取最优平台推荐
     * @param productId 商品ID
     * @param customerType 客户业态
     * @return 推荐结果
     */
    Map<String, Object> recommendPlatform(String productId, String customerType);
}
