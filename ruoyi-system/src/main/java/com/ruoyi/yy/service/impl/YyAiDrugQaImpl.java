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
public class YyAiDrugQaImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiDrugQaImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药知识专家。根据用户问题提供准确的药品信息。\n" +
        "注意：\n" +
        "1. 必须基于可靠来源回答\n" +
        "2. 涉到处方药时提醒用户咨询医生\n" +
        "3. 不提供诊断建议\n" +
        "4. 返回JSON: {\"answer\": \"...\", \"sources\": [\"...\"], \"warnings\": [\"...\"]}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> ask(String question, String drugName) {
        String userPrompt = "药品: " + drugName + "\n问题: " + question;

        YyAiRequest request = new YyAiRequest();
        request.setScene("drug_qa");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Drug QA failed: {}", response.getErrorMessage());
            return YyAiResponseParser.fallbackMap("暂时无法回答该问题，请稍后再试或咨询专业药师。");
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("answer", root.path("answer").asText("暂无答案"));
            result.put("sources", YyAiResponseParser.parseArray(root, "sources"));
            result.put("warnings", YyAiResponseParser.parseArray(root, "warnings"));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse drug QA response", e);
            return YyAiResponseParser.fallbackMap("暂时无法回答该问题，请稍后再试或咨询专业药师。");
        }
    }
}
