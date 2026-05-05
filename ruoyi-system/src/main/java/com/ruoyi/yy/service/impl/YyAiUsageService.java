package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import com.ruoyi.yy.domain.YyAiUsageLog;
import com.ruoyi.yy.mapper.YyAiQuotaConfigMapper;
import com.ruoyi.yy.mapper.YyAiUsageLogMapper;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class YyAiUsageService {

    private final YyAiQuotaConfigMapper quotaMapper;
    private final YyAiUsageLogMapper usageLogMapper;

    public YyAiUsageService(YyAiQuotaConfigMapper quotaMapper, YyAiUsageLogMapper usageLogMapper) {
        this.quotaMapper = quotaMapper;
        this.usageLogMapper = usageLogMapper;
    }

    public boolean checkQuota(Long userId, String usageType, int tierLevel) {
        YyAiQuotaConfig config = quotaMapper.selectByTierLevel(tierLevel);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0) {
            return false;
        }

        int limit = "chat".equals(usageType) ? config.getDailyChatLimit() : config.getDailyToolLimit();
        if (limit == -1) {
            return true;
        }

        int used = usageLogMapper.countTodayByUserAndType(userId, usageType);
        return used < limit;
    }

    public void recordUsage(Long userId, String usageType, String toolName, int tokens) {
        YyAiUsageLog log = new YyAiUsageLog();
        log.setUserId(userId);
        log.setUsageType(usageType);
        log.setToolName(toolName);
        log.setTokensUsed(tokens);
        usageLogMapper.insert(log);
    }

    public Map<String, Object> getTodayUsage(Long userId, int tierLevel) {
        YyAiQuotaConfig config = quotaMapper.selectByTierLevel(tierLevel);
        int chatLimit = config != null ? config.getDailyChatLimit() : 0;
        int toolLimit = config != null ? config.getDailyToolLimit() : 0;
        int chatUsed = usageLogMapper.countTodayByUserAndType(userId, "chat");
        int toolUsed = usageLogMapper.countTodayByUserAndType(userId, "tool");

        Map<String, Object> info = new HashMap<>();
        info.put("chatUsed", chatUsed);
        info.put("chatLimit", chatLimit);
        info.put("toolUsed", toolUsed);
        info.put("toolLimit", toolLimit);
        info.put("model", "deepseek-chat");
        return info;
    }
}
