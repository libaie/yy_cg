package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 推荐奖励配置对象 yy_referral_config
 *
 * @author ruoyi
 * @date 2026-04-05
 */
public class YyReferralConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联yy_member_tier表的ID，NULL=全等级通用 */
    @Excel(name = "会员等级ID")
    private Long tierId;

    /** 奖励类型：1=固定金额 2=百分比 */
    @Excel(name = "奖励类型", readConverterExp = "1=固定金额,2=百分比")
    private Integer rewardType;

    /** 直推奖励(金额或百分比) */
    @Excel(name = "直推奖励")
    private BigDecimal directReward;

    /** 间推奖励(金额或百分比) */
    @Excel(name = "间推奖励")
    private BigDecimal indirectReward;

    /** 0=禁用 1=启用 */
    @Excel(name = "状态", readConverterExp = "0=禁用,1=启用")
    private Integer isActive;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setTierId(Long tierId) { this.tierId = tierId; }
    public Long getTierId() { return tierId; }

    public void setRewardType(Integer rewardType) { this.rewardType = rewardType; }
    public Integer getRewardType() { return rewardType; }

    public void setDirectReward(BigDecimal directReward) { this.directReward = directReward; }
    public BigDecimal getDirectReward() { return directReward; }

    public void setIndirectReward(BigDecimal indirectReward) { this.indirectReward = indirectReward; }
    public BigDecimal getIndirectReward() { return indirectReward; }

    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public Integer getIsActive() { return isActive; }
}
