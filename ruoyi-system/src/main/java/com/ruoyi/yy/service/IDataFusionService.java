package com.ruoyi.yy.service;

import com.ruoyi.yy.dto.YyDataIngestDTO;
import java.util.List;
import java.util.Map;

/**
 * 数据融合服务接口
 * 核心职责：接收各平台加密数据 -> 解密 -> 字段映射 -> 融合分组 -> 入库
 */
public interface IDataFusionService {

    /**
     * 处理数据接入（解密 + 映射 + 融合 + 入库）
     * @param dto 数据接入请求
     * @return 处理结果统计
     */
    Map<String, Object> ingest(YyDataIngestDTO dto);

    /**
     * 仅做字段映射（不解密、不入库），用于解密后预览映射结果
     * @param platformCode 平台代码
     * @param decryptedJson 已解密的 JSON 字符串
     * @param entryPath 数据入口路径，如 "data.wholesales"，空则自动检测
     * @return 包含 products(标准商品列表)、mappedCount、hitFields 等统计信息
     */
    Map<String, Object> mapOnly(String platformCode, String decryptedJson, String entryPath);
}
