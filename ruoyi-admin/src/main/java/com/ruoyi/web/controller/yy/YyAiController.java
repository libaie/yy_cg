package com.ruoyi.web.controller.yy;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.domain.YyAiQuotaConfig;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPurchaseAdvice;
import com.ruoyi.yy.mapper.YyAiQuotaConfigMapper;
import com.ruoyi.yy.service.*;
import com.ruoyi.yy.service.impl.YyAiUsageService;
import com.ruoyi.yy.service.impl.YyAiIntentRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/yy/ai")
public class YyAiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(YyAiController.class);

    @Autowired private YyAiUsageService usageService;
    @Autowired private YyAiIntentRouter intentRouter;
    @Autowired private IYyAiGateway aiGateway;
    @Autowired private IYyUserTierService userTierService;
    @Autowired private IYyAiAdvisor advisorService;
    @Autowired private IYyAiDrugQa drugQa;
    @Autowired private IYyAiInsight insightService;
    @Autowired private IYyAiRecommend recommendService;
    @Autowired @Qualifier("sseExecutor") private ExecutorService sseExecutor;

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);

        if (!usageService.checkQuota(userId, "chat", tierLevel)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .data(JSON.toJSONString(Map.of("type", "error", "data", "今日对话次数已用完，升级会员解锁更多"))));
            } catch (IOException ignored) {}
            try { emitter.complete(); } catch (Exception ignored) {}
            return emitter;
        }

        String message = (String) body.getOrDefault("message", "");
        String model = (String) body.getOrDefault("model", "deepseek-chat");

        SseEmitter emitter = new SseEmitter(60_000L);
        sseExecutor.submit(() -> {
            IYyAiGateway.StreamResult streamResult = null;
            try {
                emitter.send(SseEmitter.event()
                    .data(JSON.toJSONString(Map.of("type", "status", "data", "analyzing_intent"))));

                String intent = intentRouter.route(message);
                String systemPrompt = buildSystemPrompt(intent);
                int maxTokens = getMaxTokens(tierLevel);

                YyAiRequest request = new YyAiRequest();
                request.setScene("chat_" + intent.toLowerCase());
                request.setSystemPrompt(systemPrompt);
                request.setUserPrompt(message);
                request.setModel(model);
                request.setTemperature(0.7);
                request.setMaxTokens(maxTokens);

                streamResult = aiGateway.callStreamResult(request);
                Iterator<String> tokens = streamResult.getTokens();

                final IYyAiGateway.StreamResult sr = streamResult;
                emitter.onCompletion(sr::close);
                emitter.onTimeout(sr::close);
                emitter.onError(e -> sr.close());

                int tokenCount = 0;
                while (tokens.hasNext()) {
                    String token = tokens.next();
                    emitter.send(SseEmitter.event()
                        .data(JSON.toJSONString(Map.of("type", "token", "data", token, "intent", intent))));
                    tokenCount++;
                }

                emitter.send(SseEmitter.event()
                    .data(JSON.toJSONString(Map.of("type", "done", "data", ""))));
                usageService.recordUsage(userId, "chat", null, tokenCount);

            } catch (IOException e) {
                log.debug("SSE client disconnected: {}", e.getMessage());
            } catch (Exception e) {
                log.error("SSE chat error", e);
                try {
                    emitter.send(SseEmitter.event()
                        .data(JSON.toJSONString(Map.of("type", "error", "data", "AI服务暂时不可用，请稍后重试"))));
                } catch (IOException ignored) {}
            } finally {
                if (streamResult != null) streamResult.close();
                try { emitter.complete(); } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/advisor")
    public AjaxResult advisor(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        if (!usageService.checkQuota(userId, "tool", tierLevel))
            return AjaxResult.error(429, "今日快捷功能次数已用完");
        String drugName = (String) body.getOrDefault("drugName", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.getOrDefault("prices", Collections.emptyList());
        List<YyPriceComparison> prices = raw.stream().map(m -> {
            YyPriceComparison p = new YyPriceComparison();
            p.setSourcePlatform((String) m.get("sourcePlatform"));
            if (m.get("currentPrice") instanceof Number n) p.setCurrentPrice(BigDecimal.valueOf(n.doubleValue()));
            if (m.get("freightAmount") instanceof Number n) p.setFreightAmount(BigDecimal.valueOf(n.doubleValue()));
            if (m.get("stockQuantity") instanceof Number n) p.setStockQuantity(n.intValue());
            return p;
        }).collect(Collectors.toList());
        String model = (String) body.getOrDefault("model", null);
        YyPurchaseAdvice advice = model != null ? advisorService.getAdvice(drugName, prices, model) : advisorService.getAdvice(drugName, prices);
        usageService.recordUsage(userId, "tool", "advisor", 0);
        return success(advice);
    }

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/insight")
    public AjaxResult insightApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        if (!usageService.checkQuota(userId, "tool", getUserTierLevel(userId)))
            return AjaxResult.error(429, "今日快捷功能次数已用完");
        String drugName = (String) body.getOrDefault("drugName", "");
        String prices = body.getOrDefault("prices", "[]").toString();
        Map<String, Object> result = insightService.analyze(drugName, prices);
        usageService.recordUsage(userId, "tool", "insight", 0);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/drug-qa")
    public AjaxResult drugQa(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        if (!usageService.checkQuota(userId, "tool", getUserTierLevel(userId)))
            return AjaxResult.error(429, "今日快捷功能次数已用完");
        String question = (String) body.getOrDefault("question", "");
        String drugName = (String) body.getOrDefault("drugName", "");
        Map<String, Object> result = drugQa.ask(question, drugName);
        usageService.recordUsage(userId, "tool", "drug_qa", 0);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/recommend")
    public AjaxResult recommendApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        if (!usageService.checkQuota(userId, "tool", getUserTierLevel(userId)))
            return AjaxResult.error(429, "今日快捷功能次数已用完");
        String category = (String) body.getOrDefault("category", "");
        int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 5;
        Map<String, Object> result = recommendService.recommend(category, limit);
        usageService.recordUsage(userId, "tool", "recommend", 0);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/usage")
    public AjaxResult usage() {
        Long userId = getUserId();
        return success(usageService.getTodayUsage(userId, getUserTierLevel(userId)));
    }

    @Autowired(required = false)
    private YyAiQuotaConfigMapper quotaConfigMapper;

    
    @GetMapping("/ai-quota/list")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult quotaList() {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return success(quotaConfigMapper.selectAll());
    }

    @GetMapping("/ai-quota/{id}")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult quotaGet(@PathVariable Long id) {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return success(quotaConfigMapper.selectAll().stream()
            .filter(c -> c.getId().equals(id)).findFirst().orElse(null));
    }

    @PutMapping("/ai-quota")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult quotaUpdate(@Valid @RequestBody YyAiQuotaConfig config) {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return toAjax(quotaConfigMapper.updateById(config));
    }

    int getUserTierLevel(Long userId) {
        return userTierService.getUserTierLevel(userId);
    }

    String buildSystemPrompt(String intent) {
        return switch (intent) {
            case "ADVISOR" -> "你是医药采购顾问，帮助用户选择最优采购平台和方案。";
            case "INSIGHT" -> "你是医药采购数据分析专家，分析价格差异原因。";
            case "DRUG_QA" -> "你是医药知识专家，提供准确的药品信息。涉到处方药时提醒咨询医生。";
            case "RECOMMEND" -> "你是医药采购推荐专家，根据采购历史推荐关联药品。";
            default -> "你是医药采购AI助手，帮助用户解决采购相关问题。";
        };
    }

    int getMaxTokens(int tierLevel) {
        return switch (tierLevel) {
            case 3 -> 1600;
            case 2 -> 1200;
            default -> 800;
        };
    }
}
