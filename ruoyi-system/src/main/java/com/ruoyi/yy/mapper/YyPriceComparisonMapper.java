package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPriceComparison;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 比价记录 Mapper
 */
public interface YyPriceComparisonMapper {
    
    /**
     * 查询比价记录
     */
    YyPriceComparison selectYyPriceComparisonById(Long id);
    
    /**
     * 按SKU和平台查询最新比价记录
     */
    List<YyPriceComparison> selectBySkuAndPlatform(
        @Param("skuId") String skuId,
        @Param("sourcePlatform") String sourcePlatform,
        @Param("customerType") String customerType
    );
    
    /**
     * 按条件查询比价记录
     */
    List<YyPriceComparison> selectYyPriceComparisonList(YyPriceComparison query);
    
    /**
     * 批量查询比价记录（按SKU ID列表）
     */
    List<YyPriceComparison> selectBySkuIds(
        @Param("skuIds") List<String> skuIds,
        @Param("customerType") String customerType
    );
    
    /**
     * 查询商品在各平台的最新价格
     */
    List<YyPriceComparison> selectLatestPricesBySku(
        @Param("skuId") String skuId,
        @Param("customerType") String customerType
    );
    
    /**
     * 插入比价记录
     */
    int insertYyPriceComparison(YyPriceComparison yyPriceComparison);
    
    /**
     * 批量插入比价记录
     */
    int batchInsert(List<YyPriceComparison> list);
    
    /**
     * 更新比价记录
     */
    int updateYyPriceComparison(YyPriceComparison yyPriceComparison);
    
    /**
     * 删除比价记录
     */
    int deleteYyPriceComparisonById(Long id);
    
    /**
     * 批量删除比价记录
     */
    int deleteYyPriceComparisonByIds(Long[] ids);
    
    /**
     * 查询价格趋势
     */
    List<Map<String, Object>> selectPriceTrend(
        @Param("skuId") String skuId,
        @Param("platformCode") String platformCode,
        @Param("days") int days
    );
    
    /**
     * 查询比价统计
     */
    Map<String, Object> selectComparisonStats(
        @Param("customerType") String customerType
    );
    
    /**
     * 清理过期数据
     */
    int deleteExpiredData(@Param("expireDays") int expireDays);
}
