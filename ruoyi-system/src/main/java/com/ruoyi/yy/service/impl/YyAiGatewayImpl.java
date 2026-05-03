package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiPromptTemplate;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.handler.YyCircuitBreaker;
import com.ruoyi.yy.mapper.YyAiPromptTemplateMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI统一网关 -- 封装通义千问API调用
 */
@Service
public class YyAiGatewayImpl implements IYyAiGateway {

    private static final Logger log = LoggerFactory.getLogger(YyAiGatewayImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final YyCircuitBreaker circuitBreaker = new YyCircuitBreaker(3, 300_000);

    @Autowired(required = false)
    private YyAiPromptTemplateMapper promptTemplateMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${ai.dashscope.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    @Override
    public YyAiResponse call(YyAiRequest request) {
        long startTime = System.currentTimeMillis();

        String cacheKey = buildCacheKey(request);
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return YyAiResponse.ok(cached, "cached", 0, 0,
                        System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed", e);
            }
        }

        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker OPEN, skipping AI call for scene={}", request.getScene());
            return YyAiResponse.fail("Circuit breaker open, AI service temporarily unavailable");
        }

        try {
            YyAiResponse response = callDashScope(request);
            if (response.isSuccess()) {
                circuitBreaker.recordSuccess();
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, response.getContent(),
                            24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.warn("Redis cache write failed", e);
                    }
                }
            } else {
                circuitBreaker.recordFailure();
            }
            return response;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("AI call failed for scene={}", request.getScene(), e);
            return YyAiResponse.fail(e.getMessage());
        }
    }

    private YyAiResponse callDashScope(YyAiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            String requestBody = JSON.writeValueAsString(Map.of(
                "model", request.getModel(),
                "input", Map.of(
                    "messages", new Object[]{
                        Map.of("role", "system", "content", request.getSystemPrompt()),
                        Map.of("role", "user", "content", request.getUserPrompt())
                    }
                ),
                "parameters", Map.of(
                    "temperature", request.getTemperature(),
                    "max_tokens", request.getMaxTokens(),
                    "result_format", "message"
                )
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> httpResponse =
                HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long latency = System.currentTimeMillis() - startTime;

            if (httpResponse.statusCode() != 200) {
                return YyAiResponse.fail("HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
            }

            JsonNode root = JSON.readTree(httpResponse.body());
            JsonNode output = root.path("output");
            JsonNode choices = output.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                JsonNode usage = output.path("usage");
                int promptTokens = usage.path("input_tokens").asInt(0);
                int completionTokens = usage.path("output_tokens").asInt(0);
                return YyAiResponse.ok(content, request.getModel(), promptTokens, completionTokens, latency);
            }

            return YyAiResponse.fail("Empty response from DashScope");
        } catch (java.net.http.HttpTimeoutException e) {
            return YyAiResponse.fail("AI call timeout after 10s");
        } catch (Exception e) {
            return YyAiResponse.fail("AI call error: " + e.getMessage());
        }
    }

    public String loadPromptTemplate(String templateCode) {
        if (promptTemplateMapper == null) return null;
        YyAiPromptTemplate template = promptTemplateMapper.selectYyAiPromptTemplateByCode(templateCode);
        return template != null ? template.getUserPromptTemplate() : null;
    }

    private String buildCacheKey(YyAiRequest request) {
        String hash = Integer.toHexString(
            (request.getScene() + "|" + request.getUserPrompt()).hashCode()
        );
        return "ai:cache:" + request.getScene() + ":" + hash;
    }
}
