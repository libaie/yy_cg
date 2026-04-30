package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 平台商品快照 yy_product_snapshot
 */
public class YyProductSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sourcePlatform;
    private String skuId;
    private String productId;
    private String sourceApi;

    // 融合关联
    private Long drugId;
    private BigDecimal fusionConfidence;

    // 索引/查询字段
    private String commonName;
    private String barcode;
    private String approvalNumber;
    private String manufacturer;
    private String specification;
    private BigDecimal priceCurrent;
    private Integer stockQuantity;

    // 完整数据
    private String productData;      // JSON string
    private String rawDataPayload;   // JSON string

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSourceApi() { return sourceApi; }
    public void setSourceApi(String sourceApi) { this.sourceApi = sourceApi; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public BigDecimal getFusionConfidence() { return fusionConfidence; }
    public void setFusionConfidence(BigDecimal fusionConfidence) { this.fusionConfidence = fusionConfidence; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public BigDecimal getPriceCurrent() { return priceCurrent; }
    public void setPriceCurrent(BigDecimal priceCurrent) { this.priceCurrent = priceCurrent; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getProductData() { return productData; }
    public void setProductData(String productData) { this.productData = productData; }
    public String getRawDataPayload() { return rawDataPayload; }
    public void setRawDataPayload(String rawDataPayload) { this.rawDataPayload = rawDataPayload; }
    public Date getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Date collectedAt) { this.collectedAt = collectedAt; }
    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }
}
