package com.ruoyi.yy.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 字段映射质量指标 Mapper
 * 记录每个平台+API+标准字段的映射成功率、空值率、转换/校验失败率
 */
public interface YyMappingMetricsMapper {

    /**
     * 插入或累加更新一条指标记录
     * 按 (platform_id, api_code, standard_field) 唯一键去重
     */
    void upsertMetrics(Map<String, Object> record);

    /**
     * 查询指定平台的所有指标，按成功率升序排列（最差的排前面）
     */
    List<Map<String, Object>> selectByPlatform(@Param("platformId") Long platformId);
}
