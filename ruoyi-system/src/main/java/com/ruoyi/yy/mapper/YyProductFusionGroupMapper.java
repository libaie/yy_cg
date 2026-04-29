package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyProductFusionGroup;
import java.util.List;

/**
 * 商品融合分组 Mapper
 */
public interface YyProductFusionGroupMapper {

    YyProductFusionGroup selectYyProductFusionGroupById(Long id);

    YyProductFusionGroup selectByFusionKey(String fusionKey);

    List<YyProductFusionGroup> selectYyProductFusionGroupList(YyProductFusionGroup yyProductFusionGroup);

    int insertYyProductFusionGroup(YyProductFusionGroup yyProductFusionGroup);

    int updateYyProductFusionGroup(YyProductFusionGroup yyProductFusionGroup);

    int deleteYyProductFusionGroupByIds(Long[] ids);

    int deleteYyProductFusionGroupById(Long id);
}
