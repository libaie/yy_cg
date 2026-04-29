package com.ruoyi.yy.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yy.mapper.YyReferralConfigMapper;
import com.ruoyi.yy.domain.YyReferralConfig;
import com.ruoyi.yy.service.IYyReferralConfigService;

/**
 * 推荐奖励配置Service业务层处理
 */
@Service
public class YyReferralConfigServiceImpl implements IYyReferralConfigService
{
    @Autowired
    private YyReferralConfigMapper yyReferralConfigMapper;

    @Override
    public YyReferralConfig selectYyReferralConfigById(Long id) {
        return yyReferralConfigMapper.selectYyReferralConfigById(id);
    }

    @Override
    public List<YyReferralConfig> selectYyReferralConfigList(YyReferralConfig config) {
        return yyReferralConfigMapper.selectYyReferralConfigList(config);
    }

    @Override
    public int insertYyReferralConfig(YyReferralConfig config) {
        return yyReferralConfigMapper.insertYyReferralConfig(config);
    }

    @Override
    public int updateYyReferralConfig(YyReferralConfig config) {
        return yyReferralConfigMapper.updateYyReferralConfig(config);
    }

    @Override
    public int deleteYyReferralConfigByIds(Long[] ids) {
        return yyReferralConfigMapper.deleteYyReferralConfigByIds(ids);
    }

    @Override
    public int deleteYyReferralConfigById(Long id) {
        return yyReferralConfigMapper.deleteYyReferralConfigById(id);
    }
}
