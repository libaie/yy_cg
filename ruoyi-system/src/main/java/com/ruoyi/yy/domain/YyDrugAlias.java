package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 药品平台别名映射 yy_drug_alias
 */
public class YyDrugAlias extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long drugId;
    private String platformCode;
    private String platformProductName;
    private String platformManufacturer;
    private String platformSpecification;
    private String platformSkuId;
    private BigDecimal confidence;
    private String matchMethod;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastVerifiedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformProductName() { return platformProductName; }
    public void setPlatformProductName(String platformProductName) { this.platformProductName = platformProductName; }
    public String getPlatformManufacturer() { return platformManufacturer; }
    public void setPlatformManufacturer(String platformManufacturer) { this.platformManufacturer = platformManufacturer; }
    public String getPlatformSpecification() { return platformSpecification; }
    public void setPlatformSpecification(String platformSpecification) { this.platformSpecification = platformSpecification; }
    public String getPlatformSkuId() { return platformSkuId; }
    public void setPlatformSkuId(String platformSkuId) { this.platformSkuId = platformSkuId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }
    public Date getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Date lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
}
