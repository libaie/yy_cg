package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.mapper.YyMappingMetricsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段映射质量指标异步记录器
 *
 * 所有字段映射管道的执行结果通过 {@link #record} 写入内存缓冲区，
 * 每 60 秒由定时任务批量 flush 到数据库，避免映射热路径阻塞在 DB 写入上。
 */
@Component
public class YyMappingMetricsRecorder {

    private static final Logger log = LoggerFactory.getLogger(YyMappingMetricsRecorder.class);

    @Autowired
    private YyMappingMetricsMapper metricsMapper;

    private volatile ConcurrentHashMap<String, Map<String, Object>> buffer = new ConcurrentHashMap<>();

    /**
     * 记录一次字段映射尝试的结果。
     *
     * @param platformId       平台 ID
     * @param apiCode          API 编码（可为 null）
     * @param standardField    标准字段名
     * @param success          是否映射成功
     * @param wasNull          原始值是否为 null
     * @param transformFailed  类型转换是否失败
     * @param validationFailed 校验是否失败
     * @param sampleValue      采样值（仅成功时有意义的值）
     */
    public void record(Long platformId, String apiCode, String standardField,
                       boolean success, boolean wasNull,
                       boolean transformFailed, boolean validationFailed,
                       String sampleValue) {
        String normalizedApiCode = apiCode != null ? apiCode : "";
        String key = platformId + "|" + normalizedApiCode + "|" + standardField;
        buffer.compute(key, (k, v) -> {
            if (v == null) {
                v = new HashMap<>();
                v.put("platformId", platformId);
                v.put("apiCode", normalizedApiCode);
                v.put("standardField", standardField);
                v.put("totalAttempts", 1L);
                v.put("successCount", success ? 1L : 0L);
                v.put("nullCount", wasNull ? 1L : 0L);
                v.put("transformFailCount", transformFailed ? 1L : 0L);
                v.put("validationFailCount", validationFailed ? 1L : 0L);
            } else {
                v.put("totalAttempts", (Long) v.get("totalAttempts") + 1);
                if (success) {
                    v.put("successCount", (Long) v.get("successCount") + 1);
                }
                if (wasNull) {
                    v.put("nullCount", (Long) v.get("nullCount") + 1);
                }
                if (transformFailed) {
                    v.put("transformFailCount", (Long) v.get("transformFailCount") + 1);
                }
                if (validationFailed) {
                    v.put("validationFailCount", (Long) v.get("validationFailCount") + 1);
                }
            }
            if (sampleValue != null) {
                v.put("lastValueSample", sampleValue);
            }
            return v;
        });
    }

    /**
     * 每 60 秒将内存缓冲区批量写入数据库。
     */
    @Scheduled(fixedRate = 60000)
    public void flush() {
        // 原子替换整个 Map 引用，避免与 record() 的 ConcurrentHashMap.compute() 产生竞态
        ConcurrentHashMap<String, Map<String, Object>> old = buffer;
        buffer = new ConcurrentHashMap<>();
        if (old.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> snapshot = new HashMap<>(old);
        int successCount = 0;
        for (Map<String, Object> record : snapshot.values()) {
            try {
                metricsMapper.upsertMetrics(record);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to upsert metrics for platform={}, field={}",
                        record.get("platformId"), record.get("standardField"), e);
            }
        }
        if (successCount > 0) {
            log.debug("Flushed {} mapping metrics records to DB", successCount);
        }
    }
}
