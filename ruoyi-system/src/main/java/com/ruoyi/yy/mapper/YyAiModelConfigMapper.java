package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiModelConfig;
import java.util.List;

public interface YyAiModelConfigMapper {
    List<YyAiModelConfig> selectEnabled();
    YyAiModelConfig selectByModelCode(String modelCode);
    List<YyAiModelConfig> selectAll();
    YyAiModelConfig selectById(Long id);
    int insert(YyAiModelConfig config);
    int updateById(YyAiModelConfig config);
    int deleteById(Long id);
}
