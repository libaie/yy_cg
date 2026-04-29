package com.ruoyi.yy.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.yy.domain.YyFieldMapping;
import com.ruoyi.yy.mapper.YyFieldMappingMapper;
import com.ruoyi.yy.service.IYyFieldMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 平台字段映射配置 Service 实现
 */
@Service
public class YyFieldMappingServiceImpl implements IYyFieldMappingService {

    @Autowired
    private YyFieldMappingMapper yyFieldMappingMapper;

    @Override
    public YyFieldMapping selectYyFieldMappingById(Long id) {
        return yyFieldMappingMapper.selectYyFieldMappingById(id);
    }

    @Override
    public List<YyFieldMapping> selectYyFieldMappingList(YyFieldMapping yyFieldMapping) {
        return yyFieldMappingMapper.selectYyFieldMappingList(yyFieldMapping);
    }

    @Override
    public Map<String, String> getPlatformMappings(Long platformId) {
        List<Map<String, String>> rows = yyFieldMappingMapper.selectMappingsByPlatformId(platformId);
        Map<String, String> result = new HashMap<>();
        for (Map<String, String> row : rows) {
            String standardField = row.get("standard_field");
            String platformField = row.get("platform_field");
            if (standardField != null && platformField != null) {
                result.put(standardField, platformField);
            }
        }
        return result;
    }

    @Override
    public String getEntryPath(Long platformId) {
        return yyFieldMappingMapper.selectEntryPathByPlatformId(platformId);
    }

    @Override
    public int insertYyFieldMapping(YyFieldMapping yyFieldMapping) {
        yyFieldMapping.setCreateTime(DateUtils.getNowDate());
        return yyFieldMappingMapper.insertYyFieldMapping(yyFieldMapping);
    }

    @Override
    public int updateYyFieldMapping(YyFieldMapping yyFieldMapping) {
        yyFieldMapping.setUpdateTime(DateUtils.getNowDate());
        return yyFieldMappingMapper.updateYyFieldMapping(yyFieldMapping);
    }

    @Override
    public int deleteYyFieldMappingByIds(Long[] ids) {
        return yyFieldMappingMapper.deleteYyFieldMappingByIds(ids);
    }

    @Override
    public int deleteYyFieldMappingById(Long id) {
        return yyFieldMappingMapper.deleteYyFieldMappingById(id);
    }

    @Override
    @Transactional
    public int batchSave(Long platformId, List<YyFieldMapping> list) {
        // 过滤空行：只保留 standardField 和 platformField 都有值的
        List<YyFieldMapping> validList = new ArrayList<>();
        for (YyFieldMapping mapping : list) {
            if (mapping.getStandardField() != null && !mapping.getStandardField().isEmpty()
                && mapping.getPlatformField() != null && !mapping.getPlatformField().isEmpty()) {
                mapping.setPlatformId(platformId);
                validList.add(mapping);
            }
        }
        if (validList.isEmpty()) {
            return 0;
        }

        // 校验：同一平台下标准字段不能重复
        Set<String> seen = new HashSet<>();
        for (YyFieldMapping mapping : validList) {
            if (!seen.add(mapping.getStandardField())) {
                throw new RuntimeException("标准字段「" + mapping.getStandardField() + "」重复配置，同一平台每个标准字段只能映射一次");
            }
        }

        return yyFieldMappingMapper.batchSaveYyFieldMapping(validList);
    }
}
