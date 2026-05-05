package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyFieldMappingRule;
import java.util.List;

public interface YyFieldMappingRuleMapper {
    List<YyFieldMappingRule> selectByPlatformAndApi(Long platformId, String apiCode);
    List<YyFieldMappingRule> selectAll(YyFieldMappingRule rule);
    YyFieldMappingRule selectById(Long id);
    int insert(YyFieldMappingRule rule);
    int batchInsert(List<YyFieldMappingRule> list);
    int update(YyFieldMappingRule rule);
    int deleteById(Long id);
}
