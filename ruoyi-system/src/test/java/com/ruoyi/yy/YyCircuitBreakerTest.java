package com.ruoyi.yy;

import com.ruoyi.yy.handler.YyCircuitBreaker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YyCircuitBreakerTest {

    @Test
    void initialState_closed() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        assertTrue(cb.allowRequest());
        assertEquals("CLOSED", cb.getState());
    }

    @Test
    void tripAfterFailures() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        cb.recordFailure();
        cb.recordFailure();
        assertTrue(cb.allowRequest());
        cb.recordFailure();
        assertFalse(cb.allowRequest());
        assertEquals("OPEN", cb.getState());
    }

    @Test
    void halfOpenAfterTimeout() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 100);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();
        assertFalse(cb.allowRequest());

        try { Thread.sleep(150); } catch (InterruptedException ignored) {}

        assertTrue(cb.allowRequest());
        assertEquals("HALF_OPEN", cb.getState());
    }

    @Test
    void halfOpen_success_closes() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 100);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        cb.allowRequest();
        cb.recordSuccess();
        assertEquals("CLOSED", cb.getState());
    }

    @Test
    void recordSuccess_resetsFailureCount() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordSuccess();
        cb.recordFailure();
        cb.recordFailure();
        assertTrue(cb.allowRequest());
    }
}
