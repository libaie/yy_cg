package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.List;

/**
 * 平台信息对象 yy_platform
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public class YyPlatform extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 平台主键ID */
    @JsonProperty("pId")
    private Long pId;

    /** 平台名称 */
    @Excel(name = "平台名称")
    private String platformName;

    /** 平台编码 */
    @Excel(name = "平台编码")
    private String platformCode;

    /** 平台Logo图片链接 */
    @Excel(name = "平台Logo图片链接")
    private String platformLogoUrl;

    /** 平台登录跳转链接 */
    @Excel(name = "平台登录跳转链接")
    private String platformLoginUrl;

    /** 平台首页地址 */
    @Excel(name = "平台首页地址")
    private String platformHomeUrl;

    /** Token作用域域名 */
    @Excel(name = "Token域名")
    private String tokenDomain;

    /** Token键名 */
    @Excel(name = "Token键名")
    private String tokenKey;

    /** Token存储类型 */
    @Excel(name = "Token存储类型")
    private String tokenStorageType;

    /** 状态：0禁用 1启用 */
    @Excel(name = "状态", readConverterExp = "0=禁用,1=启用")
    private Integer isActive;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 关联的API配置列表（非数据库字段） */
    private List<YyPlatformApi> apis;

    /** 当前用户的绑定状态（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer bindStatus;

    /** 当前用户的登录状态（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer loginStatus;

    /** 当前用户的平台Token（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    /** 当前用户的Token过期时间（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date tokenExpireTime;

    /** 当前用户的平台账号（脱敏，非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String platformUsername;

    /** 当前用户的平台昵称（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String platformNickname;

    /** 当前用户的绑定时间（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date bindTime;

    /** 当前用户的最后登录时间（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date lastLoginTime;

    /** 当前用户的最后同步Token时间（非数据库字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date lastSyncTime;

    public Long getPId() 
    {
        return pId;
    }

    public void setPId(Long pId) 
    {
        this.pId = pId;
    }

    public String getPlatformName() 
    {
        return platformName;
    }

    public void setPlatformName(String platformName) 
    {
        this.platformName = platformName;
    }

    public String getPlatformCode() 
    {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) 
    {
        this.platformCode = platformCode;
    }

    public String getPlatformLogoUrl() 
    {
        return platformLogoUrl;
    }

    public void setPlatformLogoUrl(String platformLogoUrl) 
    {
        this.platformLogoUrl = platformLogoUrl;
    }

    public String getPlatformLoginUrl() 
    {
        return platformLoginUrl;
    }

    public void setPlatformLoginUrl(String platformLoginUrl) 
    {
        this.platformLoginUrl = platformLoginUrl;
    }

    public String getPlatformHomeUrl() 
    {
        return platformHomeUrl;
    }

    public void setPlatformHomeUrl(String platformHomeUrl) 
    {
        this.platformHomeUrl = platformHomeUrl;
    }

    public String getTokenDomain() 
    {
        return tokenDomain;
    }

    public void setTokenDomain(String tokenDomain) 
    {
        this.tokenDomain = tokenDomain;
    }

    public String getTokenKey() 
    {
        return tokenKey;
    }

    public void setTokenKey(String tokenKey) 
    {
        this.tokenKey = tokenKey;
    }

    public String getTokenStorageType() 
    {
        return tokenStorageType;
    }

    public void setTokenStorageType(String tokenStorageType) 
    {
        this.tokenStorageType = tokenStorageType;
    }

    public Integer getIsActive() 
    {
        return isActive;
    }

    public void setIsActive(Integer isActive) 
    {
        this.isActive = isActive;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public List<YyPlatformApi> getApis() 
    {
        return apis;
    }

    public void setApis(List<YyPlatformApi> apis) 
    {
        this.apis = apis;
    }

    public Integer getBindStatus() { return bindStatus; }
    public void setBindStatus(Integer bindStatus) { this.bindStatus = bindStatus; }
    public Integer getLoginStatus() { return loginStatus; }
    public void setLoginStatus(Integer loginStatus) { this.loginStatus = loginStatus; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public java.util.Date getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(java.util.Date tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }
    public String getPlatformUsername() { return platformUsername; }
    public void setPlatformUsername(String platformUsername) { this.platformUsername = platformUsername; }
    public String getPlatformNickname() { return platformNickname; }
    public void setPlatformNickname(String platformNickname) { this.platformNickname = platformNickname; }
    public java.util.Date getBindTime() { return bindTime; }
    public void setBindTime(java.util.Date bindTime) { this.bindTime = bindTime; }
    public java.util.Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(java.util.Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public java.util.Date getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(java.util.Date lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("pId", getPId())
            .append("platformName", getPlatformName())
            .append("platformCode", getPlatformCode())
            .append("platformLogoUrl", getPlatformLogoUrl())
            .append("platformLoginUrl", getPlatformLoginUrl())
            .append("platformHomeUrl", getPlatformHomeUrl())
            .append("tokenDomain", getTokenDomain())
            .append("tokenKey", getTokenKey())
            .append("tokenStorageType", getTokenStorageType())
            .append("isActive", getIsActive())
            .append("sortOrder", getSortOrder())
            .append("createTime", getCreateTime())
            .toString();
    }
}
