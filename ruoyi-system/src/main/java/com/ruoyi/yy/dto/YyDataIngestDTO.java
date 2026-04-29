package com.ruoyi.yy.dto;

import lombok.Data;

/**
 * 数据接入请求 DTO
 * 插件采集加密数据后，通过此接口提交给后端进行解密、映射、融合
 */
@Data
public class YyDataIngestDTO {
    private String platformCode;       // 平台代码（如 yyjzt, ybm100）
    private String apiCode;            // 来源API: hot/search/flash_kill
    private String encryptData;        // 加密后的原始数据
    private Integer dataEncryptType;   // 加密类型（参考字典 yy_platform_encrypt_type）
    private String apiType;            // API类型标识（如 product_list, product_detail）
    private String collectedAt;        // 插件端采集时间（ISO 8601 格式）
}
