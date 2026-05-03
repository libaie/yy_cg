package com.ruoyi.yy.handler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器实现（线程安全）
 *
 * CLOSED -> 连续失败达到阈值 -> OPEN
 * OPEN -> 等待超时 -> HALF_OPEN（仅放行1个探测请求）
 * HALF_OPEN -> 成功 -> CLOSED / 失败 -> OPEN
 */
public class YyCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openDurationMs;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger halfOpenPermits = new AtomicInteger(0);

    public YyCircuitBreaker(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    public synchronized boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > openDurationMs) {
                state.set(State.HALF_OPEN);
                halfOpenPermits.set(1);
                return true;
            }
            return false;
        }
        // HALF_OPEN — allow exactly one probe request
        if (halfOpenPermits.decrementAndGet() >= 0) {
            return true;
        }
        halfOpenPermits.set(0);
        return false;
    }

    public synchronized void recordSuccess() {
        failureCount.set(0);
        halfOpenPermits.set(0);
        state.set(State.CLOSED);
    }

    public synchronized void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    public String getState() {
        return state.get().name();
    }
}
