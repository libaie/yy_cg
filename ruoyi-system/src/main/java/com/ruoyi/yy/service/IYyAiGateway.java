package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;

/**
 * AI网关接口
 */
public interface IYyAiGateway {

    YyAiResponse call(YyAiRequest request);

    default boolean isHealthy() {
        return true;
    }
}
