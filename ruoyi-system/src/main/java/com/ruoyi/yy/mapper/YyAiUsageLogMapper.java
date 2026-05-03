package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiUsageLog;
import org.apache.ibatis.annotations.Param;

public interface YyAiUsageLogMapper {
    int insert(YyAiUsageLog log);
    int countTodayByUserAndType(@Param("userId") Long userId, @Param("usageType") String usageType);
}
