package com.ruoyi.yy.service;

import java.util.List;
import com.ruoyi.yy.domain.YyReferralConfig;

/**
 * 推荐奖励配置Service接口
 */
public interface IYyReferralConfigService {
    public YyReferralConfig selectYyReferralConfigById(Long id);
    public List<YyReferralConfig> selectYyReferralConfigList(YyReferralConfig config);
    public int insertYyReferralConfig(YyReferralConfig config);
    public int updateYyReferralConfig(YyReferralConfig config);
    public int deleteYyReferralConfigByIds(Long[] ids);
    public int deleteYyReferralConfigById(Long id);
}
