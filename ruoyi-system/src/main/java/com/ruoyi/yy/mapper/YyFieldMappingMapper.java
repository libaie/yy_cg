package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyFieldMapping;
import java.util.List;
import java.util.Map;

/**
 * 平台字段映射配置 Mapper
 */
public interface YyFieldMappingMapper {

    YyFieldMapping selectYyFieldMappingById(Long id);

    List<YyFieldMapping> selectYyFieldMappingList(YyFieldMapping yyFieldMapping);

    /**
     * 根据平台ID查询所有启用的映射（返回Map: standardField -> platformField）
     */
    List<Map<String, String>> selectMappingsByPlatformId(Long platformId);

    /**
     * 根据平台ID查询数据入口路径（取第一条启用记录的 entry_path）
     */
    String selectEntryPathByPlatformId(Long platformId);

    int insertYyFieldMapping(YyFieldMapping yyFieldMapping);

    int updateYyFieldMapping(YyFieldMapping yyFieldMapping);

    int deleteYyFieldMappingByIds(Long[] ids);

    int deleteYyFieldMappingById(Long id);

    /**
     * 批量保存（插入或更新）
     */
    int batchSaveYyFieldMapping(List<YyFieldMapping> list);
}
