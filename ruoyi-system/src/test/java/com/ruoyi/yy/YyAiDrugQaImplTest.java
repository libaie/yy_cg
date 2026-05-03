package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiDrugQaImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiDrugQaImplTest {

    private IYyAiGateway gateway;
    private YyAiDrugQaImpl drugQa;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        drugQa = new YyAiDrugQaImpl();
        ReflectionTestUtils.setField(drugQa, "aiGateway", gateway);
    }

    @Test
    void ask_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "answer", "阿莫西林适用于敏感菌引起的感染",
            "sources", List.of("中国药典"),
            "warnings", List.of("青霉素过敏者禁用")
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = drugQa.ask("阿莫西林的适应症是什么", "阿莫西林胶囊");

        assertNotNull(result);
        assertEquals("阿莫西林适用于敏感菌引起的感染", result.get("answer"));
        assertNotNull(result.get("sources"));
        assertNotNull(result.get("warnings"));
    }

    @Test
    void ask_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = drugQa.ask("测试问题", "测试药品");

        assertNotNull(result);
        assertNotNull(result.get("answer"));
        assertTrue(((String) result.get("answer")).contains("暂时无法"));
    }

    @Test
    void ask_invalidJson_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("not json", "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = drugQa.ask("测试问题", "测试药品");

        assertNotNull(result);
        assertNotNull(result.get("answer"));
    }
}
