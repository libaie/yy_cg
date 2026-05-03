package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPurchaseAdvice;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiAdvisorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiAdvisorImplTest {

    private IYyAiGateway gateway;
    private YyAiAdvisorImpl advisor;

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        advisor = new YyAiAdvisorImpl();
        ReflectionTestUtils.setField(advisor, "aiGateway", gateway);
    }

    @Test
    void getAdvice_noPrices_returnsFallback() {
        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", Collections.emptyList());
        assertNotNull(advice);
        assertEquals("暂无该药品的比价数据", advice.getSummary());
    }

    @Test
    void getAdvice_nullPrices_returnsFallback() {
        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", null);
        assertNotNull(advice);
        assertEquals("暂无该药品的比价数据", advice.getSummary());
    }

    @Test
    void getAdvice_withPrices_callsGateway() {
        YyPriceComparison p1 = new YyPriceComparison();
        p1.setSourcePlatform("ysbang");
        p1.setCurrentPrice(new BigDecimal("25.50"));

        YyPriceComparison p2 = new YyPriceComparison();
        p2.setSourcePlatform("yaojingduo");
        p2.setCurrentPrice(new BigDecimal("23.00"));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("{\"summary\":\"推荐yaojingduo\",\"bestPlatform\":\"yaojingduo\",\"bestPrice\":23.00,\"totalSaving\":2.50,\"tips\":[\"价格较低\"]}", "qwen-turbo", 100, 50, 200)
        );

        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", Arrays.asList(p1, p2));

        assertNotNull(advice);
        verify(gateway).call(any(YyAiRequest.class));
    }

    @Test
    void getAdvice_gatewayFailure_returnsFallback() {
        YyPriceComparison p1 = new YyPriceComparison();
        p1.setSourcePlatform("ysbang");
        p1.setCurrentPrice(new BigDecimal("25.50"));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(YyAiResponse.fail("timeout"));

        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", List.of(p1));

        assertNotNull(advice);
        assertNotNull(advice.getSummary());
        assertEquals("ysbang", advice.getBestPlatform());
    }

    @Test
    void getAdvice_invalidJson_returnsFallback() {
        YyPriceComparison p1 = new YyPriceComparison();
        p1.setSourcePlatform("ysbang");
        p1.setCurrentPrice(new BigDecimal("25.50"));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("not valid json", "qwen-turbo", 100, 50, 200)
        );

        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", List.of(p1));

        assertNotNull(advice);
        assertEquals("ysbang", advice.getBestPlatform());
    }

    @Test
    void fallbackAdvice_selectsCheapest() {
        YyPriceComparison p1 = new YyPriceComparison();
        p1.setSourcePlatform("ysbang");
        p1.setCurrentPrice(new BigDecimal("25.50"));

        YyPriceComparison p2 = new YyPriceComparison();
        p2.setSourcePlatform("yaojingduo");
        p2.setCurrentPrice(new BigDecimal("23.00"));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(YyAiResponse.fail("error"));

        YyPurchaseAdvice advice = advisor.getAdvice("阿莫西林", Arrays.asList(p1, p2));

        assertEquals("yaojingduo", advice.getBestPlatform());
        assertEquals(new BigDecimal("23.00"), advice.getBestPrice());
    }
}
