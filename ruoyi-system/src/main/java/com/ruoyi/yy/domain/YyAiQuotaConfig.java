package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class YyAiQuotaConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer tierLevel;
    private Integer dailyChatLimit;
    private Integer dailyToolLimit;
    private Integer maxTokensPerReq;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTierLevel() { return tierLevel; }
    public void setTierLevel(Integer tierLevel) { this.tierLevel = tierLevel; }
    public Integer getDailyChatLimit() { return dailyChatLimit; }
    public void setDailyChatLimit(Integer dailyChatLimit) { this.dailyChatLimit = dailyChatLimit; }
    public Integer getDailyToolLimit() { return dailyToolLimit; }
    public void setDailyToolLimit(Integer dailyToolLimit) { this.dailyToolLimit = dailyToolLimit; }
    public Integer getMaxTokensPerReq() { return maxTokensPerReq; }
    public void setMaxTokensPerReq(Integer maxTokensPerReq) { this.maxTokensPerReq = maxTokensPerReq; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
