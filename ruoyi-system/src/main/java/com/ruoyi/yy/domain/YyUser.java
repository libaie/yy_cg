package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户对象 yy_user
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public class YyUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long userId;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 密码 */
    private String password;

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickName;

    /** 头像链接 */
    @Excel(name = "头像链接")
    private String avatar;

    /** 会员等级(0:普通, 1:黄金, 2:铂金, 3:钻石) */
    @Excel(name = "会员等级(0:普通, 1:黄金, 2:铂金, 3:钻石)")
    private Long memberLevel;

    /** 关联yy_member_tier表的ID */
    @Excel(name = "关联yy_member_tier表的ID")
    private Long memberLevelId;

    /** 会员到期时间(过期逻辑以此字段为准) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "会员到期时间(过期逻辑以此字段为准)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date memberExpireTime;

    /** 首次成为会员的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "首次成为会员的时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date firstMemberTime;

    /** 账户余额 */
    @Excel(name = "账户余额")
    private BigDecimal balance;

    /** 专属Chatbot链接 */
    @Excel(name = "专属Chatbot链接")
    private String chatbotUrl;

    /** 邀请码 */
    @Excel(name = "邀请码")
    private String inviteCode;

    /** 推荐人邀请码 */
    @Excel(name = "推荐人邀请码")
    private String referrerCode;

    /** 用户注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "用户注册时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date regTime;

    /** 最近一次支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最近一次支付时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastPayTime;

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }

    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }

    public void setNickName(String nickName) 
    {
        this.nickName = nickName;
    }

    public String getNickName() 
    {
        return nickName;
    }

    public void setAvatar(String avatar) 
    {
        this.avatar = avatar;
    }

    public String getAvatar() 
    {
        return avatar;
    }

    public void setMemberLevel(Long memberLevel) 
    {
        this.memberLevel = memberLevel;
    }

    public Long getMemberLevel() 
    {
        return memberLevel;
    }

    public void setMemberLevelId(Long memberLevelId) 
    {
        this.memberLevelId = memberLevelId;
    }

    public Long getMemberLevelId() 
    {
        return memberLevelId;
    }

    public void setMemberExpireTime(Date memberExpireTime) 
    {
        this.memberExpireTime = memberExpireTime;
    }

    public Date getMemberExpireTime() 
    {
        return memberExpireTime;
    }

    public void setFirstMemberTime(Date firstMemberTime) 
    {
        this.firstMemberTime = firstMemberTime;
    }

    public Date getFirstMemberTime() 
    {
        return firstMemberTime;
    }

    public void setBalance(BigDecimal balance) 
    {
        this.balance = balance;
    }

    public BigDecimal getBalance() 
    {
        return balance;
    }

    public void setChatbotUrl(String chatbotUrl) 
    {
        this.chatbotUrl = chatbotUrl;
    }

    public String getChatbotUrl() 
    {
        return chatbotUrl;
    }

    public void setInviteCode(String inviteCode) 
    {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode() 
    {
        return inviteCode;
    }

    public void setReferrerCode(String referrerCode) 
    {
        this.referrerCode = referrerCode;
    }

    public String getReferrerCode() 
    {
        return referrerCode;
    }

    public void setRegTime(Date regTime) 
    {
        this.regTime = regTime;
    }

    public Date getRegTime() 
    {
        return regTime;
    }

    public void setLastPayTime(Date lastPayTime)
    {
        this.lastPayTime = lastPayTime;
    }

    public Date getLastPayTime()
    {
        return lastPayTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("phone", getPhone())
            .append("nickName", getNickName())
            .append("avatar", getAvatar())
            .append("memberLevel", getMemberLevel())
            .append("memberLevelId", getMemberLevelId())
            .append("memberExpireTime", getMemberExpireTime())
            .append("firstMemberTime", getFirstMemberTime())
            .append("balance", getBalance())
            .append("chatbotUrl", getChatbotUrl())
            .append("inviteCode", getInviteCode())
            .append("referrerCode", getReferrerCode())
            .append("regTime", getRegTime())
            .append("lastPayTime", getLastPayTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
