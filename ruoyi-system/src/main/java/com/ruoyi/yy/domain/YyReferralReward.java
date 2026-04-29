package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 推荐奖励记录对象 yy_referral_reward
 *
 * @author ruoyi
 * @date 2026-04-05
 */
public class YyReferralReward extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 推荐人用户ID */
    @Excel(name = "推荐人ID")
    private Long referrerId;

    /** 被推荐人用户ID */
    @Excel(name = "被推荐人ID")
    private Long referredId;

    /** 推荐深度：1=直推 2+=间推 */
    @Excel(name = "推荐深度")
    private Integer referralDepth;

    /** 触发类型：1=充值 2=续费 */
    @Excel(name = "触发类型", readConverterExp = "1=充值,2=续费")
    private Integer triggerType;

    /** 触发充值金额 */
    @Excel(name = "订单金额")
    private BigDecimal triggerAmount;

    /** 奖励金额 */
    @Excel(name = "奖励金额")
    private BigDecimal rewardAmount;

    /** 计算方式：1=固定金额 2=百分比 */
    @Excel(name = "计算方式", readConverterExp = "1=固定金额,2=百分比")
    private Integer calculatedBy;

    /** 状态：0=待发放 1=已发放 2=已取消 */
    @Excel(name = "状态", readConverterExp = "0=待发放,1=已发放,2=已取消")
    private Integer status;

    /** 使用的配置ID */
    private Long configId;

    /** 关联订单ID */
    private Long payOrderId;

    /** 推荐人手机号（非数据库字段，关联查询） */
    private String referrerPhone;

    /** 被推荐人手机号（非数据库字段，关联查询） */
    private String referredPhone;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setReferrerId(Long referrerId) { this.referrerId = referrerId; }
    public Long getReferrerId() { return referrerId; }

    public void setReferredId(Long referredId) { this.referredId = referredId; }
    public Long getReferredId() { return referredId; }

    public void setReferralDepth(Integer referralDepth) { this.referralDepth = referralDepth; }
    public Integer getReferralDepth() { return referralDepth; }

    public void setTriggerType(Integer triggerType) { this.triggerType = triggerType; }
    public Integer getTriggerType() { return triggerType; }

    public void setTriggerAmount(BigDecimal triggerAmount) { this.triggerAmount = triggerAmount; }
    public BigDecimal getTriggerAmount() { return triggerAmount; }

    public void setRewardAmount(BigDecimal rewardAmount) { this.rewardAmount = rewardAmount; }
    public BigDecimal getRewardAmount() { return rewardAmount; }

    public void setCalculatedBy(Integer calculatedBy) { this.calculatedBy = calculatedBy; }
    public Integer getCalculatedBy() { return calculatedBy; }

    public void setStatus(Integer status) { this.status = status; }
    public Integer getStatus() { return status; }

    public void setConfigId(Long configId) { this.configId = configId; }
    public Long getConfigId() { return configId; }

    public void setPayOrderId(Long payOrderId) { this.payOrderId = payOrderId; }
    public Long getPayOrderId() { return payOrderId; }

    public void setReferrerPhone(String referrerPhone) { this.referrerPhone = referrerPhone; }
    public String getReferrerPhone() { return referrerPhone; }

    public void setReferredPhone(String referredPhone) { this.referredPhone = referredPhone; }
    public String getReferredPhone() { return referredPhone; }
}
