package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyFieldMapping;
import java.util.List;
import java.util.Map;

/**
 * 平台字段映射配置 Service
 */
public interface IYyFieldMappingService {

    YyFieldMapping selectYyFieldMappingById(Long id);

    List<YyFieldMapping> selectYyFieldMappingList(YyFieldMapping yyFieldMapping);

    /**
     * 根据平台ID获取映射 Map: standardField -> platformField
     */
    Map<String, String> getPlatformMappings(Long platformId);

    /**
     * 根据平台ID获取数据入口路径
     */
    String getEntryPath(Long platformId);

    int insertYyFieldMapping(YyFieldMapping yyFieldMapping);

    int updateYyFieldMapping(YyFieldMapping yyFieldMapping);

    int deleteYyFieldMappingByIds(Long[] ids);

    int deleteYyFieldMappingById(Long id);

    /**
     * 批量保存某个平台的全部映射（先删后增，或使用 ON DUPLICATE KEY）
     */
    int batchSave(Long platformId, List<YyFieldMapping> list);
}
