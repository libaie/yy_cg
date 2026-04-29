package com.ruoyi.yy.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户平台关联对象 yy_user_platform
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
public class YyUserPlatform extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 平台ID */
    private Long platformId;

    /** 绑定状态：0未绑定 1已绑定 */
    @Excel(name = "绑定状态")
    private Integer bindStatus;

    /** 登录状态：0离线 1在线 */
    @Excel(name = "登录状态")
    private Integer loginStatus;

    /** 平台Token */
    @Excel(name = "平台Token")
    private String token;

    /** Token过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "Token过期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date tokenExpireTime;

    /** 平台账号（脱敏） */
    @Excel(name = "平台账号")
    private String platformUsername;

    /** 平台昵称 */
    @Excel(name = "平台昵称")
    private String platformNickname;

    /** 绑定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "绑定时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date bindTime;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 最后同步Token时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后同步时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    /** 关联的用户信息（非数据库字段） */
    private YyUser user;

    /** 关联的平台信息（非数据库字段） */
    private YyPlatform platform;

    /** 绑定系统的用户昵称（非数据库字段，SQL关联） */
    private String userNickName;

    /** 绑定系统的用户手机号（非数据库字段，SQL关联） */
    private String userPhone;

    public YyUser getUser() { return user; }
    public void setUser(YyUser user) { this.user = user; }

    public YyPlatform getPlatform() { return platform; }
    public void setPlatform(YyPlatform platform) { this.platform = platform; }

    public String getUserNickName() { return userNickName; }
    public void setUserNickName(String userNickName) { this.userNickName = userNickName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPlatformId() { return platformId; }
    public void setPlatformId(Long platformId) { this.platformId = platformId; }

    public Integer getBindStatus() { return bindStatus; }
    public void setBindStatus(Integer bindStatus) { this.bindStatus = bindStatus; }

    public Integer getLoginStatus() { return loginStatus; }
    public void setLoginStatus(Integer loginStatus) { this.loginStatus = loginStatus; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Date getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(Date tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }

    public String getPlatformUsername() { return platformUsername; }
    public void setPlatformUsername(String platformUsername) { this.platformUsername = platformUsername; }

    public String getPlatformNickname() { return platformNickname; }
    public void setPlatformNickname(String platformNickname) { this.platformNickname = platformNickname; }

    public Date getBindTime() { return bindTime; }
    public void setBindTime(Date bindTime) { this.bindTime = bindTime; }

    public Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Date getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Date lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("platformId", getPlatformId())
            .append("bindStatus", getBindStatus())
            .append("loginStatus", getLoginStatus())
            .append("bindTime", getBindTime())
            .append("lastLoginTime", getLastLoginTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
