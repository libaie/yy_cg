package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiInsightImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiInsightImplTest {

    private IYyAiGateway gateway;
    private YyAiInsightImpl insight;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        insight = new YyAiInsightImpl();
        ReflectionTestUtils.setField(insight, "aiGateway", gateway);
    }

    @Test
    void analyze_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "summary", "该药品在不同平台价差约20%",
            "insights", List.of("药京价格最低", "平台B含运费"),
            "recommendation", "推荐在药京采购"
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = insight.analyze("阿莫西林", "[{\"platform\":\"药京\",\"price\":12.5}]");

        assertNotNull(result);
        assertEquals("该药品在不同平台价差约20%", result.get("summary"));
        assertNotNull(result.get("insights"));
        assertNotNull(result.get("recommendation"));
    }

    @Test
    void analyze_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = insight.analyze("测试药品", "[]");

        assertNotNull(result);
        assertTrue(((String) result.get("summary")).contains("暂时无法"));
    }
}
