package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import java.util.Iterator;

/**
 * AI网关接口
 */
public interface IYyAiGateway {

    YyAiResponse call(YyAiRequest request);

    default boolean isHealthy() {
        return true;
    }

    /**
     * 流式调用结果，封装 Iterator 和底层 HTTP 响应流
     * 调用方必须在消费完毕或客户端断开时调用 close() 释放连接
     */
    class StreamResult implements AutoCloseable {
        private final Iterator<String> tokens;
        private final AutoCloseable resource;

        public StreamResult(Iterator<String> tokens, AutoCloseable resource) {
            this.tokens = tokens;
            this.resource = resource;
        }

        public Iterator<String> getTokens() { return tokens; }

        @Override
        public void close() {
            try { if (resource != null) resource.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 流式调用，返回可关闭的 StreamResult
     * Controller 必须在 SseEmitter.onCompletion() 中调用 result.close()
     */
    default StreamResult callStreamResult(YyAiRequest request) {
        throw new UnsupportedOperationException("callStreamResult not implemented");
    }
}
