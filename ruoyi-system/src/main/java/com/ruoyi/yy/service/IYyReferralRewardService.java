package com.ruoyi.yy.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.yy.domain.YyReferralReward;

/**
 * 推荐奖励记录Service接口
 */
public interface IYyReferralRewardService {
    public YyReferralReward selectYyReferralRewardById(Long id);
    public List<YyReferralReward> selectYyReferralRewardList(YyReferralReward reward);
    public int deleteYyReferralRewardByIds(Long[] ids);
    public int deleteYyReferralRewardById(Long id);
    
    /**
     * 处理充值推荐奖励
     * @param userId 充值用户ID
     * @param tierId 会员等级ID（可能为NULL）
     * @param payAmount 支付金额
     * @param payOrderId 关联订单/订阅ID
     */
    public void processReferralReward(Long userId, Long tierId, BigDecimal payAmount, Long payOrderId);
}
