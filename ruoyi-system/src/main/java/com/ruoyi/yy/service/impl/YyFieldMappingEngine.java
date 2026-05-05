package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyFieldMappingRule;
import com.ruoyi.yy.dto.MappedProductDTO;
import com.ruoyi.yy.mapper.YyFieldMappingRuleMapper;
import com.ruoyi.yy.model.MappingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段映射规则引擎 V2
 * 管道式处理：路径提取 → 备选尝试 → 类型清洗 → 值映射 → 校验 → 默认值
 */
@Component
public class YyFieldMappingEngine {

    private static final Logger log = LoggerFactory.getLogger(YyFieldMappingEngine.class);
    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
    };

    @Autowired(required = false)
    private YyFieldMappingRuleMapper ruleMapper;

    /** 缓存条目，带加载时间戳用于 TTL 过期 */
    private static class CacheEntry {
        final List<YyFieldMappingRule> rules;
        final long loadedAt;
        CacheEntry(List<YyFieldMappingRule> rules) { this.rules = rules; this.loadedAt = System.currentTimeMillis(); }
        boolean isStale() { return System.currentTimeMillis() - loadedAt > 600_000; }
    }

    /** 缓存规则，按 platformId → apiCode → CacheEntry，10 分钟 TTL */
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();

    /** 加载平台+API的规则（10 分钟缓存，过期自动重载） */
    public List<YyFieldMappingRule> loadRules(Long platformId, String apiCode) {
        if (ruleMapper == null) return Collections.emptyList();
        String key = platformId + "|" + (apiCode != null ? apiCode : "*");
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isStale()) return entry.rules;

        List<YyFieldMappingRule> rules = new ArrayList<>(ruleMapper.selectByPlatformAndApi(platformId, apiCode));
        // 合并 API 专属规则 + 平台默认规则（api_code=NULL）
        List<YyFieldMappingRule> defaults = ruleMapper.selectByPlatformAndApi(platformId, null);
        Set<String> seen = new HashSet<>();
        for (YyFieldMappingRule r : rules) seen.add(r.getStandardField());
        for (YyFieldMappingRule d : defaults) {
            if (!seen.contains(d.getStandardField())) rules.add(d);
        }
        rules.sort(Comparator.comparing(YyFieldMappingRule::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        cache.put(key, new CacheEntry(rules));
        return rules;
    }

    /** 清除缓存 */
    public void clearCache() { cache.clear(); }

    /**
     * 对原始数据执行所有规则，返回 MappingResult
     * @param rawJson 原始平台 JSON（已解密的字符串或对象）
     * @param platformId 平台ID
     * @param apiCode API编码
     */
    @SuppressWarnings("unchecked")
    public MappingResult execute(Object rawJson, Long platformId, String apiCode) {
        Map<String, Object> data;
        if (rawJson instanceof String) data = JSON.parseObject((String) rawJson);
        else if (rawJson instanceof Map) data = (Map<String, Object>) rawJson;
        else data = new LinkedHashMap<>();

        List<YyFieldMappingRule> rules = loadRules(platformId, apiCode);
        return executeWithRules(data, rules);
    }

    /**
     * 批量执行：加载规则一次，对列表中每个数据项逐一执行
     * @param items 数据项列表
     * @param platformId 平台ID
     * @param apiCode API编码
     */
    @SuppressWarnings("unchecked")
    public List<MappingResult> executeBatch(List<?> items, Long platformId, String apiCode) {
        List<YyFieldMappingRule> rules = loadRules(platformId, apiCode);
        List<MappingResult> results = new ArrayList<>();
        for (Object item : items) {
            Map<String, Object> data;
            if (item instanceof String) data = JSON.parseObject((String) item);
            else if (item instanceof Map) data = (Map<String, Object>) item;
            else data = new LinkedHashMap<>();
            results.add(executeWithRules(data, rules));
        }
        return results;
    }

    /**
     * 将 MappingResult 转换为 MappedProductDTO
     * @param result 字段映射结果
     * @param platformCode 平台编码
     */
    public MappedProductDTO convertToProduct(MappingResult result, String platformCode) {
        MappedProductDTO dto = new MappedProductDTO();
        Map<String, Object> fields = result.getFields();

        dto.setSourcePlatform(platformCode);

        // 核心标识
        dto.setProductId(getString(fields, "productId"));
        dto.setSkuId(getString(fields, "skuId"));
        dto.setSourceApi(getString(fields, "sourceApi"));
        dto.setRawDataPayload(getString(fields, "rawDataPayload"));

        // 基础信息
        dto.setBarcode(getString(fields, "barcode"));
        dto.setProductName(getString(fields, "productName"));
        dto.setCommonName(getString(fields, "commonName"));
        dto.setBrandName(getString(fields, "brandName"));
        dto.setManufacturer(getString(fields, "manufacturer"));
        dto.setApprovalNumber(getString(fields, "approvalNumber"));

        // 分类
        dto.setCategoryId(getString(fields, "categoryId"));
        dto.setCategoryName(getString(fields, "categoryName"));

        // 规格
        dto.setSpecification(getString(fields, "specification"));
        dto.setUnit(getString(fields, "unit"));
        dto.setPackingRatio(getString(fields, "packingRatio"));

        // 状态与库存
        dto.setProductStatus(getString(fields, "productStatus"));
        dto.setStockQuantity(getInt(fields, "stockQuantity"));
        dto.setWarehouseStock(wrapJsonArray(fields.get("warehouseStock")));

        // 图片
        dto.setMainImages(wrapJsonArray(fields.get("mainImages")));

        // 限购
        dto.setMinOrderQty(getInt(fields, "minOrderQty"));
        dto.setMaxOrderQty(getInt(fields, "maxOrderQty"));

        // 日期
        dto.setProductionDate(getDate(fields, "productionDate"));
        dto.setExpirationDate(getDate(fields, "expirationDate"));
        dto.setShelfLife(getString(fields, "shelfLife"));

        // 医药专属
        dto.setIsPrescriptionDrug(getInt(fields, "isPrescriptionDrug"));
        dto.setMedicareType(getString(fields, "medicareType"));
        dto.setTraceabilityCodeStatus(getInt(fields, "traceabilityCodeStatus"));

        // 销售
        dto.setSalesVolume(getInt(fields, "salesVolume"));
        dto.setShopName(getString(fields, "shopName"));

        // 价格
        dto.setPriceRetail(getBigDecimal(fields, "priceRetail"));
        dto.setPriceCurrent(getBigDecimal(fields, "priceCurrent"));
        dto.setPriceStepRules(wrapJsonArray(fields.get("priceStepRules")));
        dto.setPriceAssemble(getBigDecimal(fields, "priceAssemble"));

        // 物流与税务
        dto.setIsTaxIncluded(getInt(fields, "isTaxIncluded"));
        dto.setFreightAmount(getBigDecimal(fields, "freightAmount"));
        dto.setFreeShippingThreshold(getBigDecimal(fields, "freeShippingThreshold"));

        // 标签与活动
        dto.setTags(getStringList(fields, "tags"));
        dto.setMarketingTags(getStringList(fields, "marketingTags"));
        dto.setActivityDetails(wrapJsonArray(fields.get("activityDetails")));
        dto.setPurchaseLimits(wrapJsonArray(fields.get("purchaseLimits")));

        // 融合相关
        dto.setFusionGroupId(getLong(fields, "fusionGroupId"));
        dto.setFusionKey(getString(fields, "fusionKey"));

        // 时间
        dto.setCollectedAt(getDate(fields, "collectedAt"));
        dto.setSyncedAt(getDate(fields, "syncedAt"));

        return dto;
    }

    // ---- pipeline core ----

    /**
     * 对已解析的 data 执行规则管道，返回 MappingResult（含失败和校验错误）
     */
    @SuppressWarnings("unchecked")
    private MappingResult executeWithRules(Map<String, Object> data, List<YyFieldMappingRule> rules) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> requiredFieldFailures = new ArrayList<>();
        Map<String, String> validationErrors = new LinkedHashMap<>();

        for (YyFieldMappingRule rule : rules) {
            if (rule.getIsEnabled() != null && rule.getIsEnabled() == 0) continue;

            // Step 1-2: 路径提取 + 备选尝试
            Object rawValue = extractFromPaths(data, rule.getSourcePaths());
            if (rawValue == null && rule.getIsRequired() == null) continue;

            // Step 3: 类型清洗
            Object cleaned = applyTransform(rawValue, rule.getTransformType(), rule.getTransformConfig());

            // Step 4: 值映射
            if (rule.getValueMap() != null && cleaned instanceof String) {
                Map<String, Object> vMap = JSON.parseObject(rule.getValueMap());
                if (vMap.containsKey(cleaned)) cleaned = vMap.get(cleaned);
            }

            // Step 5: 校验
            if (!validate(cleaned, rule.getValidation())) {
                validationErrors.put(rule.getStandardField(), "Validation failed: " + rule.getValidation());
                if (rule.getIsRequired() != null && rule.getIsRequired() == 1) {
                    log.warn("[RuleEngine] 必填字段 {} 校验失败，使用默认值", rule.getStandardField());
                }
                if (rule.getDefaultValue() != null) cleaned = castDefault(rule.getDefaultValue(), rule.getTransformType());
                else if (rule.getIsRequired() != null && rule.getIsRequired() == 1) continue; // 跳过
            }

            // Step 6: 默认值（当值为 null 时）
            if (cleaned == null && rule.getDefaultValue() != null) {
                cleaned = castDefault(rule.getDefaultValue(), rule.getTransformType());
            }

            if (cleaned != null) {
                result.put(rule.getStandardField(), cleaned);
            } else if (rule.getIsRequired() != null && rule.getIsRequired() == 1) {
                requiredFieldFailures.add(rule.getStandardField());
                log.debug("[RuleEngine] 必填字段 {} 无值", rule.getStandardField());
            }
        }

        return new MappingResult(result, requiredFieldFailures, validationErrors);
    }

    /**
     * 对单个原始 JSON 执行规则（内部用于 executeBatch，也可独立使用）
     */
    @SuppressWarnings("unchecked")
    private MappingResult executeSingle(Object rawJson, Long platformId, String apiCode) {
        Map<String, Object> data;
        if (rawJson instanceof String) data = JSON.parseObject((String) rawJson);
        else if (rawJson instanceof Map) data = (Map<String, Object>) rawJson;
        else data = new LinkedHashMap<>();

        List<YyFieldMappingRule> rules = loadRules(platformId, apiCode);
        return executeWithRules(data, rules);
    }

    // ---- private helpers ----

    /** 从 JSON 嵌套路径中提取值，支持备选路径列表 */
    private Object extractFromPaths(Map<String, Object> data, String sourcePathsJson) {
        if (sourcePathsJson == null || sourcePathsJson.isBlank()) return null;
        List<String> paths = JSON.parseArray(sourcePathsJson, String.class);
        if (paths == null) return null;

        for (String path : paths) {
            Object val = extractByPath(data, path.trim());
            if (val != null && !val.toString().isEmpty()) return val;
        }
        return null;
    }

    /** 按点号分隔路径提取值，支持数组索引 */
    @SuppressWarnings("unchecked")
    private Object extractByPath(Map<String, Object> data, String path) {
        if (data == null || path == null) return null;
        String[] segments = path.split("\\.");
        Object current = data;
        for (String seg : segments) {
            if (current == null) return null;
            // 处理数组索引 data.products[0]
            Matcher m = Pattern.compile("(\\w+)\\[(\\d+)\\]").matcher(seg);
            if (m.matches()) {
                Map<String, Object> map = (Map<String, Object>) current;
                Object arr = map.get(m.group(1));
                if (arr instanceof List) {
                    int idx = Integer.parseInt(m.group(2));
                    current = ((List<?>) arr).size() > idx ? ((List<?>) arr).get(idx) : null;
                } else { current = null; }
            } else {
                if (current instanceof Map) current = ((Map<String, Object>) current).get(seg);
                else return null;
            }
        }
        return current;
    }

    /** 转换清洗 */
    private Object applyTransform(Object raw, String type, String configJson) {
        if (raw == null) return null;
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;

        if (type == null || "none".equals(type)) return s;

        return switch (type) {
            case "number"  -> transformNumber(s, configJson);
            case "date"    -> transformDate(s);
            case "strip"   -> transformStrip(s, configJson);
            case "boolean" -> transformBoolean(s);
            default -> s;
        };
    }

    private Object transformNumber(String s, String configJson) {
        if (configJson != null) {
            JSONObject cfg = JSON.parseObject(configJson);
            String strip = cfg.getString("strip");
            if (strip != null) for (char c : strip.toCharArray()) s = s.replace(String.valueOf(c), "");
        }
        // 剥离非数字字符（保留 . 和 -）
        s = s.replaceAll("[^\\d.\\-]", "");
        if (s.isEmpty()) return null;
        try {
            BigDecimal num = new BigDecimal(s);
            if (configJson != null) {
                JSONObject cfg = JSON.parseObject(configJson);
                int scale = cfg.getIntValue("scale", -1);
                if (scale >= 0) num = num.setScale(scale, RoundingMode.HALF_UP);
            }
            return num;
        } catch (Exception e) { return null; }
    }

    private Object transformDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s, fmt).toString(); } catch (Exception ignored) {}
        }
        return s; // 无法解析时返回原值
    }

    private Object transformStrip(String s, String configJson) {
        if (configJson == null) return s;
        JSONObject cfg = JSON.parseObject(configJson);
        String prefix = cfg.getString("prefix");
        String suffix = cfg.getString("suffix");
        if (prefix != null && s.startsWith(prefix)) s = s.substring(prefix.length());
        if (suffix != null && s.endsWith(suffix)) s = s.substring(0, s.length() - suffix.length());
        return s;
    }

    /** 简单校验 */
    private boolean validate(Object val, String validationJson) {
        if (validationJson == null || val == null) return true;
        JSONObject v = JSON.parseObject(validationJson);

        if (val instanceof BigDecimal num) {
            if (v.containsKey("min") && num.compareTo(v.getBigDecimal("min")) < 0) return false;
            if (v.containsKey("max") && num.compareTo(v.getBigDecimal("max")) > 0) return false;
        }

        if (v.containsKey("pattern")) {
            Pattern p = Pattern.compile(v.getString("pattern"));
            if (!p.matcher(val.toString()).matches()) return false;
        }

        if (v.containsKey("enum")) {
            List<String> enums = JSON.parseArray(v.getString("enum"), String.class);
            if (enums != null && !enums.contains(val.toString())) return false;
        }

        return true;
    }

    private Object castDefault(String def, String transformType) {
        if ("number".equals(transformType)) {
            try { return new BigDecimal(def); } catch (Exception e) { return def; }
        }
        return def;
    }

    /** 布尔转换：是/true/1/yes → 1，其余 → 0 */
    private Object transformBoolean(String s) {
        String lower = s.toLowerCase().trim();
        if ("true".equals(lower) || "1".equals(lower) || "是".equals(lower) || "yes".equals(lower)) {
            return 1;
        }
        return 0;
    }

    // ---- convertToProduct helpers ----

    /** 需要包装为 JSON 数组的字段 */
    private static final Set<String> JSON_ARRAY_FIELDS = Set.of(
        "warehouseStock", "mainImages", "priceStepRules", "activityDetails", "purchaseLimits"
    );

    /** 如果值是 JSONObject 则包一层 JSONArray 再 toString */
    private String wrapJsonArray(Object val) {
        if (val == null) return null;
        if (val instanceof JSONArray) return val.toString();
        if (val instanceof JSONObject) return new JSONArray().fluentAdd(val).toString();
        if (val instanceof List) return JSONArray.of(val).toString();
        return new JSONArray().fluentAdd(val).toString();
    }

    private String getString(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private Long getLong(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal getBigDecimal(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private Date getDate(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return null;
        if (val instanceof Date) return (Date) val;
        if (val instanceof LocalDate) return java.sql.Date.valueOf((LocalDate) val);
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return null;
        if (val instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) val) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return null;
    }
}
