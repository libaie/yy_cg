package com.ruoyi.yy.controller;

import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiIntentRouter;
import com.ruoyi.yy.service.impl.YyAiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiControllerTest {

    private YyAiController controller;
    private YyAiUsageService usageService;
    private YyAiIntentRouter intentRouter;
    private IYyAiGateway aiGateway;

    @BeforeEach
    void setUp() {
        controller = new YyAiController();
        usageService = mock(YyAiUsageService.class);
        intentRouter = mock(YyAiIntentRouter.class);
        aiGateway = mock(IYyAiGateway.class);

        ReflectionTestUtils.setField(controller, "usageService", usageService);
        ReflectionTestUtils.setField(controller, "intentRouter", intentRouter);
        ReflectionTestUtils.setField(controller, "aiGateway", aiGateway);
        ReflectionTestUtils.setField(controller, "subscriptionMapper", mock(YyMemberSubscriptionMapper.class));
        ReflectionTestUtils.setField(controller, "tierMapper", mock(YyMemberTierMapper.class));
        ReflectionTestUtils.setField(controller, "drugQa", mock(com.ruoyi.yy.service.impl.YyAiDrugQaImpl.class));
        ReflectionTestUtils.setField(controller, "insightService", mock(com.ruoyi.yy.service.impl.YyAiInsightImpl.class));
        ReflectionTestUtils.setField(controller, "recommendService", mock(com.ruoyi.yy.service.impl.YyAiRecommendImpl.class));
    }

    @Test
    void chat_quotaExceeded_returnsEmitter() {
        YyAiController spyCtrl = spy(controller);
        doReturn(1L).when(spyCtrl).getUserId();
        when(usageService.checkQuota(eq(1L), eq("chat"), anyInt())).thenReturn(false);

        SseEmitter result = spyCtrl.chat(Map.of("message", "test"));
        assertNotNull(result);
    }

    @Test
    void getUserTierLevel_noSubscription_returnsZero() {
        YyMemberSubscriptionMapper subMapper = mock(YyMemberSubscriptionMapper.class);
        when(subMapper.selectYyMemberSubscriptionList(any())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(controller, "subscriptionMapper", subMapper);

        int level = controller.getUserTierLevel(1L);
        assertEquals(0, level);
    }

    @Test
    void getUserTierLevel_hasSubscription_returnsMemberLevel() {
        YyMemberSubscriptionMapper subMapper = mock(YyMemberSubscriptionMapper.class);
        YyMemberTierMapper tierMapper = mock(YyMemberTierMapper.class);
        ReflectionTestUtils.setField(controller, "subscriptionMapper", subMapper);
        ReflectionTestUtils.setField(controller, "tierMapper", tierMapper);

        YyMemberSubscription sub = new YyMemberSubscription();
        sub.setTierId(2L);
        when(subMapper.selectYyMemberSubscriptionList(any())).thenReturn(List.of(sub));

        YyMemberTier tier = new YyMemberTier();
        tier.setMemberLevel(2);
        when(tierMapper.selectYyMemberTierByTierId(2L)).thenReturn(tier);

        int level = controller.getUserTierLevel(1L);
        assertEquals(2, level);
    }

    @Test
    void escapeJson_escapesSpecialChars() {
        assertEquals("hello\\nworld", YyAiController.escapeJson("hello\nworld"));
        assertEquals("say \\\"hi\\\"", YyAiController.escapeJson("say \"hi\""));
        assertEquals("", YyAiController.escapeJson(null));
    }

    @Test
    void buildSystemPrompt_returnsCorrectPrompt() {
        assertEquals("你是医药采购顾问，帮助用户选择最优采购平台和方案。",
            controller.buildSystemPrompt("ADVISOR"));
        assertEquals("你是医药采购AI助手，帮助用户解决采购相关问题。",
            controller.buildSystemPrompt("GENERAL"));
    }

    @Test
    void getMaxTokens_returnsCorrectValues() {
        assertEquals(1600, controller.getMaxTokens(3));
        assertEquals(1200, controller.getMaxTokens(2));
        assertEquals(800, controller.getMaxTokens(0));
        assertEquals(800, controller.getMaxTokens(1));
    }

    @Test
    void advisor_quotaExceeded_returnsError() {
        YyAiController spyCtrl = spy(controller);
        doReturn(1L).when(spyCtrl).getUserId();
        when(usageService.checkQuota(eq(1L), eq("tool"), anyInt())).thenReturn(false);

        var result = spyCtrl.advisor(Map.of("drugName", "阿莫西林"));
        assertNotNull(result);
    }

    @Test
    void usage_returnsSuccess() {
        YyAiController spyCtrl = spy(controller);
        doReturn(1L).when(spyCtrl).getUserId();
        when(usageService.getTodayUsage(eq(1L), anyInt())).thenReturn(Map.of("chatUsed", 3));

        var result = spyCtrl.usage();
        assertNotNull(result);
    }
}
