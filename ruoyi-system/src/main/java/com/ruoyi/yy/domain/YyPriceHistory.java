package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格历史 yy_price_history
 */
public class YyPriceHistory {

    private Long id;
    private String sourcePlatform;
    private String skuId;
    private String productName;
    private String specification;
    private String manufacturer;
    private BigDecimal priceCurrent;
    private BigDecimal priceRetail;
    private BigDecimal priceAssemble;
    private Integer stockQuantity;
    private BigDecimal freightAmount;
    private String shopName;
    private LocalDateTime collectedAt;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getSourcePlatform() {
        return sourcePlatform;
    }
    public void setSourcePlatform(String sourcePlatform) {
        this.sourcePlatform = sourcePlatform;
    }
    public String getSkuId() {
        return skuId;
    }
    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getSpecification() {
        return specification;
    }
    public void setSpecification(String specification) {
        this.specification = specification;
    }
    public String getManufacturer() {
        return manufacturer;
    }
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    public BigDecimal getPriceCurrent() {
        return priceCurrent;
    }
    public void setPriceCurrent(BigDecimal priceCurrent) {
        this.priceCurrent = priceCurrent;
    }
    public BigDecimal getPriceRetail() {
        return priceRetail;
    }
    public void setPriceRetail(BigDecimal priceRetail) {
        this.priceRetail = priceRetail;
    }
    public BigDecimal getPriceAssemble() {
        return priceAssemble;
    }
    public void setPriceAssemble(BigDecimal priceAssemble) {
        this.priceAssemble = priceAssemble;
    }
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    public BigDecimal getFreightAmount() {
        return freightAmount;
    }
    public void setFreightAmount(BigDecimal freightAmount) {
        this.freightAmount = freightAmount;
    }
    public String getShopName() {
        return shopName;
    }
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }
    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
