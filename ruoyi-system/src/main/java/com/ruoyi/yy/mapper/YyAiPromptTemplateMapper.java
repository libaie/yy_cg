package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiPromptTemplate;
import org.apache.ibatis.annotations.Param;

/**
 * AI Prompt模板Mapper接口
 */
public interface YyAiPromptTemplateMapper {

    YyAiPromptTemplate selectYyAiPromptTemplateByCode(@Param("templateCode") String templateCode);

    int insertYyAiPromptTemplate(YyAiPromptTemplate record);

    int updateYyAiPromptTemplate(YyAiPromptTemplate record);
}
