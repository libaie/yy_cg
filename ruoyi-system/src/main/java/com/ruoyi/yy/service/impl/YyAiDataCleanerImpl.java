package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyCleanResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI数据清洗 — 批量标准化厂家名、规格、通用名
 */
@Service
public class YyAiDataCleanerImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiDataCleanerImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药数据清洗专家。请分析以下商品数据，识别需要标准化的字段。\n" +
        "请返回JSON数组，每个元素包含：\n" +
        "{\"snapshotId\": id, \"field\": \"字段名\", \"original\": \"原值\", " +
        "\"suggested\": \"建议值\", \"confidence\": 0.9, \"reason\": \"原因\"}";

    @Autowired
    private IYyAiGateway aiGateway;

    @Autowired
    private YyProductSnapshotMapper snapshotMapper;

    /**
     * 批量清洗商品数据
     */
    public YyCleanResult cleanProductData(List<YyProductSnapshot> snapshots) {
        YyCleanResult result = new YyCleanResult();
        result.setTotalProcessed(snapshots.size());

        if (snapshots.isEmpty()) {
            return result;
        }

        List<List<YyProductSnapshot>> batches = partition(snapshots, 20);
        List<Map<String, Object>> allSuggestions = new ArrayList<>();

        for (List<YyProductSnapshot> batch : batches) {
            String userPrompt = buildPrompt(batch);

            YyAiRequest request = new YyAiRequest();
            request.setScene("cleaner");
            request.setSystemPrompt(SYSTEM_PROMPT);
            request.setUserPrompt(userPrompt);
            request.setModel("qwen-turbo");
            request.setTemperature(0.1);
            request.setMaxTokens(2000);

            YyAiResponse response = aiGateway.call(request);

            if (response.isSuccess()) {
                List<Map<String, Object>> batchSuggestions = parseSuggestions(response.getContent());
                allSuggestions.addAll(batchSuggestions);
            } else {
                log.warn("AI cleaner batch failed: {}", response.getErrorMessage());
            }
        }

        result.setSuggestions(allSuggestions);
        result.setNeedsReview(allSuggestions.size());
        return result;
    }

    private String buildPrompt(List<YyProductSnapshot> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("待清洗的商品数据：\n\n");

        for (YyProductSnapshot s : batch) {
            sb.append("ID: ").append(s.getId()).append("\n");
            sb.append("  通用名: ").append(nullSafe(s.getCommonName())).append("\n");
            sb.append("  厂家: ").append(nullSafe(s.getManufacturer())).append("\n");
            sb.append("  规格: ").append(nullSafe(s.getSpecification())).append("\n");
            sb.append("  批准文号: ").append(nullSafe(s.getApprovalNumber())).append("\n");
            sb.append("  69码: ").append(nullSafe(s.getBarcode())).append("\n\n");
        }

        sb.append("请识别需要标准化的字段，如厂家名不一致、规格格式混乱等。");
        return sb.toString();
    }

    private List<Map<String, Object>> parseSuggestions(String content) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        try {
            JsonNode root = JSON.readTree(content);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("snapshotId", item.path("snapshotId").asLong());
                    suggestion.put("field", item.path("field").asText());
                    suggestion.put("original", item.path("original").asText());
                    suggestion.put("suggested", item.path("suggested").asText());
                    suggestion.put("confidence", item.path("confidence").asDouble());
                    suggestion.put("reason", item.path("reason").asText());
                    suggestions.add(suggestion);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI cleaner response", e);
        }
        return suggestions;
    }

    private List<List<YyProductSnapshot>> partition(List<YyProductSnapshot> list, int size) {
        List<List<YyProductSnapshot>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String nullSafe(String value) {
        return value != null ? value : "未知";
    }
}
