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
import java.util.Collections;
import java.util.Iterator;
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

    @Override
    public StreamResult callStreamResult(YyAiRequest request) {
        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker OPEN for stream, scene={}", request.getScene());
            return new StreamResult(Collections.emptyIterator(), null);
        }

        try {
            String systemMsg = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String userMsg = request.getUserPrompt() != null ? request.getUserPrompt() : "";
            String requestBody = JSON.writeValueAsString(Map.of(
                "model", request.getModel(),
                "input", Map.of(
                    "messages", new Object[]{
                        Map.of("role", "system", "content", systemMsg),
                        Map.of("role", "user", "content", userMsg)
                    }
                ),
                "parameters", Map.of(
                    "temperature", request.getTemperature(),
                    "max_tokens", request.getMaxTokens(),
                    "result_format", "message",
                    "stream", true
                )
            ));

            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<java.util.stream.Stream<String>> httpResponse =
                HTTP_CLIENT.send(httpReq, HttpResponse.BodyHandlers.ofLines());

            if (httpResponse.statusCode() != 200) {
                circuitBreaker.recordFailure();
                httpResponse.body().close();
                return new StreamResult(Collections.emptyIterator(), null);
            }

            java.util.stream.Stream<String> responseBody = httpResponse.body();
            Iterator<String> lineIterator = responseBody.iterator();
            Iterator<String> tokenIterator = new Iterator<String>() {
                private String nextToken = null;
                private boolean done = false;
                private boolean successRecorded = false;

                @Override
                public boolean hasNext() {
                    if (done) return false;
                    if (nextToken != null) return true;
                    while (lineIterator.hasNext()) {
                        String line = lineIterator.next();
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            if ("[DONE]".equals(json)) {
                                markDone(true);
                                return false;
                            }
                            try {
                                JsonNode node = JSON.readTree(json);
                                String token = node.path("output").path("text").asText("");
                                if (!token.isEmpty()) {
                                    nextToken = token;
                                    return true;
                                }
                            } catch (Exception e) {
                                log.debug("Skip malformed SSE line: {}", line);
                            }
                        }
                    }
                    markDone(true);
                    return false;
                }

                @Override
                public String next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    String token = nextToken;
                    nextToken = null;
                    return token;
                }

                private void markDone(boolean success) {
                    if (!successRecorded) {
                        successRecorded = true;
                        done = true;
                        if (success) {
                            circuitBreaker.recordSuccess();
                        } else {
                            circuitBreaker.recordFailure();
                        }
                    }
                }
            };

            Iterator<String> safeIterator = new Iterator<String>() {
                @Override public boolean hasNext() {
                    try { return tokenIterator.hasNext(); }
                    catch (Exception e) { circuitBreaker.recordFailure(); throw e; }
                }
                @Override public String next() {
                    try { return tokenIterator.next(); }
                    catch (Exception e) { circuitBreaker.recordFailure(); throw e; }
                }
            };

            return new StreamResult(safeIterator, responseBody::close);
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("Stream call failed", e);
            return new StreamResult(Collections.emptyIterator(), null);
        }
    }

    private YyAiResponse callDashScope(YyAiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            String systemMsg = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String userMsg = request.getUserPrompt() != null ? request.getUserPrompt() : "";
            String requestBody = JSON.writeValueAsString(Map.of(
                "model", request.getModel(),
                "input", Map.of(
                    "messages", new Object[]{
                        Map.of("role", "system", "content", systemMsg),
                        Map.of("role", "user", "content", userMsg)
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
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((request.getScene() + "|" + request.getUserPrompt()).getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return "ai:cache:" + request.getScene() + ":" + sb;
        } catch (Exception e) {
            return "ai:cache:" + request.getScene() + ":" + request.getUserPrompt().hashCode();
        }
    }
}
