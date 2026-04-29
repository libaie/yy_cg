package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyReferralReward;

/**
 * 推荐奖励记录Mapper接口
 */
public interface YyReferralRewardMapper {
    public YyReferralReward selectYyReferralRewardById(Long id);
    public List<YyReferralReward> selectYyReferralRewardList(YyReferralReward reward);
    public int insertYyReferralReward(YyReferralReward reward);
    public int updateYyReferralReward(YyReferralReward reward);
    public int deleteYyReferralRewardById(Long id);
    public int deleteYyReferralRewardByIds(Long[] ids);

    /**
     * 幂等检查：同一订单 + 同一推荐人 + 同一深度 不重复发
     */
    public int countDuplicateReward(Long referrerId, Long referredId, Integer depth, Long payOrderId);
}
