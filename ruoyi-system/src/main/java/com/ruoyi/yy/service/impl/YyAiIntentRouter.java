package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class YyAiIntentRouter {

    private static final Logger log = LoggerFactory.getLogger(YyAiIntentRouter.class);
    private static final Set<String> VALID_INTENTS = Set.of(
        "ADVISOR", "INSIGHT", "DRUG_QA", "RECOMMEND", "GENERAL"
    );

    private static final String SYSTEM_PROMPT =
        "你是意图分类器。根据用户消息，返回以下意图之一：\n" +
        "- ADVISOR: 采购建议、比价、哪个平台便宜、推荐采购\n" +
        "- INSIGHT: 价格分析、为什么价格不同、价格趋势\n" +
        "- DRUG_QA: 药品信息、用法用量、适应症、副作用、相互作用\n" +
        "- RECOMMEND: 推荐药品、同类药品、关联商品\n" +
        "- GENERAL: 其他所有问题\n\n" +
        "只返回意图代码，不要其他内容。";

    @Autowired
    private IYyAiGateway aiGateway;

    public String route(String userMessage) {
        YyAiRequest request = new YyAiRequest();
        request.setScene("intent_router");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userMessage);
        request.setModel("qwen-turbo");
        request.setTemperature(0.0);
        request.setMaxTokens(50);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Intent routing failed: {}", response.getErrorMessage());
            return "GENERAL";
        }

        String intent = response.getContent().trim().toUpperCase();
        if (!VALID_INTENTS.contains(intent)) {
            log.warn("Invalid intent from LLM: {}", intent);
            return "GENERAL";
        }

        return intent;
    }
}
