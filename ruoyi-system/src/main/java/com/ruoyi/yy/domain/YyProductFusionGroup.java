package com.ruoyi.yy.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 商品融合分组对象 yy_product_fusion_group
 */
public class YyProductFusionGroup extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fusionKey;

    @Excel(name = "药品通用名")
    private String commonName;

    @Excel(name = "规格")
    private String specification;

    @Excel(name = "生产厂家")
    private String manufacturer;

    @Excel(name = "批准文号")
    private String approvalNumber;

    @Excel(name = "有货平台数")
    private Integer platformCount;

    @Excel(name = "全平台最低价")
    private BigDecimal minPrice;

    private Long minPricePlatformId;

    @Excel(name = "全平台总库存")
    private Integer totalStock;

    @Excel(name = "状态")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdated;

    // 非数据库字段：平台名称
    private String platformName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFusionKey() { return fusionKey; }
    public void setFusionKey(String fusionKey) { this.fusionKey = fusionKey; }

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }

    public Integer getPlatformCount() { return platformCount; }
    public void setPlatformCount(Integer platformCount) { this.platformCount = platformCount; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public Long getMinPricePlatformId() { return minPricePlatformId; }
    public void setMinPricePlatformId(Long minPricePlatformId) { this.minPricePlatformId = minPricePlatformId; }

    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public void setGenericName(String commonName2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setGenericName'");
    }
}
