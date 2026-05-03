package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPurchaseAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI比价顾问 — 综合多平台价格给出采购建议
 */
@Service
public class YyAiAdvisorImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiAdvisorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购比价专家。请根据以下多平台价格数据，给出最优采购建议。\n" +
        "请返回JSON格式：\n" +
        "{\"summary\": \"自然语言建议\", \"bestPlatform\": \"平台编码\", \"bestPrice\": 价格, " +
        "\"totalSaving\": 节省金额, \"tips\": [\"建议1\", \"建议2\"]}";

    @Autowired
    private IYyAiGateway aiGateway;

    /**
     * 获取采购建议
     */
    public YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices) {
        if (prices == null || prices.isEmpty()) {
            YyPurchaseAdvice empty = new YyPurchaseAdvice();
            empty.setSummary("暂无该药品的比价数据");
            empty.setTips(new ArrayList<>());
            return empty;
        }

        String userPrompt = buildPrompt(drugName, prices);

        YyAiRequest request = new YyAiRequest();
        request.setScene("advisor");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("AI advisor failed: {}", response.getErrorMessage());
            return buildFallbackAdvice(prices);
        }

        return parseResponse(response.getContent(), prices);
    }

    private String buildPrompt(String drugName, List<YyPriceComparison> prices) {
        StringBuilder sb = new StringBuilder();
        sb.append("药品: ").append(drugName).append("\n\n");
        sb.append("各平台价格:\n");

        for (YyPriceComparison price : prices) {
            sb.append("- 平台: ").append(price.getSourcePlatform())
              .append(" 供货价: ").append(price.getCurrentPrice())
              .append(" 运费: ").append(price.getFreightAmount() != null ? price.getFreightAmount() : "0")
              .append(" 库存: ").append(price.getStockQuantity() != null ? price.getStockQuantity() : "未知")
              .append("\n");
        }

        sb.append("\n请分析哪个平台最划算，考虑价格、运费、库存因素，并给出凑单建议。");
        return sb.toString();
    }

    private YyPurchaseAdvice parseResponse(String content, List<YyPriceComparison> prices) {
        try {
            JsonNode root = JSON.readTree(content);
            YyPurchaseAdvice advice = new YyPurchaseAdvice();
            advice.setSummary(root.path("summary").asText("暂无建议"));
            advice.setBestPlatform(root.path("bestPlatform").asText());
            advice.setBestPrice(new BigDecimal(root.path("bestPrice").asText("0")));
            advice.setTotalSaving(new BigDecimal(root.path("totalSaving").asText("0")));

            List<String> tips = new ArrayList<>();
            JsonNode tipsNode = root.path("tips");
            if (tipsNode.isArray()) {
                for (JsonNode tip : tipsNode) {
                    tips.add(tip.asText());
                }
            }
            advice.setTips(tips);
            return advice;
        } catch (Exception e) {
            log.error("Failed to parse AI advisor response", e);
            return buildFallbackAdvice(prices);
        }
    }

    /**
     * 降级建议：不调用LLM，直接取最低价
     */
    private YyPurchaseAdvice buildFallbackAdvice(List<YyPriceComparison> prices) {
        YyPriceComparison cheapest = prices.stream()
            .min((a, b) -> {
                BigDecimal priceA = a.getCurrentPrice() != null ? a.getCurrentPrice() : BigDecimal.valueOf(99999);
                BigDecimal priceB = b.getCurrentPrice() != null ? b.getCurrentPrice() : BigDecimal.valueOf(99999);
                return priceA.compareTo(priceB);
            })
            .orElse(null);

        YyPurchaseAdvice advice = new YyPurchaseAdvice();
        if (cheapest != null) {
            advice.setSummary("推荐在 " + cheapest.getSourcePlatform() + " 采购，价格最低");
            advice.setBestPlatform(cheapest.getSourcePlatform());
            advice.setBestPrice(cheapest.getCurrentPrice());
        } else {
            advice.setSummary("暂无价格数据");
        }
        advice.setTips(new ArrayList<>());
        return advice;
    }
}
