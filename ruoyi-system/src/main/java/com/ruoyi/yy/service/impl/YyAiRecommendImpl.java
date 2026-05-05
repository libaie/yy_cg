package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiRecommend;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class YyAiRecommendImpl implements IYyAiRecommend {

    private static final Logger log = LoggerFactory.getLogger(YyAiRecommendImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购推荐专家。根据用户采购历史和当前药品，推荐关联药品。\n" +
        "返回JSON: {\"recommendations\": [{\"drugName\": \"...\", \"reason\": \"...\", \"estimatedPrice\": ...}], \"basedOn\": \"...\"}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> recommend(String category, int limit) {
        String userPrompt = "药品类别: " + category + "\n推荐数量: " + limit;

        YyAiRequest request = new YyAiRequest();
        request.setScene("recommend");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("deepseek-chat");
        request.setTemperature(0.5);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Recommend failed: {}", response.getErrorMessage());
            return fallbackResult();
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", parseRecommendations(root));
            result.put("basedOn", root.path("basedOn").asText("基于采购历史"));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse recommend response", e);
            return fallbackResult();
        }
    }

    private List<Map<String, Object>> parseRecommendations(JsonNode root) {
        List<Map<String, Object>> list = new ArrayList<>();
        JsonNode node = root.path("recommendations");
        if (node.isArray()) {
            for (JsonNode item : node) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("drugName", item.path("drugName").asText(""));
                rec.put("reason", item.path("reason").asText(""));
                rec.put("estimatedPrice", item.path("estimatedPrice").asDouble(0));
                list.add(rec);
            }
        }
        return list;
    }

    private Map<String, Object> fallbackResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("recommendations", Collections.emptyList());
        result.put("basedOn", "暂时无法生成推荐，请稍后再试。");
        return result;
    }
}
