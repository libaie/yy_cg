package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class YyAiInsightImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiInsightImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购数据分析专家。分析以下多平台价格数据，解读价格差异原因。\n" +
        "返回JSON: {\"summary\": \"...\", \"insights\": [\"...\"], \"recommendation\": \"...\"}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> analyze(String drugName, String pricesJson) {
        String userPrompt = "药品: " + drugName + "\n多平台价格数据:\n" + pricesJson;

        YyAiRequest request = new YyAiRequest();
        request.setScene("insight");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Insight analysis failed: {}", response.getErrorMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("summary", "暂时无法分析价格数据，请稍后再试。");
            result.put("insights", Collections.emptyList());
            result.put("recommendation", "");
            return result;
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("summary", root.path("summary").asText("暂无分析"));
            result.put("insights", YyAiResponseParser.parseArray(root, "insights"));
            result.put("recommendation", root.path("recommendation").asText(""));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse insight response", e);
            Map<String, Object> result = new HashMap<>();
            result.put("summary", "暂时无法分析价格数据，请稍后再试。");
            result.put("insights", Collections.emptyList());
            result.put("recommendation", "");
            return result;
        }
    }
}
