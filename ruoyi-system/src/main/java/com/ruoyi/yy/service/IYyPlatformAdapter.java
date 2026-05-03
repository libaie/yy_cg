package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyProductSnapshot;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 平台适配器接口 — 每个B2B平台一个实现
 */
public interface IYyPlatformAdapter {

    /**
     * 平台编码（与yy_platform.platform_code一致）
     */
    String getPlatformCode();

    /**
     * 解密平台返回的数据
     */
    String decrypt(String encryptedData, String platformKey, int encryptType);

    /**
     * 从解密后的JSON中提取商品数组
     */
    JSONArray extractProductArray(String decryptedJson, String entryPath);

    /**
     * 将平台原始商品数据标准化为YyProductSnapshot
     */
    YyProductSnapshot normalizeProduct(JSONObject rawItem, String platformCode, String apiCode);

    /**
     * 构建搜索关键词列表
     */
    java.util.List<String> buildSearchKeywords(String drugName);
}
