package com.ruoyi.yy.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会员订阅订单对象 yy_member_subscription
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public class YyMemberSubscription extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订阅记录ID */
    private Long subId;

    /** 关联yy_user表ID */
    @Excel(name = "关联yy_user表ID")
    private Long userId;

    /** 关联yy_member_tier表ID */
    @Excel(name = "关联yy_member_tier表ID")
    private Long tierId;

    /** 会员开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "会员开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 会员到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "会员到期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 支付状态(0未支付, 1已支付, 2已取消, 3支付中) */
    @Excel(name = "支付状态(0未支付, 1已支付, 2已取消, 3支付中)")
    private Integer payStatus;

    /** 支付超时时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "支付超时时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date payExpireTime;

    /** 系统订单号 */
    @Excel(name = "系统订单号")
    private String orderNo;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 第三方支付流水号 */
    @Excel(name = "第三方支付流水号")
    private String transactionId;

    /** 用户信息 */
    @JsonFormat
    private YyUser user;

    /** 套餐信息 */
    @JsonFormat
    private YyMemberTier tier;

    public void setSubId(Long subId) 
    {
        this.subId = subId;
    }

    public Long getSubId() 
    {
        return subId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTierId(Long tierId) 
    {
        this.tierId = tierId;
    }

    public Long getTierId() 
    {
        return tierId;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setPayStatus(Integer payStatus) 
    {
        this.payStatus = payStatus;
    }

    public Integer getPayStatus() 
    {
        return payStatus;
    }

    public void setPayExpireTime(Date payExpireTime) 
    {
        this.payExpireTime = payExpireTime;
    }

    public Date getPayExpireTime() 
    {
        return payExpireTime;
    }

    public void setPayTime(Date payTime) 
    {
        this.payTime = payTime;
    }

    public Date getPayTime() 
    {
        return payTime;
    }

    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }

    public void setTransactionId(String transactionId) 
    {
        this.transactionId = transactionId;
    }

    public String getTransactionId() 
    {
        return transactionId;
    }

    public YyUser getUser() {
        return user;
    }

    public void setUser(YyUser user) {
        this.user = user;
    }

    public YyMemberTier getTier() {
        return tier;
    }

    public void setTier(YyMemberTier tier) {
        this.tier = tier;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("subId", getSubId())
            .append("userId", getUserId())
            .append("tierId", getTierId())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("payStatus", getPayStatus())
            .append("orderNo", getOrderNo())
            .append("transactionId", getTransactionId())
            .append("payExpireTime", getPayExpireTime())
            .append("payTime", getPayTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
