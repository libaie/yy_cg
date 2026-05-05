package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyFieldMappingRule;
import com.ruoyi.yy.mapper.YyFieldMappingRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    /** 缓存规则，按 platformId → apiCode → rules */
    private final Map<String, List<YyFieldMappingRule>> cache = new LinkedHashMap<>();

    /** 加载平台+API的规则 */
    public List<YyFieldMappingRule> loadRules(Long platformId, String apiCode) {
        if (ruleMapper == null) return Collections.emptyList();
        String key = platformId + "|" + (apiCode != null ? apiCode : "*");
        return cache.computeIfAbsent(key, k -> {
            List<YyFieldMappingRule> rules = ruleMapper.selectByPlatformAndApi(platformId, apiCode);
            // 合并 API 专属规则 + 平台默认规则（api_code=NULL）
            List<YyFieldMappingRule> defaults = ruleMapper.selectByPlatformAndApi(platformId, null);
            Set<String> seen = new HashSet<>();
            for (YyFieldMappingRule r : rules) seen.add(r.getStandardField());
            for (YyFieldMappingRule d : defaults) {
                if (!seen.contains(d.getStandardField())) rules.add(d);
            }
            rules.sort(Comparator.comparing(YyFieldMappingRule::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
            return rules;
        });
    }

    /** 清除缓存 */
    public void clearCache() { cache.clear(); }

    /**
     * 对原始数据执行所有规则，返回标准化结果 Map
     * @param rawJson 原始平台 JSON（已解密的字符串或对象）
     * @param platformId 平台ID
     * @param apiCode API编码
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Object rawJson, Long platformId, String apiCode) {
        Map<String, Object> data;
        if (rawJson instanceof String) data = JSON.parseObject((String) rawJson);
        else if (rawJson instanceof Map) data = (Map<String, Object>) rawJson;
        else data = new LinkedHashMap<>();

        List<YyFieldMappingRule> rules = loadRules(platformId, apiCode);
        Map<String, Object> result = new LinkedHashMap<>();

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

            if (cleaned != null) result.put(rule.getStandardField(), cleaned);
            else if (rule.getIsRequired() != null && rule.getIsRequired() == 1) {
                log.debug("[RuleEngine] 必填字段 {} 无值", rule.getStandardField());
            }
        }

        return result;
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
            case "number" -> transformNumber(s, configJson);
            case "date"   -> transformDate(s);
            case "strip"  -> transformStrip(s, configJson);
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
}
