package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.dto.MappedProductDTO;
import com.ruoyi.yy.dto.YyDataIngestDTO;
import com.ruoyi.yy.model.MappingResult;
import com.ruoyi.yy.service.IDataIngestService;
import com.ruoyi.yy.service.IYyPlatformDecryptService;
import com.ruoyi.yy.service.IYyPlatformDegradationService;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.yy.service.IYyProductFusionService;
import com.ruoyi.yy.service.IYyProductSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 数据接入编排器 V2
 *
 * <p>将原来的 God Class {@link DataFusionServiceImpl} 拆分为管道式架构，
 * 把解密、映射、融合、快照、价格历史各步骤委托给独立服务，编排器仅负责串联流程。</p>
 *
 * <p>通过 {@code app.data-ingest.legacy-mode=false}（默认）激活；
 * 设置 {@code true} 则回退到旧的 {@link DataFusionServiceImpl}。</p>
 *
 * @author fdAgent
 * @date 2026-05-06
 */
@Service("dataIngestService")
@Primary
@ConditionalOnProperty(name = "app.data-ingest.legacy-mode", havingValue = "false", matchIfMissing = true)
public class DataIngestOrchestrator implements IDataIngestService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestOrchestrator.class);

    /** 自动检测 JSON 数组时尝试的常见字段名 */
    private static final String[] ARRAY_DETECT_KEYS = {"items", "products", "goods", "data", "list", "result", "rows"};

    @Autowired
    private IYyPlatformService platformService;

    @Autowired
    private IYyPlatformDecryptService decryptService;

    @Autowired
    private YyFieldMappingEngine mappingEngine;

    @Autowired
    private IYyProductFusionService fusionService;

    @Autowired
    private IYyProductSnapshotService snapshotService;

    @Autowired
    private IYyPlatformDegradationService degradationService;

    // ======================== ingest ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ingest(YyDataIngestDTO dto) {
        String platformCode = dto.getPlatformCode();

        try {
            return doIngest(dto);
        } catch (Exception e) {
            log.error("Ingest pipeline failed for platform={}", dto.getPlatformCode(), e);
            return degradedFallback(platformCode);
        }
    }

    /**
     * 核心管道流程（从 ingest 抽离，便于 try-catch 包裹）
     */
    private Map<String, Object> doIngest(YyDataIngestDTO dto) {
        String platformCode = dto.getPlatformCode();
        String apiCode = dto.getApiCode();

        // ====== Step 1: 解密 ======
        int encryptType = dto.getDataEncryptType() != null ? dto.getDataEncryptType() : -1;
        String decryptedJson = decryptService.decrypt(platformCode, dto.getEncryptData(), encryptType);
        if (decryptedJson == null) {
            return buildResult(0, 0, 0, "解密失败");
        }

        // ====== Step 2: 加载平台 ======
        YyPlatform platform = platformService.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            return buildResult(0, 0, 0, "平台不存在: " + platformCode);
        }

        // ====== Step 3: 解析 JSON 数组 ======
        List<JSONObject> items = detectJsonArray(decryptedJson, null);
        if (items.isEmpty()) {
            return buildResult(0, 0, 0, "未解析到商品数据");
        }

        // ====== Step 4: 字段映射（V2 引擎批量执行） ======
        List<MappingResult> results = mappingEngine.executeBatch(items, platform.getPId(), apiCode);

        // ====== Step 5: 融合 + 快照 + 价格历史 ======
        Set<Long> touchedGroupIds = new HashSet<>();
        int totalMapped = 0;

        for (MappingResult result : results) {
            if (result.hasRequiredFieldFailures()) {
                log.warn("MappingResult has required field failures: {} for platform={}, skipping",
                        result.getRequiredFieldFailures(), platformCode);
                continue;
            }

            // 5a: 转换为 DTO
            MappedProductDTO productDto = mappingEngine.convertToProduct(result, platformCode);

            // 5b: 获取或创建融合分组
            YyProductFusionGroup group = fusionService.getOrCreateGroup(productDto, platformCode);
            if (group != null) {
                touchedGroupIds.add(group.getId());

                // 5c: 构建快照
                YyProductSnapshot snapshot = buildSnapshot(productDto, platformCode, apiCode);

                // 5d: 保存快照
                snapshotService.saveSnapshot(snapshot);

                // 5e: 双写价格历史
                snapshotService.appendPriceHistory(snapshot);

                totalMapped++;
            }
        }

        // ====== Step 6: 更新所有受影响的融合分组聚合 ======
        for (Long groupId : touchedGroupIds) {
            fusionService.updateAggregation(groupId);
        }

        return buildResult(totalMapped, touchedGroupIds.size(), 0, "融合成功");
    }

    // ======================== degradedFallback ========================

    /**
     * 降级回退：实时融合失败时，查询历史缓存快照作为兜底数据。
     */
    private Map<String, Object> degradedFallback(String platformCode) {
        List<YyProductSnapshot> snapshots = degradationService.getCachedByPlatform(platformCode, 100);

        if (snapshots.isEmpty()) {
            return buildResult(0, 0, 0, "平台数据获取失败，且无历史缓存可用");
        }

        List<YyStandardProduct> degradedProducts = new ArrayList<>();
        for (YyProductSnapshot s : snapshots) {
            YyStandardProduct p = new YyStandardProduct();
            p.setSourcePlatform(s.getSourcePlatform());
            p.setSkuId(s.getSkuId());
            p.setProductId(s.getProductId());
            p.setSourceApi(s.getSourceApi());
            p.setCommonName(s.getCommonName());
            p.setBarcode(s.getBarcode());
            p.setApprovalNumber(s.getApprovalNumber());
            p.setManufacturer(s.getManufacturer());
            p.setSpecification(s.getSpecification());
            p.setPriceCurrent(s.getPriceCurrent());
            p.setStockQuantity(s.getStockQuantity());
            p.setCollectedAt(s.getCollectedAt());
            p.setDegraded(true);
            degradedProducts.add(p);
        }

        Map<String, Object> result = buildResult(degradedProducts.size(), 0, 0,
                "实时采集失败(" + platformCode + ")，已回退到历史缓存数据");

        result.put("degraded", true);
        result.put("degradedSource", "yy_product_snapshot");
        result.put("degradedProducts", degradedProducts);

        log.info("降级回退完成 platform={}: 返回 {} 条缓存快照.", platformCode, degradedProducts.size());
        return result;
    }

    // ======================== mapOnly ========================

    @Override
    public Map<String, Object> mapOnly(String platformCode, String decryptedJson, String entryPath) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> mappedList = new ArrayList<>();

        YyPlatform platform = platformService.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            log.warn("mapOnly: 平台不存在 {}", platformCode);
            result.put("products", mappedList);
            result.put("message", "平台不存在: " + platformCode);
            return result;
        }

        // 解析 JSON 并检测数组
        List<JSONObject> items = detectJsonArray(decryptedJson, entryPath);

        if (items.isEmpty()) {
            result.put("products", mappedList);
            result.put("mappedCount", 0);
            result.put("message", "未解析到商品数据");
            return result;
        }

        // V2 引擎批量映射
        List<MappingResult> results = mappingEngine.executeBatch(items, platform.getPId(), null);

        // 收集字段映射结果并统计
        Set<String> hitFields = new HashSet<>();
        for (MappingResult mr : results) {
            Map<String, Object> map = new LinkedHashMap<>(mr.getFields());
            map.values().removeIf(Objects::isNull);
            hitFields.addAll(map.keySet());
            mappedList.add(map);
        }

        result.put("products", mappedList);
        result.put("mappedCount", mappedList.size());
        result.put("hitFields", hitFields.size());
        result.put("hitFieldNames", hitFields);
        result.put("message", mappedList.isEmpty() ? "未解析到商品数据" : "映射成功");

        return result;
    }

    // ======================== 私有工具方法 ========================

    /**
     * 从解密后的 JSON 中检测并提取商品对象数组。
     *
     * <p>支持以下格式：
     * <ul>
     *   <li>顶层数组：[{...}, {...}]</li>
     *   <li>嵌套路径：{"data": {"wholesales": [{...}]}}（通过 entryPath 指定）</li>
     *   <li>自动检测常见字段名：items, products, goods, data, list, result, rows</li>
     *   <li>兜底：将整个 JSON 对象作为单元素处理</li>
     * </ul>
     *
     * @param json      解密后的 JSON 字符串
     * @param entryPath 数据入口路径（点号分隔），nullable
     * @return JSONObject 列表，不会为 null
     */
    private List<JSONObject> detectJsonArray(String json, String entryPath) {
        List<JSONObject> result = new ArrayList<>();

        try {
            Object parsed = JSON.parse(json);
            List<?> itemArray = null;

            if (parsed instanceof JSONArray) {
                // 顶层数组
                itemArray = (JSONArray) parsed;
            } else if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;

                // 按 entryPath 定位
                if (entryPath != null && !entryPath.isEmpty()) {
                    Object target = getByPath(obj, entryPath);
                    if (target instanceof JSONArray) {
                        itemArray = (JSONArray) target;
                    } else if (target instanceof JSONObject) {
                        result.add((JSONObject) target);
                        return result;
                    }
                }

                // 自动检测常见字段名
                if (itemArray == null) {
                    for (String key : ARRAY_DETECT_KEYS) {
                        Object val = obj.get(key);
                        if (val instanceof JSONArray) {
                            itemArray = (JSONArray) val;
                            break;
                        }
                    }
                }

                // 兜底：整个对象作为单元素
                if (itemArray == null) {
                    result.add(obj);
                    return result;
                }
            }

            if (itemArray != null) {
                for (Object element : itemArray) {
                    if (element instanceof JSONObject) {
                        result.add((JSONObject) element);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON失败", e);
        }

        return result;
    }

    /**
     * 按点号分隔的路径从 JSONObject 中取值。
     *
     * @param obj  JSON 对象
     * @param path 点号分隔的路径，如 "data.wholesales"
     * @return 路径指向的值，未找到返回 null
     */
    private Object getByPath(JSONObject obj, String path) {
        if (path == null || path.isEmpty()) {
            return obj;
        }
        String[] segments = path.split("\\.");
        Object current = obj;
        for (String seg : segments) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(seg);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * 从映射后的 DTO 构建平台商品快照对象。
     */
    private YyProductSnapshot buildSnapshot(MappedProductDTO dto, String platformCode, String apiCode) {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform(platformCode);
        snapshot.setSkuId(dto.getSkuId());
        snapshot.setProductId(dto.getProductId());
        snapshot.setSourceApi(apiCode);
        snapshot.setCommonName(dto.getCommonName());
        snapshot.setBarcode(dto.getBarcode());
        snapshot.setApprovalNumber(dto.getApprovalNumber());
        snapshot.setManufacturer(dto.getManufacturer());
        snapshot.setSpecification(dto.getSpecification());
        snapshot.setPriceCurrent(dto.getPriceCurrent());
        snapshot.setStockQuantity(dto.getStockQuantity());
        snapshot.setProductData(dto.getRawDataPayload() != null ? dto.getRawDataPayload() : "{}");
        snapshot.setCollectedAt(dto.getCollectedAt() != null ? dto.getCollectedAt() : new Date());
        return snapshot;
    }

    /**
     * 构建结果统计 Map。
     */
    private Map<String, Object> buildResult(int total, int groupCount, int newGroups, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("groupCount", groupCount);
        result.put("newGroups", newGroups);
        result.put("message", message);
        return result;
    }
}
