package com.ruoyi.yy.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 销售预测 Service 接口
 * 核心功能：热销预测、缺货预警、补货建议
 */
public interface ISalesForecastService {

    // ========== 热销预测 ==========

    /**
     * 15天热销预测
     * @param skuId SKU ID（可选，为空则预测所有商品）
     * @param regionCode 区域编码（可选）
     * @return 预测结果列表
     */
    List<Map<String, Object>> forecast15Days(String skuId, String regionCode);

    /**
     * 30天热销预测
     * @param skuId SKU ID（可选）
     * @param regionCode 区域编码（可选）
     * @return 预测结果列表
     */
    List<Map<String, Object>> forecast30Days(String skuId, String regionCode);

    /**
     * 按品类预测
     * @param categoryId 分类ID
     * @param days 预测天数
     * @param regionCode 区域编码（可选）
     * @return 预测结果列表
     */
    List<Map<String, Object>> forecastByCategory(String categoryId, int days, String regionCode);

    // ========== 热销榜单 ==========

    /**
     * 获取热销TOP榜单
     * @param rankingType 排行类型：daily/weekly/monthly
     * @param regionCode 区域编码（可选）
     * @param limit 返回数量
     * @return 热销商品列表
     */
    List<Map<String, Object>> getHotProductRanking(String rankingType, String regionCode, int limit);

    /**
     * 获取品类热销榜
     * @param categoryId 分类ID
     * @param rankingType 排行类型
     * @param limit 返回数量
     * @return 热销商品列表
     */
    List<Map<String, Object>> getHotProductByCategory(String categoryId, String rankingType, int limit);

    // ========== 缺货预警 ==========

    /**
     * 检查缺货预警
     * @param skuId SKU ID（可选）
     * @param platformCode 平台编码（可选）
     * @return 预警列表
     */
    List<Map<String, Object>> checkStockAlert(String skuId, String platformCode);

    /**
     * 获取预警统计
     * @return 预警统计信息
     */
    Map<String, Object> getAlertStats();

    /**
     * 处理预警
     * @param alertId 预警ID
     * @param action 处理动作：resolve/ignore
     * @param operator 操作人
     * @return 处理结果
     */
    boolean handleAlert(Long alertId, String action, String operator);

    // ========== 补货建议 ==========

    /**
     * 生成补货建议
     * @param skuId SKU ID
     * @param platformCode 平台编码
     * @return 补货建议
     */
    Map<String, Object> generateReplenishmentSuggestion(String skuId, String platformCode);

    /**
     * 批量生成补货建议
     * @param skuIds SKU ID列表（可选，为空则生成所有）
     * @return 补货建议列表
     */
    List<Map<String, Object>> batchReplenishmentSuggestion(List<String> skuIds);

    // ========== 数据采集 ==========

    /**
     * 采集销售历史数据
     * @param platformCode 平台编码
     * @param date 采集日期
     * @return 采集结果
     */
    Map<String, Object> collectSalesHistory(String platformCode, Date date);

    /**
     * 生成热销榜单
     * @param rankingType 排行类型
     * @param date 排行日期
     * @return 生成结果
     */
    Map<String, Object> generateHotRanking(String rankingType, Date date);

    // ========== 统计分析 ==========

    /**
     * 获取销售趋势
     * @param skuId SKU ID
     * @param days 天数
     * @return 趋势数据
     */
    List<Map<String, Object>> getSalesTrend(String skuId, int days);

    /**
     * 获取预测准确率统计
     * @param days 统计天数
     * @return 准确率统计
     */
    Map<String, Object> getForecastAccuracy(int days);
}
