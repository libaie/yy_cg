package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiRecommendImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiRecommendImplTest {

    private IYyAiGateway gateway;
    private YyAiRecommendImpl recommend;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        recommend = new YyAiRecommendImpl();
        ReflectionTestUtils.setField(recommend, "aiGateway", gateway);
    }

    @Test
    void recommend_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "recommendations", List.of(
                Map.of("drugName", "头孢克洛", "reason", "同类抗生素", "estimatedPrice", 15.0)
            ),
            "basedOn", "基于阿莫西林采购历史"
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = recommend.recommend("抗生素", 5);

        assertNotNull(result);
        assertNotNull(result.get("recommendations"));
        assertNotNull(result.get("basedOn"));
    }

    @Test
    void recommend_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = recommend.recommend("抗生素", 5);

        assertNotNull(result);
        assertNotNull(result.get("recommendations"));
    }
}
