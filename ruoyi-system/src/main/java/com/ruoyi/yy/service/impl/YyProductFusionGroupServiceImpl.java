package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.mapper.YyProductFusionGroupMapper;
import com.ruoyi.yy.service.IYyProductFusionGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品融合分组 Service 实现
 */
@Service
public class YyProductFusionGroupServiceImpl implements IYyProductFusionGroupService {

    @Autowired
    private YyProductFusionGroupMapper yyProductFusionGroupMapper;

    @Override
    public YyProductFusionGroup selectYyProductFusionGroupById(Long id) {
        return yyProductFusionGroupMapper.selectYyProductFusionGroupById(id);
    }

    @Override
    public YyProductFusionGroup selectByFusionKey(String fusionKey) {
        return yyProductFusionGroupMapper.selectByFusionKey(fusionKey);
    }

    @Override
    public List<YyProductFusionGroup> selectYyProductFusionGroupList(YyProductFusionGroup yyProductFusionGroup) {
        return yyProductFusionGroupMapper.selectYyProductFusionGroupList(yyProductFusionGroup);
    }

    @Override
    public int insertYyProductFusionGroup(YyProductFusionGroup yyProductFusionGroup) {
        return yyProductFusionGroupMapper.insertYyProductFusionGroup(yyProductFusionGroup);
    }

    @Override
    public int updateYyProductFusionGroup(YyProductFusionGroup yyProductFusionGroup) {
        return yyProductFusionGroupMapper.updateYyProductFusionGroup(yyProductFusionGroup);
    }

    @Override
    public int deleteYyProductFusionGroupByIds(Long[] ids) {
        return yyProductFusionGroupMapper.deleteYyProductFusionGroupByIds(ids);
    }

    @Override
    public int deleteYyProductFusionGroupById(Long id) {
        return yyProductFusionGroupMapper.deleteYyProductFusionGroupById(id);
    }
}
