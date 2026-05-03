package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import java.util.List;

public interface YyAiQuotaConfigMapper {
    YyAiQuotaConfig selectByTierLevel(Integer tierLevel);
    List<YyAiQuotaConfig> selectAll();
    int updateById(YyAiQuotaConfig config);
}
