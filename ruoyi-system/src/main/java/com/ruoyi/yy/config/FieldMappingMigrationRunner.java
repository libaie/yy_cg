package com.ruoyi.yy.config;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.yy.domain.YyFieldMappingRule;
import com.ruoyi.yy.mapper.YyFieldMappingRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字段映射迁移器：将 yy_field_mapping 旧表数据迁移到 yy_field_mapping_rule 新表
 *
 * 触发方式：启动时添加 JVM 参数 -Dfield.mapping.migrate=true
 *
 * 转换语义：
 * - 分号 (;) 表示合并组："a;b" 解析为两组 [["a"],["b"]]，每组各自取值
 * - 逗号 (,) 表示回退组："a,b" 解析为一组 [["a","b"]]，依次尝试
 * - 混合："a,b;c" 解析为 [["a","b"],["c"]]，两组（第一组有回退）
 *
 * @author ruoyi
 */
@Component
public class FieldMappingMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FieldMappingMigrationRunner.class);

    private static final String MIGRATE_PROPERTY = "field.mapping.migrate";

    @Autowired
    private YyFieldMappingRuleMapper ruleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        String migrateFlag = System.getProperty(MIGRATE_PROPERTY, "false");
        if (!"true".equals(migrateFlag)) {
            log.info("字段映射迁移已跳过（设置 -D{}=true 以启用）", MIGRATE_PROPERTY);
            return;
        }

        log.info("========== 开始字段映射迁移 ==========");

        // 1. 读取旧表数据
        String selectSql = "SELECT platform_id, standard_field, platform_field, field_type, " +
                "is_required, sort_order FROM yy_field_mapping " +
                "WHERE standard_field IS NOT NULL AND platform_field IS NOT NULL AND status = 1 " +
                "ORDER BY platform_id, sort_order";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
        log.info("从 yy_field_mapping 读取到 {} 条启用记录", rows.size());

        if (rows.isEmpty()) {
            log.info("没有需要迁移的数据，迁移结束");
            return;
        }

        // 2. 转换为 YyFieldMappingRule 列表
        List<YyFieldMappingRule> rules = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            YyFieldMappingRule rule = convertRow(row);
            if (rule != null) {
                rules.add(rule);
            }
        }

        log.info("成功转换 {} 条规则", rules.size());

        // 3. 按 platformId 分组
        Map<Long, List<YyFieldMappingRule>> groupedByPlatform = new HashMap<>();
        for (YyFieldMappingRule rule : rules) {
            groupedByPlatform
                    .computeIfAbsent(rule.getPlatformId(), k -> new ArrayList<>())
                    .add(rule);
        }

        // 4. 删除旧规则表中的所有数据
        int deletedCount = jdbcTemplate.update("DELETE FROM yy_field_mapping_rule");
        log.info("已清空 yy_field_mapping_rule 表，删除 {} 条记录", deletedCount);

        // 5. 按平台分组批量插入
        int totalInserted = 0;
        for (Map.Entry<Long, List<YyFieldMappingRule>> entry : groupedByPlatform.entrySet()) {
            List<YyFieldMappingRule> platformRules = entry.getValue();
            ruleMapper.batchInsert(platformRules);
            totalInserted += platformRules.size();
            log.info("平台 {} 插入 {} 条规则", entry.getKey(), platformRules.size());
        }

        log.info("========== 字段映射迁移完成，共迁移 {} 条规则到 {} 个平台 ==========",
                totalInserted, groupedByPlatform.size());
    }

    /**
     * 将旧表行数据转换为 YyFieldMappingRule
     */
    private YyFieldMappingRule convertRow(Map<String, Object> row) {
        String platformField = (String) row.get("platform_field");
        if (platformField == null || platformField.trim().isEmpty()) {
            return null;
        }

        String fieldType = (String) row.get("field_type");
        String standardField = (String) row.get("standard_field");

        // 解析 sourcePaths
        List<List<String>> fieldGroups = parseFieldGroups(platformField);
        if (fieldGroups.isEmpty()) {
            log.warn("标准字段 '{}' 的 platform_field '{}' 解析为空，跳过", standardField, platformField);
            return null;
        }

        // 映射字段类型
        String[] typeAndConfig = mapFieldType(fieldType);

        YyFieldMappingRule rule = new YyFieldMappingRule();
        rule.setPlatformId(toLong(row.get("platform_id")));
        rule.setStandardField(standardField);
        rule.setSourcePaths(JSON.toJSONString(fieldGroups));
        rule.setTransformType(typeAndConfig[0]);
        rule.setTransformConfig(typeAndConfig[1]);
        rule.setIsRequired(toInteger(row.get("is_required")));
        rule.setSortOrder(toInteger(row.get("sort_order")));
        rule.setIsEnabled(1);
        return rule;
    }

    /**
     * 将 platform_field 字符串解析为字段组。
     *
     * 分号 (;) = 组间分隔符（合并组）：
     *   "a;b" → [["a"],["b"]]  — 两组，每组取一个路径
     *
     * 逗号 (,) = 组内分隔符（回退组）：
     *   "a,b" → [["a","b"]]  — 一组，先试 a，失败则 b
     *
     * 混合：
     *   "a,b;c" → [["a","b"],["c"]]  — 两组，第一组有回退
     *
     * @param platformField 原始平台字段字符串
     * @return 解析后的字段组列表（JSON 二维数组结构）
     */
    static List<List<String>> parseFieldGroups(String platformField) {
        List<List<String>> groups = new ArrayList<>();

        String[] mergeGroups = platformField.split(";");
        for (String mergeGroup : mergeGroups) {
            List<String> fallbackPaths = Arrays.stream(mergeGroup.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!fallbackPaths.isEmpty()) {
                groups.add(fallbackPaths);
            }
        }

        return groups;
    }

    /**
     * 将旧表的 field_type 映射为 (transformType, transformConfig)
     *
     * @param fieldType 旧字段类型（decimal/price/integer/count/date/datetime/boolean/bool）
     * @return [transformType, transformConfig]
     */
    static String[] mapFieldType(String fieldType) {
        if (fieldType == null) {
            return new String[]{"none", null};
        }
        switch (fieldType.toLowerCase().trim()) {
            case "decimal":
            case "price":
                return new String[]{"number", "{\"scale\":2,\"strip\":\"¥$€£元角分厘/盒瓶袋支片粒条,，\"}"};
            case "integer":
            case "count":
                return new String[]{"number", "{\"scale\":0}"};
            case "date":
            case "datetime":
                return new String[]{"date", null};
            case "boolean":
            case "bool":
                return new String[]{"boolean", null};
            default:
                return new String[]{"none", null};
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
