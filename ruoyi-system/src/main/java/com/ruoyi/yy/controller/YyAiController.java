package com.ruoyi.yy.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiUsageService;
import com.ruoyi.yy.service.impl.YyAiIntentRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/yy/ai")
public class YyAiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(YyAiController.class);

    @Autowired private YyAiUsageService usageService;
    @Autowired private YyAiIntentRouter intentRouter;
    @Autowired private IYyAiGateway aiGateway;
    @Autowired(required = false) private YyMemberSubscriptionMapper subscriptionMapper;
    @Autowired(required = false) private YyMemberTierMapper tierMapper;
    @Autowired private com.ruoyi.yy.service.impl.YyAiDrugQaImpl drugQa;
    @Autowired private com.ruoyi.yy.service.impl.YyAiInsightImpl insightService;
    @Autowired private com.ruoyi.yy.service.impl.YyAiRecommendImpl recommendService;

    // 有界线程池：核心 4 线程，最大 16 线程，队列容量 64，拒绝策略 CallerRunsPolicy
    private final ExecutorService sseExecutor = new ThreadPoolExecutor(
        4, 16, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(64),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PreDestroy
    public void destroy() {
        sseExecutor.shutdown();
        try {
            if (!sseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                sseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);

        if (!usageService.checkQuota(userId, "chat", tierLevel)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .data("{\"type\":\"error\",\"data\":\"今日对话次数已用完，升级会员解锁更多\"}"));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        String message = (String) body.getOrDefault("message", "");

        SseEmitter emitter = new SseEmitter(60_000L);
        sseExecutor.submit(() -> {
            IYyAiGateway.StreamResult streamResult = null;
            try {
                emitter.send(SseEmitter.event()
                    .data("{\"type\":\"status\",\"data\":\"analyzing_intent\"}"));

                String intent = intentRouter.route(message);
                String systemPrompt = buildSystemPrompt(intent);
                int maxTokens = getMaxTokens(tierLevel);

                YyAiRequest request = new YyAiRequest();
                request.setScene("chat_" + intent.toLowerCase());
                request.setSystemPrompt(systemPrompt);
                request.setUserPrompt(message);
                request.setModel("qwen-turbo");
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
                        .data("{\"type\":\"token\",\"data\":\"" + escapeJson(token) + "\",\"intent\":\"" + intent + "\"}"));
                    tokenCount++;
                }

                emitter.send(SseEmitter.event()
                    .data("{\"type\":\"done\",\"data\":\"\"}"));
                usageService.recordUsage(userId, "chat", null, tokenCount);

            } catch (IOException e) {
                log.debug("SSE client disconnected: {}", e.getMessage());
            } catch (Exception e) {
                log.error("SSE chat error", e);
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"type\":\"error\",\"data\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {}
            } finally {
                if (streamResult != null) streamResult.close();
                emitter.complete();
            }
        });

        return emitter;
    }

    @PostMapping("/advisor")
    public AjaxResult advisor(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return AjaxResult.error(429, "今日快捷功能次数已用完，升级会员解锁更多");
        }
        String drugName = (String) body.getOrDefault("drugName", "");
        usageService.recordUsage(userId, "tool", "advisor", 0);
        return success("采购顾问功能已调用");
    }

    @PostMapping("/insight")
    public AjaxResult insightApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return AjaxResult.error(429, "今日快捷功能次数已用完，升级会员解锁更多");
        }
        String drugName = (String) body.getOrDefault("drugName", "");
        String prices = body.getOrDefault("prices", "[]").toString();
        Map<String, Object> result = insightService.analyze(drugName, prices);
        usageService.recordUsage(userId, "tool", "insight", 0);
        return success(result);
    }

    @PostMapping("/drug-qa")
    public AjaxResult drugQa(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return AjaxResult.error(429, "今日快捷功能次数已用完，升级会员解锁更多");
        }
        String question = (String) body.getOrDefault("question", "");
        String drugName = (String) body.getOrDefault("drugName", "");
        Map<String, Object> result = drugQa.ask(question, drugName);
        usageService.recordUsage(userId, "tool", "drug_qa", 0);
        return success(result);
    }

    @PostMapping("/recommend")
    public AjaxResult recommendApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return AjaxResult.error(429, "今日快捷功能次数已用完，升级会员解锁更多");
        }
        String category = (String) body.getOrDefault("category", "");
        int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 5;
        Map<String, Object> result = recommendService.recommend(category, limit);
        usageService.recordUsage(userId, "tool", "recommend", 0);
        return success(result);
    }

    @GetMapping("/usage")
    public AjaxResult usage() {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel(userId);
        return success(usageService.getTodayUsage(userId, tierLevel));
    }

    // ===== 配额管理端点（管理端使用）=====

    @Autowired(required = false)
    private com.ruoyi.yy.mapper.YyAiQuotaConfigMapper quotaConfigMapper;

    @GetMapping("/ai-quota/list")
    public AjaxResult quotaList() {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return success(quotaConfigMapper.selectAll());
    }

    @GetMapping("/ai-quota/{id}")
    public AjaxResult quotaGet(@PathVariable Long id) {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return success(quotaConfigMapper.selectAll().stream()
            .filter(c -> c.getId().equals(id)).findFirst().orElse(null));
    }

    @PutMapping("/ai-quota")
    public AjaxResult quotaUpdate(@RequestBody com.ruoyi.yy.domain.YyAiQuotaConfig config) {
        if (quotaConfigMapper == null) return error("配额服务未初始化");
        return toAjax(quotaConfigMapper.updateById(config));
    }

    /**
     * 获取用户会员等级
     * YyUser → YyMemberSubscription → YyMemberTier.memberLevel
     */
    int getUserTierLevel(Long userId) {
        if (subscriptionMapper == null || tierMapper == null) return 0;
        try {
            YyMemberSubscription query = new YyMemberSubscription();
            query.setUserId(userId);
            query.setPayStatus(1);
            List<YyMemberSubscription> subs = subscriptionMapper.selectYyMemberSubscriptionList(query);
            if (subs == null || subs.isEmpty()) return 0;
            YyMemberSubscription sub = subs.get(0);
            YyMemberTier tier = tierMapper.selectYyMemberTierByTierId(sub.getTierId());
            return tier != null ? tier.getMemberLevel() : 0;
        } catch (Exception e) {
            log.warn("Failed to get user tier level for userId={}", userId, e);
            return 0;
        }
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

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
