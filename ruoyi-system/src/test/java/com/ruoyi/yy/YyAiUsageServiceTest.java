package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import com.ruoyi.yy.mapper.YyAiQuotaConfigMapper;
import com.ruoyi.yy.mapper.YyAiUsageLogMapper;
import com.ruoyi.yy.service.impl.YyAiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiUsageServiceTest {

    private YyAiQuotaConfigMapper quotaMapper;
    private YyAiUsageLogMapper usageLogMapper;
    private YyAiUsageService service;

    @BeforeEach
    void setUp() {
        quotaMapper = mock(YyAiQuotaConfigMapper.class);
        usageLogMapper = mock(YyAiUsageLogMapper.class);
        service = new YyAiUsageService(quotaMapper, usageLogMapper);
    }

    @Test
    void checkQuota_unlimited_returnsTrue() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setEnabled(1);
        config.setDailyChatLimit(-1);
        when(quotaMapper.selectByTierLevel(2)).thenReturn(config);

        assertTrue(service.checkQuota(1L, "chat", 2));
    }

    @Test
    void checkQuota_underLimit_returnsTrue() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setEnabled(1);
        config.setDailyChatLimit(10);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(5);

        assertTrue(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_atLimit_returnsFalse() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(10);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_noConfig_returnsFalse() {
        when(quotaMapper.selectByTierLevel(0)).thenReturn(null);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_disabled_returnsFalse() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        config.setEnabled(0);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void recordUsage_insertsLog() {
        service.recordUsage(1L, "chat", null, 100);

        verify(usageLogMapper).insert(argThat(log ->
            log.getUserId().equals(1L) &&
            log.getUsageType().equals("chat") &&
            log.getTokensUsed().equals(100)
        ));
    }

    @Test
    void getTodayUsage_returnsCorrectInfo() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        config.setDailyToolLimit(5);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(3);
        when(usageLogMapper.countTodayByUserAndType(1L, "tool")).thenReturn(1);

        var info = service.getTodayUsage(1L, 0);

        assertEquals(3, info.get("chatUsed"));
        assertEquals(10, info.get("chatLimit"));
        assertEquals(1, info.get("toolUsed"));
        assertEquals(5, info.get("toolLimit"));
    }
}
