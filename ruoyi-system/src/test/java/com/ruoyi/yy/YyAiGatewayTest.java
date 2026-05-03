package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.handler.YyCircuitBreaker;
import com.ruoyi.yy.service.impl.YyAiGatewayImpl;
import com.ruoyi.yy.service.impl.YyMockAiGateway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YyAiGatewayTest {

    @Test
    void mockGateway_returnsSuccess() {
        YyMockAiGateway gateway = new YyMockAiGateway();
        YyAiRequest request = new YyAiRequest("match", "test input");

        YyAiResponse response = gateway.call(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getContent());
        assertEquals("mock", response.getModel());
    }

    @Test
    void mockGateway_failure() {
        YyMockAiGateway gateway = new YyMockAiGateway();
        gateway.setShouldFail(true);
        YyAiRequest request = new YyAiRequest("match", "test input");

        YyAiResponse response = gateway.call(request);

        assertFalse(response.isSuccess());
        assertEquals("Mock failure", response.getErrorMessage());
    }

    @Test
    void circuitBreaker_preventsCalls() {
        YyCircuitBreaker cb = new YyCircuitBreaker(2, 300000);
        cb.recordFailure();
        cb.recordFailure();
        assertFalse(cb.allowRequest());
    }
}
