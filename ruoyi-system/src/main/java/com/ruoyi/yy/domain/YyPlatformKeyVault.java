package com.ruoyi.yy.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 平台密钥金库对象 yy_platform_key_vault
 * 
 * @author ruoyi
 * @date 2026-04-12
 */
public class YyPlatformKeyVault extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 密钥库主键 */
    private Long vaultId;

    /** 所属平台ID (关联 yy_platform) */
    @Excel(name = "所属平台ID (关联 yy_platform)")
    private Long platformId;

    /** 对称密钥 (Key) */
    @Excel(name = "对称密钥 (Key)")
    private String symmetricKey;

    /** 初始化向量 (IV) */
    @Excel(name = "初始化向量 (IV)")
    private String symmetricIv;

    /** RSA公钥 */
    @Excel(name = "RSA公钥")
    private String rsaPublicKey;

    /** RSA私钥 */
    @Excel(name = "RSA私钥")
    private String rsaPrivateKey;

    /** 应用AppKey/ClientId */
    @Excel(name = "应用AppKey/ClientId")
    private String appKey;

    /** 应用密钥AppSecret */
    @Excel(name = "应用密钥AppSecret")
    private String appSecret;

    public void setVaultId(Long vaultId) 
    {
        this.vaultId = vaultId;
    }

    public Long getVaultId() 
    {
        return vaultId;
    }

    public void setPlatformId(Long platformId) 
    {
        this.platformId = platformId;
    }

    public Long getPlatformId() 
    {
        return platformId;
    }

    public void setSymmetricKey(String symmetricKey) 
    {
        this.symmetricKey = symmetricKey;
    }

    public String getSymmetricKey() 
    {
        return symmetricKey;
    }

    public void setSymmetricIv(String symmetricIv) 
    {
        this.symmetricIv = symmetricIv;
    }

    public String getSymmetricIv() 
    {
        return symmetricIv;
    }

    public void setRsaPublicKey(String rsaPublicKey) 
    {
        this.rsaPublicKey = rsaPublicKey;
    }

    public String getRsaPublicKey() 
    {
        return rsaPublicKey;
    }

    public void setRsaPrivateKey(String rsaPrivateKey) 
    {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public String getRsaPrivateKey() 
    {
        return rsaPrivateKey;
    }

    public void setAppKey(String appKey) 
    {
        this.appKey = appKey;
    }

    public String getAppKey() 
    {
        return appKey;
    }

    public void setAppSecret(String appSecret) 
    {
        this.appSecret = appSecret;
    }

    public String getAppSecret() 
    {
        return appSecret;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("vaultId", getVaultId())
            .append("platformId", getPlatformId())
            .append("symmetricKey", getSymmetricKey())
            .append("symmetricIv", getSymmetricIv())
            .append("rsaPublicKey", getRsaPublicKey())
            .append("rsaPrivateKey", getRsaPrivateKey())
            .append("appKey", getAppKey())
            .append("appSecret", getAppSecret())
            .append("remark", getRemark())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
