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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI统一网关 — DeepSeek API
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

    @Autowired(required = false)
    private com.ruoyi.yy.mapper.YyAiModelConfigMapper modelConfigMapper;

    @Value("${ai.deepseek.api-key:}")
    private String defaultApiKey;

    @Value("${ai.deepseek.endpoint:https://api.deepseek.com/v1/chat/completions}")
    private String defaultEndpoint;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String defaultModel;

    /** 根据 modelCode 动态获取模型配置，无配置时回退到 application.yml */
    private ModelConfig resolveModel(String modelCode) {
        if (modelConfigMapper != null && modelCode != null) {
            com.ruoyi.yy.domain.YyAiModelConfig cfg = modelConfigMapper.selectByModelCode(modelCode);
            if (cfg != null && cfg.getIsEnabled() != null && cfg.getIsEnabled() == 1) {
                return new ModelConfig(
                    cfg.getEndpoint() != null ? cfg.getEndpoint() : defaultEndpoint,
                    cfg.getApiKey() != null ? cfg.getApiKey() : defaultApiKey,
                    cfg.getModelCode()
                );
            }
        }
        return new ModelConfig(defaultEndpoint, defaultApiKey, modelCode != null ? modelCode : defaultModel);
    }

    private record ModelConfig(String endpoint, String apiKey, String model) {}

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
                log.warn("Redis read failed", e);
            }
        }

        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit OPEN, scene={}", request.getScene());
            return YyAiResponse.fail("AI服务暂时不可用，请稍后重试");
        }

        try {
            YyAiResponse response = callDeepSeek(request);
            if (response.isSuccess()) {
                circuitBreaker.recordSuccess();
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, response.getContent(), 24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.warn("Redis write failed", e);
                    }
                }
            } else {
                circuitBreaker.recordFailure();
            }
            return response;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("DeepSeek call failed scene={}", request.getScene(), e);
            return YyAiResponse.fail("AI服务调用失败，请稍后重试");
        }
    }

    @Override
    public StreamResult callStreamResult(YyAiRequest request) {
        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit OPEN for stream, scene={}", request.getScene());
            return new StreamResult(Collections.emptyIterator(), null);
        }

        AtomicBoolean cbRecorded = new AtomicBoolean(false);

        try {
            ModelConfig mc = resolveModel(request.getModel());
            String systemMsg = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String userMsg = request.getUserPrompt() != null ? request.getUserPrompt() : "";

            List<Map<String, String>> messages = new ArrayList<>();
            if (!systemMsg.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemMsg));
            }
            messages.add(Map.of("role", "user", "content", userMsg));

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("model", mc.model());
            reqBody.put("messages", messages);
            reqBody.put("temperature", request.getTemperature());
            reqBody.put("max_tokens", request.getMaxTokens());
            reqBody.put("stream", true);

            String requestBody = JSON.writeValueAsString(reqBody);

            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(mc.endpoint()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + mc.apiKey())
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<java.util.stream.Stream<String>> httpResponse =
                HTTP_CLIENT.send(httpReq, HttpResponse.BodyHandlers.ofLines());

            if (httpResponse.statusCode() != 200) {
                if (cbRecorded.compareAndSet(false, true)) circuitBreaker.recordFailure();
                httpResponse.body().close();
                log.error("DeepSeek stream HTTP {}", httpResponse.statusCode());
                return new StreamResult(Collections.emptyIterator(), null);
            }

            java.util.stream.Stream<String> responseBody = httpResponse.body();
            Iterator<String> lineIterator = responseBody.iterator();
            Iterator<String> tokenIterator = parseSseTokens(lineIterator);

            Iterator<String> safeIterator = new Iterator<String>() {
                @Override public boolean hasNext() {
                    try {
                        boolean hasMore = tokenIterator.hasNext();
                        if (!hasMore && cbRecorded.compareAndSet(false, true))
                            circuitBreaker.recordSuccess();
                        return hasMore;
                    } catch (Exception e) {
                        if (cbRecorded.compareAndSet(false, true)) circuitBreaker.recordFailure();
                        throw e;
                    }
                }
                @Override public String next() {
                    try { return tokenIterator.next(); }
                    catch (Exception e) {
                        if (cbRecorded.compareAndSet(false, true)) circuitBreaker.recordFailure();
                        throw e;
                    }
                }
            };

            return new StreamResult(safeIterator, responseBody::close);
        } catch (Exception e) {
            if (cbRecorded.compareAndSet(false, true)) circuitBreaker.recordFailure();
            log.error("Stream call failed", e);
            return new StreamResult(Collections.emptyIterator(), null);
        }
    }

    // ---- private methods ----

    private YyAiResponse callDeepSeek(YyAiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            ModelConfig mc = resolveModel(request.getModel());
            String systemMsg = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String userMsg = request.getUserPrompt() != null ? request.getUserPrompt() : "";

            List<Map<String, String>> messages = new ArrayList<>();
            if (!systemMsg.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemMsg));
            }
            messages.add(Map.of("role", "user", "content", userMsg));

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("model", mc.model());
            reqBody.put("messages", messages);
            reqBody.put("temperature", request.getTemperature());
            reqBody.put("max_tokens", request.getMaxTokens());
            reqBody.put("stream", false);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(mc.endpoint()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + mc.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(reqBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> httpResponse =
                HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long latency = System.currentTimeMillis() - startTime;

            if (httpResponse.statusCode() != 200) {
                return YyAiResponse.fail("HTTP " + httpResponse.statusCode());
            }

            JsonNode root = JSON.readTree(httpResponse.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                JsonNode usage = root.path("usage");
                int promptTokens = usage.path("prompt_tokens").asInt(0);
                int completionTokens = usage.path("completion_tokens").asInt(0);
                return YyAiResponse.ok(content, mc.model(), promptTokens, completionTokens, latency);
            }

            return YyAiResponse.fail("Empty response from AI");
        } catch (java.net.http.HttpTimeoutException e) {
            return YyAiResponse.fail("AI调用超时");
        } catch (Exception e) {
            return YyAiResponse.fail("AI调用异常");
        }
    }

    public String loadPromptTemplate(String templateCode) {
        if (promptTemplateMapper == null) return null;
        YyAiPromptTemplate template = promptTemplateMapper.selectYyAiPromptTemplateByCode(templateCode);
        return template != null ? template.getUserPromptTemplate() : null;
    }

    private String buildCacheKey(YyAiRequest request) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((request.getScene() + "|" + request.getUserPrompt()).getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", digest[i]));
            return "ai:cache:" + request.getScene() + ":" + sb;
        } catch (Exception e) {
            return "ai:cache:" + request.getScene() + ":" + request.getUserPrompt().hashCode();
        }
    }

    /**
     * Parse DeepSeek SSE stream into tokens.
     * Format: data: {"choices":[{"delta":{"content":"token"}}]}
     *         data: [DONE]
     */
    static Iterator<String> parseSseTokens(Iterator<String> lineIterator) {
        return new Iterator<String>() {
            private String nextToken = null;
            private boolean done = false;

            @Override
            public boolean hasNext() {
                if (done) return false;
                if (nextToken != null) return true;
                while (lineIterator.hasNext()) {
                    String line = lineIterator.next();
                    if (line.startsWith("data:")) {
                        String json = line.substring(5).trim();
                        if ("[DONE]".equals(json)) { done = true; return false; }
                        try {
                            JsonNode node = JSON.readTree(json);
                            JsonNode choices = node.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                String token = choices.get(0).path("delta").path("content").asText("");
                                if (!token.isEmpty()) { nextToken = token; return true; }
                            }
                        } catch (Exception e) { /* skip malformed */ }
                    }
                }
                done = true;
                return false;
            }

            @Override
            public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                String token = nextToken;
                nextToken = null;
                return token;
            }
        };
    }
}
