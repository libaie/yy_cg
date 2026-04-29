package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyReferralConfig;

/**
 * 推荐奖励配置Mapper接口
 */
public interface YyReferralConfigMapper {
    public YyReferralConfig selectYyReferralConfigById(Long id);
    public List<YyReferralConfig> selectYyReferralConfigList(YyReferralConfig config);
    /** 查询启用的配置，按 tierId 精确匹配 */
    public YyReferralConfig selectActiveTierConfig(Long tierId);
    /** 查询启用的全等级通用配置（tierId IS NULL） */
    public YyReferralConfig selectActiveGlobalConfig();
    public int insertYyReferralConfig(YyReferralConfig config);
    public int updateYyReferralConfig(YyReferralConfig config);
    public int deleteYyReferralConfigById(Long id);
    public int deleteYyReferralConfigByIds(Long[] ids);
}
