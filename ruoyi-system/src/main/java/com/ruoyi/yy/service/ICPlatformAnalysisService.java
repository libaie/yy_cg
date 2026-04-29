package com.ruoyi.yy.service;

import java.util.List;
import java.util.Map;

/**
 * C端平台行情分析 Service 接口
 * 核心功能：C端数据采集、行情分析、选品推荐
 */
public interface ICPlatformAnalysisService {

    // ========== C端数据采集 ==========

    /**
     * 采集C端平台数据
     * @param cplatform C端平台：meituan/jd/tmall/pdd
     * @param keyword 搜索关键词
     * @return 采集结果
     */
    Map<String, Object> collectCPlatformData(String cplatform, String keyword);

    /**
     * 批量采集C端数据
     * @param cplatform C端平台
     * @param keywords 关键词列表
     * @return 采集结果
     */
    Map<String, Object> batchCollectCPlatformData(String cplatform, List<String> keywords);

    // ========== C端数据分析 ==========

    /**
     * 获取C端热销排行
     * @param cplatform C端平台
     * @param rankingType 排行类型：daily/weekly/monthly
     * @param limit 返回数量
     * @return 热销商品列表
     */
    List<Map<String, Object>> getCHotRanking(String cplatform, String rankingType, int limit);

    /**
     * 获取C端价格对比
     * @param productName 商品名称
     * @return 各平台价格对比
     */
    Map<String, Object> getCPriceComparison(String productName);

    /**
     * 获取C端销售趋势
     * @param productName 商品名称
     * @param cplatform C端平台
     * @param days 天数
     * @return 销售趋势数据
     */
    List<Map<String, Object>> getCSalesTrend(String productName, String cplatform, int days);

    // ========== 选品推荐 ==========

    /**
     * 识别高毛利品种
     * @param category 分类（可选）
     * @param limit 返回数量
     * @return 高毛利品种列表
     */
    List<Map<String, Object>> identifyHighMarginProducts(String category, int limit);

    /**
     * 识别C端热销品种
     * @param cplatform C端平台
     * @param limit 返回数量
     * @return 热销品种列表
     */
    List<Map<String, Object>> identifyCHotProducts(String cplatform, int limit);

    /**
     * 生成上架建议
     * @param productName 商品名称
     * @return 上架建议
     */
    Map<String, Object> generateListingSuggestion(String productName);

    /**
     * 推荐最优销售渠道
     * @param productName 商品名称
     * @return 渠道推荐
     */
    Map<String, Object> recommendBestChannel(String productName);

    // ========== 统计分析 ==========

    /**
     * 获取C端行情统计
     * @param cplatform C端平台（可选）
     * @return 统计信息
     */
    Map<String, Object> getCPlatformStats(String cplatform);

    /**
     * 获取价格竞争力分析
     * @param productName 商品名称
     * @return 竞争力分析
     */
    Map<String, Object> getPriceCompetitiveness(String productName);
}
