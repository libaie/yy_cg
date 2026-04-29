package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会员套餐配置对象 yy_member_tier
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public class YyMemberTier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long tierId;


    /** 等级名称 */
    @Excel(name = "等级名称")
    private String tierName;

    /** 会员等级 */
    @Excel(name = "会员等级")
    private Integer memberLevel;

    /** 卡片标题 */
    @Excel(name = "卡片标题")
    private String cardTitle;

    /** 卡种 */
    @Excel(name = "卡种")
    private String cardType;

    /** 有效期(天数) */
    @Excel(name = "有效期(天数)")
    private Long durationDays;

    /** 售卖价格 */
    @Excel(name = "售卖价格")
    private BigDecimal price;

    /** 权益描述 */
    @Excel(name = "权益描述")
    private String privileges;

    /** 卡片标签 */
    @Excel(name = "卡片标签")
    private String cardTag;

    /** 限购次数 */
    @Excel(name = "限购次数")
    private Long limitCount;

    /** 是否上架 */
    @Excel(name = "是否上架")
    private Integer isActive;

    public void setTierId(Long tierId) 
    {
        this.tierId = tierId;
    }

    public Long getTierId() 
    {
        return tierId;
    }

    public void setTierName(String tierName) 
    {
        this.tierName = tierName;
    }

    public String getTierName() 
    {
        return tierName;
    }

    public void setCardTitle(String cardTitle) 
    {
        this.cardTitle = cardTitle;
    }

    public String getCardTitle() 
    {
        return cardTitle;
    }

    public void setCardType(String cardType) 
    {
        this.cardType = cardType;
    }

    public String getCardType() 
    {
        return cardType;
    }

    public void setDurationDays(Long durationDays) 
    {
        this.durationDays = durationDays;
    }

    public void setMemberLevel(Integer memberLevel) 
    {
        this.memberLevel = memberLevel;
    }

    public Integer getMemberLevel() 
    {
        return memberLevel;
    }

    public Long getDurationDays() 
    {
        return durationDays;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public void setPrivileges(String privileges) 
    {
        this.privileges = privileges;
    }

    public String getPrivileges() 
    {
        return privileges;
    }

    public void setCardTag(String cardTag) 
    {
        this.cardTag = cardTag;
    }

    public String getCardTag() 
    {
        return cardTag;
    }

    public void setLimitCount(Long limitCount) 
    {
        this.limitCount = limitCount;
    }

    public Long getLimitCount() 
    {
        return limitCount;
    }

    public void setIsActive(Integer isActive) 
    {
        this.isActive = isActive;
    }

    public Integer getIsActive() 
    {
        return isActive;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("tierId", getTierId())
            .append("tierName", getTierName())
            .append("cardTitle", getCardTitle())
            .append("cardType", getCardType())
            .append("durationDays", getDurationDays())
            .append("price", getPrice())
            .append("privileges", getPrivileges())
            .append("cardTag", getCardTag())
            .append("limitCount", getLimitCount())
            .append("isActive", getIsActive())
            .append("createTime", getCreateTime())
            .toString();
    }
}
