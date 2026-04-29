package com.ruoyi.yy.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class YyDataDecryPtDTO {
    private String platformCode;
    private String encryptData;
    private Integer dataEncryptType; // 1: AES, 2: RSA, 3: SM4, 4: SM2

    /** API 编码（如 search, hot, flash_kill），用于自动查找对应的数据入口路径 */
    private String apiCode;

    /** 原始 JSON 数据（未加密），传此字段时跳过解密 */
    private String rawData;

    /** 数据入口路径，如 "data.wholesales"，指定从哪层取商品数组 */
    private String entryPath;

    /**
     * 完整的平台响应数据（JSON 字符串）
     * 后端会根据 encryptDataPath 配置提取加密数据
     * 示例：{code:"40001", data:{i:true, o:"加密数据"}, message: ""}
     */
    private String responseData;

    /**
     * 加密数据路径（可选，优先级高于数据库配置）
     * 示例："data.o" 表示从 responseData.data.o 提取加密数据
     * 注意：与 entryPath（数据入口路径）不同
     */
    private String encryptDataPath;

    /**
     * 数据来源标识
     * 0 或 null: 用户前端（爬虫端）- 不返回原始数据，性能优化
     * 1: 服务后台 - 返回原始数据，用于配置对比
     */
    private Integer dataSource;

    public String getPlatformCode() {
        return platformCode;
    }
    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }
    public YyDataDecryPtDTO(String platformCode, String encryptData, Integer dataEncryptType) {
        this.platformCode = platformCode;
        this.encryptData = encryptData;
        this.dataEncryptType = dataEncryptType;
    }

    public String getEncryptData() {
        return encryptData;
    }
    public void setEncryptData(String encryptData) {
        this.encryptData = encryptData;
    }
  
    public Integer getDataEncryptType() {
        return dataEncryptType;
    }
    public void setDataEncryptType(Integer dataEncryptType) {
        this.dataEncryptType = dataEncryptType;
    }

    public String getApiCode() { return apiCode; }
    public void setApiCode(String apiCode) { this.apiCode = apiCode; }

    public String getRawData() {
        return rawData;
    }
    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    /** 是否为原始数据模式（跳过解密） */
    public boolean isRawMode() {
        return rawData != null && !rawData.isEmpty();
    }

    public String getEntryPath() {
        return entryPath;
    }
    public void setEntryPath(String entryPath) {
        this.entryPath = entryPath;
    }

    public String getResponseData() {
        return responseData;
    }
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getEncryptDataPath() {
        return encryptDataPath;
    }
    public void setEncryptDataPath(String encryptDataPath) {
        this.encryptDataPath = encryptDataPath;
    }

    public Integer getDataSource() {
        return dataSource;
    }
    public void setDataSource(Integer dataSource) {
        this.dataSource = dataSource;
    }
}
