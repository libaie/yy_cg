package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyFieldMappingMapper;
import com.ruoyi.yy.mapper.YyPlatformMapper;
import com.ruoyi.yy.service.IYyPlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 通用平台适配器 — 行为由DB配置驱动
 *
 * 通过yy_field_mapping表的映射规则，将不同平台的字段标准化为统一格式。
 */
@Component
public class YyConfigurablePlatformAdapter implements IYyPlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(YyConfigurablePlatformAdapter.class);

    @Autowired
    private YyFieldMappingMapper fieldMappingMapper;

    @Autowired
    private YyPlatformMapper platformMapper;

    @Override
    public String getPlatformCode() {
        return "*";
    }

    @Override
    public String decrypt(String encryptedData, String platformKey, int encryptType) {
        if (encryptType == 0) {
            return encryptedData;
        }
        if (encryptType == 1) {
            try {
                return decryptAES(encryptedData, platformKey);
            } catch (Exception e) {
                throw new RuntimeException("AES decryption failed, refusing to return ciphertext", e);
            }
        }
        throw new UnsupportedOperationException("Unsupported encryptType: " + encryptType);
    }

    @Override
    public JSONArray extractProductArray(String decryptedJson, String entryPath) {
        if (entryPath == null || entryPath.isEmpty()) {
            return JSONArray.parseArray(decryptedJson);
        }
        JSONObject current = JSONObject.parseObject(decryptedJson);
        String[] parts = entryPath.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1) {
                return current.getJSONArray(parts[i]);
            }
            current = current.getJSONObject(parts[i]);
            if (current == null) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    @Override
    public YyProductSnapshot normalizeProduct(JSONObject rawItem, String platformCode, String apiCode) {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform(platformCode);
        snapshot.setSourceApi(apiCode);

        // 通过platformCode获取platformId
        YyPlatform platform = platformMapper.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            log.warn("Platform not found for code={}", platformCode);
            snapshot.setProductData(rawItem.toJSONString());
            snapshot.setCollectedAt(new Date());
            return snapshot;
        }

        // 加载该平台的字段映射配置
        List<Map<String, String>> mappings = fieldMappingMapper.selectMappingsByPlatformId(platform.getPId());

        for (Map<String, String> mapping : mappings) {
            String platformField = mapping.get("platform_field");
            String standardField = mapping.get("standard_field");
            Object rawValue = rawItem.get(platformField);

            if (rawValue == null) continue;

            setSnapshotField(snapshot, standardField, rawValue.toString());
        }

        snapshot.setRawDataPayload(rawItem.toJSONString());
        snapshot.setProductData(rawItem.toJSONString());
        snapshot.setCollectedAt(new Date());

        return snapshot;
    }

    @Override
    public List<String> buildSearchKeywords(String drugName) {
        List<String> keywords = new ArrayList<>();
        if (drugName == null || drugName.trim().isEmpty()) {
            return keywords;
        }
        keywords.add(drugName.trim());
        String cleaned = drugName.replaceAll("[（(][^）)]*[）)]", "").trim();
        if (!cleaned.equals(drugName.trim())) {
            keywords.add(cleaned);
        }
        return keywords;
    }

    private void setSnapshotField(YyProductSnapshot snapshot, String field, String value) {
        switch (field) {
            case "sku_id": snapshot.setSkuId(value); break;
            case "product_id": snapshot.setProductId(value); break;
            case "common_name": snapshot.setCommonName(value); break;
            case "barcode": snapshot.setBarcode(value); break;
            case "approval_number": snapshot.setApprovalNumber(value); break;
            case "manufacturer": snapshot.setManufacturer(value); break;
            case "specification": snapshot.setSpecification(value); break;
            case "price_current":
                try {
                    snapshot.setPriceCurrent(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    log.warn("Invalid price_current value '{}' for sku={}", value, snapshot.getSkuId());
                }
                break;
            case "stock_quantity":
                try {
                    snapshot.setStockQuantity(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    log.warn("Invalid stock_quantity value '{}' for sku={}", value, snapshot.getSkuId());
                }
                break;
            default:
                log.debug("Unmapped field: {}", field);
        }
    }

    private String decryptAES(String data, String key) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(
            key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, spec);
        byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(data));
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }
}
