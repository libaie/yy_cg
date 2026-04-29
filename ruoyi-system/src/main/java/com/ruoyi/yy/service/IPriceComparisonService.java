package com.ruoyi.yy.service;

import com.ruoyi.yy.dto.PriceComparisonDTO;
import com.ruoyi.yy.dto.PurchasePlanDTO;
import com.ruoyi.yy.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 多平台比价服务接口
 * 核心职责：同品多平台比价、活动价格计算、最优平台推荐、采购组合建议
 */
public interface IPriceComparisonService {

    // ========== 商品比价 ==========

    /**
     * 同品多平台比价
     * @param skuId SKU ID
     * @param customerType 客户业态：single/chain/clinic/wholesale
     * @return 比价结果列表（按总价升序）
     */
    List<PriceComparisonVO> comparePrices(String skuId, String customerType);

    /**
     * 批量商品比价
     * @param skuIds SKU ID列表
     * @param customerType 客户业态
     * @return key=skuId, value=比价结果列表
     */
    Map<String, List<PriceComparisonVO>> batchComparePrices(List<String> skuIds, String customerType);

    /**
     * 按条件比价（支持搜索）
     * @param dto 比价查询条件
     * @return 比价结果列表
     */
    List<PriceComparisonVO> comparePricesByCondition(PriceComparisonDTO dto);

    // ========== 价格计算 ==========

    /**
     * 计算活动价格
     * @param basePrice 基础价格
     * @param activityType 活动类型
     * @param activityRules 活动规则JSON
     * @param orderQty 订购数量
     * @param customerType 客户业态
     * @return 活动后价格
     */
    java.math.BigDecimal calculateActivityPrice(
        java.math.BigDecimal basePrice, 
        String activityType, 
        String activityRules,
        Integer orderQty,
        String customerType
    );

    /**
     * 计算凑单满减
     * @param totalAmount 订单总金额
     * @param platformCode 平台编码
     * @return 优惠金额
     */
    java.math.BigDecimal calculateFullReduction(java.math.BigDecimal totalAmount, String platformCode);

    /**
     * 计算拼团价格
     * @param basePrice 基础价格
     * @param activityRules 拼团规则
     * @param currentGroupSize 当前成团人数
     * @return 拼团价格
     */
    java.math.BigDecimal calculateGroupBuyPrice(
        java.math.BigDecimal basePrice, 
        String activityRules,
        Integer currentGroupSize
    );

    // ========== 推荐与优化 ==========

    /**
     * 最优平台推荐
     * @param skuId SKU ID
     * @param customerType 客户业态
     * @param regionCode 区域编码（可选，影响运费）
     * @return 推荐结果
     */
    PlatformRecommendationVO recommendPlatform(String skuId, String customerType, String regionCode);

    /**
     * 生成采购组合方案
     * @param skuIds 采购商品列表
     * @param customerType 客户业态
     * @param budget 预算（可选）
     * @return 采购方案
     */
    PurchasePlanVO generatePurchasePlan(List<String> skuIds, String customerType, java.math.BigDecimal budget);

    /**
     * 优化采购组合（考虑拼团+凑单）
     * @param plan 原始采购方案
     * @return 优化后的方案
     */
    PurchasePlanVO optimizePurchasePlan(PurchasePlanVO plan);

    // ========== 数据查询 ==========

    /**
     * 获取商品历史价格趋势
     * @param skuId SKU ID
     * @param platformCode 平台编码
     * @param days 天数
     * @return 价格趋势数据
     */
    List<PriceTrendVO> getPriceTrend(String skuId, String platformCode, int days);

    /**
     * 获取平台活动列表
     * @param platformCode 平台编码（可选）
     * @param activeOnly 是否只查有效活动
     * @return 活动列表
     */
    List<PlatformActivityVO> getPlatformActivities(String platformCode, boolean activeOnly);

    /**
     * 获取比价统计
     * @param customerType 客户业态
     * @return 统计信息
     */
    PriceComparisonStatsVO getComparisonStats(String customerType);

    // ========== 数据采集 ==========

    /**
     * 采集平台价格数据
     * @param platformCode 平台编码
     * @param skuIds SKU ID列表（可选，为空则采集全部）
     * @return 采集结果
     */
    Map<String, Object> collectPriceData(String platformCode, List<String> skuIds);

    /**
     * 批量导入比价数据
     * @param comparisonList 比价数据列表
     * @return 导入结果
     */
    Map<String, Object> importPriceComparisons(List<PriceComparisonVO> comparisonList);
}
