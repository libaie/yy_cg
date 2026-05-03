package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;

/**
 * 测试用Mock AI网关
 */
public class YyMockAiGateway implements IYyAiGateway {

    private String mockResponse;
    private boolean shouldFail;

    public YyMockAiGateway(String mockResponse) {
        this.mockResponse = mockResponse;
        this.shouldFail = false;
    }

    public YyMockAiGateway() {
        this.mockResponse = "{\"matched\": true, \"drug_id\": \"1\", \"confidence\": 0.95, \"reason\": \"mock\"}";
    }

    public void setMockResponse(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public YyAiResponse call(YyAiRequest request) {
        if (shouldFail) {
            return YyAiResponse.fail("Mock failure");
        }
        return YyAiResponse.ok(mockResponse, "mock", 100, 50, 100);
    }
}
