package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiIntentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiIntentRouterTest {

    private IYyAiGateway gateway;
    private YyAiIntentRouter router;

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        router = new YyAiIntentRouter();
        ReflectionTestUtils.setField(router, "aiGateway", gateway);
    }

    @Test
    void route_advisorIntent_returnsAdvisor() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("ADVISOR", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("阿莫西林哪个平台便宜");
        assertEquals("ADVISOR", intent);
    }

    @Test
    void route_insightIntent_returnsInsight() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("INSIGHT", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("为什么这个药品价格差异这么大");
        assertEquals("INSIGHT", intent);
    }

    @Test
    void route_drugQaIntent_returnsDrugQa() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("DRUG_QA", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("阿莫西林的用法用量是什么");
        assertEquals("DRUG_QA", intent);
    }

    @Test
    void route_recommendIntent_returnsRecommend() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("RECOMMEND", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("推荐一些抗生素类药品");
        assertEquals("RECOMMEND", intent);
    }

    @Test
    void route_generalIntent_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("GENERAL", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("你好");
        assertEquals("GENERAL", intent);
    }

    @Test
    void route_gatewayFailure_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        String intent = router.route("测试消息");
        assertEquals("GENERAL", intent);
    }

    @Test
    void route_invalidIntent_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("UNKNOWN_INTENT", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("测试消息");
        assertEquals("GENERAL", intent);
    }
}
