package com.ruoyi.yy.service;

import com.ruoyi.yy.dto.YyDataIngestDTO;
import java.util.Map;

/**
 * 数据接入服务接口（V2 管道式架构）
 *
 * <p>替代 {@link IDataFusionService} 的新入口，将解密、映射、融合、快照、
 * 价格历史等步骤委托给独立管道服务。</p>
 *
 * @author fdAgent
 * @date 2026-05-06
 */
public interface IDataIngestService {

    /**
     * 处理数据接入（解密 + 映射 + 融合 + 入库 + 价格历史）
     *
     * @param dto 数据接入请求
     * @return 处理结果统计（total, groupCount, newGroups, message 等）
     */
    Map<String, Object> ingest(YyDataIngestDTO dto);

    /**
     * 仅做字段映射（不解密、不入库），用于解密后预览映射结果
     *
     * @param platformCode  平台代码
     * @param decryptedJson 已解密的 JSON 字符串
     * @param entryPath     数据入口路径，如 "data.wholesales"，空则自动检测
     * @return 包含 products(标准商品列表)、mappedCount、hitFields 等统计信息
     */
    Map<String, Object> mapOnly(String platformCode, String decryptedJson, String entryPath);
}
