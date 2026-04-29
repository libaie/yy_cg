package com.ruoyi.yy.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.yy.domain.YyReferralConfig;
import com.ruoyi.yy.domain.YyReferralReward;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.yy.mapper.YyReferralConfigMapper;
import com.ruoyi.yy.mapper.YyReferralRewardMapper;
import com.ruoyi.yy.mapper.YyUserMapper;
import com.ruoyi.yy.service.IYyReferralRewardService;
import com.ruoyi.yy.service.IYyUserService;
import com.ruoyi.common.utils.DateUtils;

/**
 * 推荐奖励记录Service业务层处理（核心推荐引擎）
 * 
 * @author ruoyi
 * @date 2026-04-05
 */
@Service
public class YyReferralRewardServiceImpl implements IYyReferralRewardService 
{
    private static final Logger log = LoggerFactory.getLogger(YyReferralRewardServiceImpl.class);

    @Autowired
    private YyReferralRewardMapper yyReferralRewardMapper;

    @Autowired
    private YyReferralConfigMapper yyReferralConfigMapper;

    @Autowired
    private YyUserMapper yyUserMapper;

    @Autowired
    private IYyUserService yyUserService;

    /**
     * 处理推荐奖励 — 当用户充值/续费会员时调用
     * 
     * @param referredId 被推荐人（充值用户）ID
     * @param tierId 充值的会员套餐ID
     * @param payAmount 实际支付金额
     * @param payOrderId 关联订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReferralReward(Long referredId, Long tierId, BigDecimal payAmount, Long payOrderId) {
        // 1. 获取被推荐用户信息，通过 referrerCode 找到推荐链
        YyUser referredUser = yyUserMapper.selectYyUserByUserId(referredId);
        if (referredUser == null || referredUser.getReferrerCode() == null || referredUser.getReferrerCode().isEmpty()) {
            log.info("用户 {} 没有推荐人，跳过推荐奖励", referredId);
            return;
        }

        // 2. 查找当前被推荐用户的推荐配置（按 tierId 精确匹配，若无则用全等级通用配置）
        YyReferralConfig config = findActiveConfig(tierId);
        if (config == null) {
            log.info("套餐 {} 没有生效的推荐奖励配置，跳过", tierId);
            return;
        }

        // 3. 沿推荐链向上追溯（直推 + 间推），最多追溯 10 层
        String currentReferrerCode = referredUser.getReferrerCode();
        int depth = 0;
        int maxDepth = 10;

        while (currentReferrerCode != null && !currentReferrerCode.isEmpty() && depth < maxDepth) {
            depth++;

            // 通过 inviteCode 查找推荐人
            YyUser referrer = yyUserMapper.selectYyUserByInviteCode(currentReferrerCode);
            if (referrer == null) {
                log.warn("推荐码 {} 找不到用户，推荐链终止", currentReferrerCode);
                break;
            }

            // 根据深度决定使用直推还是间推配置
            BigDecimal rewardAmount;
            if (depth == 1) {
                // 直推奖励
                rewardAmount = calculateReward(payAmount, config.getRewardType(), config.getDirectReward());
            } else {
                // 间推奖励
                rewardAmount = calculateReward(payAmount, config.getRewardType(), config.getIndirectReward());
            }

            // 奖励为 0 时跳过（比如间推配置为 0 但该层没有配置 override）
            if (rewardAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("深度 {} 奖励为 0，跳过推荐人 {}", depth, referrer.getUserId());
            } else {
                // 幂等检查：同一订单 + 同一推荐人 + 同一深度 不重复发
                int exists = yyReferralRewardMapper.countDuplicateReward(
                    referrer.getUserId(), referredId, depth, payOrderId);
                if (exists > 0) {
                    log.info("推荐奖励已存在，幂等跳过: referrer={}, referred={}, depth={}, orderId={}",
                        referrer.getUserId(), referredId, depth, payOrderId);
                } else {
                    // 创建奖励记录
                    YyReferralReward reward = new YyReferralReward();
                    reward.setReferrerId(referrer.getUserId());
                    reward.setReferredId(referredId);
                    reward.setReferralDepth(depth);
                    reward.setTriggerType(determineTriggerType(referredUser));
                    reward.setTriggerAmount(payAmount);
                    reward.setRewardAmount(rewardAmount);
                    reward.setCalculatedBy(config.getRewardType());
                    reward.setConfigId(config.getId());
                    reward.setPayOrderId(payOrderId);
                    reward.setStatus(0); // 待发放
                    reward.setCreateTime(DateUtils.getNowDate());

                    yyReferralRewardMapper.insertYyReferralReward(reward);
                    log.info("推荐奖励创建成功: depth={}, referrer={}, referred={}, amount={}",
                        depth, referrer.getUserId(), referredId, rewardAmount);

                    // 立即为用户余额加款（status=1 已发放）
                    yyUserService.incrementUserBalance(referrer.getUserId(), rewardAmount, 
                        String.format("推荐奖励-%s(%s)", depth == 1 ? "直推" : "间推", referredId));
                    reward.setStatus(1);
                    yyReferralRewardMapper.updateYyReferralReward(reward);
                }
            }

            // 继续追查上一层推荐人
            currentReferrerCode = referrer.getReferrerCode();
        }

        log.info("推荐奖励处理完成，追溯 {} 层，用户 {}", depth, referredId);
    }

    /**
     * 计算奖励金额
     * 
     * @param payAmount 支付金额
     * @param rewardType 配置类型：1=固定金额 2=百分比
     * @param rewardValue 配置值
     * @return 计算后的奖励金额
     */
    private BigDecimal calculateReward(BigDecimal payAmount, Integer rewardType, BigDecimal rewardValue) {
        if (rewardValue == null || payAmount == null) {
            return BigDecimal.ZERO;
        }
        if (rewardValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (rewardType != null && rewardType == 2) {
            // 百分比模式：奖励 = 支付金额 * 百分比 / 100
            return payAmount.multiply(rewardValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            // 固定金额模式
            return rewardValue;
        }
    }

    /**
     * 判断触发类型：首次购买 vs 续费
     */
    private Integer determineTriggerType(YyUser user) {
        // 如果用户之前没有会员等级，或已过期，视为"首次购买"（1=充值）
        // 如果有有效会员且在续订，视为"续费"（2=续费）
        if (user.getMemberLevel() == null || user.getMemberLevel() == 0) {
            return 1;
        }
        if (user.getMemberExpireTime() != null && user.getMemberExpireTime().before(new java.util.Date())) {
            return 1; // 过期后重新购买
        }
        return 2; // 续费
    }

    /**
     * 查找生效的推荐配置
     */
    private YyReferralConfig findActiveConfig(Long tierId) {
        // 优先查找针对该套餐的专属配置
        if (tierId != null) {
            YyReferralConfig specific = yyReferralConfigMapper.selectActiveTierConfig(tierId);
            if (specific != null) {
                return specific;
            }
        }
        // 其次查找全等级通用配置（tierId IS NULL）
        return yyReferralConfigMapper.selectActiveGlobalConfig();
    }

    /**
     * 查询推荐奖励记录列表
     */
    @Override
    public List<YyReferralReward> selectYyReferralRewardList(YyReferralReward reward) {
        return yyReferralRewardMapper.selectYyReferralRewardList(reward);
    }

    /**
     * 根据ID查询推荐奖励记录
     */
    @Override
    public YyReferralReward selectYyReferralRewardById(Long id) {
        return yyReferralRewardMapper.selectYyReferralRewardById(id);
    }

    @Override
    public int deleteYyReferralRewardById(Long id) {
        return yyReferralRewardMapper.deleteYyReferralRewardById(id);
    }

    @Override
    public int deleteYyReferralRewardByIds(Long[] ids) {
        return yyReferralRewardMapper.deleteYyReferralRewardByIds(ids);
    }
}
