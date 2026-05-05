package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyPriceHistory;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyPriceHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 价格快照服务实现
 *
 * 负责产品快照的持久化，并在每次快照写入后双写至价格历史时间序列表。
 */
@Service
public class YyPriceSnapshotServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(YyPriceSnapshotServiceImpl.class);

    @Autowired(required = false)
    private YyPriceHistoryMapper priceHistoryMapper;

    /**
     * 从快照双写价格历史记录
     *
     * 将 YyProductSnapshot 中已有的字段直接映射到 YyPriceHistory，
     * 对于快照中不存在的字段（price_retail, price_assemble, freight_amount, shop_name），
     * 尝试从 productData JSON 中提取，提取失败则置为 null。
     *
     * 历史写入失败不会阻断主流程（降级跳过）。
     */
    public void appendPriceHistory(YyProductSnapshot snapshot) {
        if (priceHistoryMapper == null) {
            return;
        }
        try {
            YyPriceHistory history = new YyPriceHistory();
            history.setSourcePlatform(snapshot.getSourcePlatform());
            history.setSkuId(snapshot.getSkuId());
            history.setProductName(snapshot.getCommonName());
            history.setSpecification(snapshot.getSpecification());
            history.setManufacturer(snapshot.getManufacturer());
            history.setPriceCurrent(snapshot.getPriceCurrent());
            history.setStockQuantity(snapshot.getStockQuantity());

            // 尝试从 productData JSON 中提取快照未直接携带的字段
            extractExtraFields(snapshot, history);

            // Date -> LocalDateTime
            Date collectedAt = snapshot.getCollectedAt();
            if (collectedAt != null) {
                history.setCollectedAt(collectedAt.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            } else {
                history.setCollectedAt(LocalDateTime.now());
            }

            priceHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("[PriceHistory] 写入失败，降级跳过: sku={}", snapshot.getSkuId(), e);
        }
    }

    /**
     * 从 productData JSON 中提取快照未直接携带的字段
     */
    private void extractExtraFields(YyProductSnapshot snapshot, YyPriceHistory history) {
        String raw = snapshot.getProductData();
        if (raw == null || raw.isEmpty() || "{}".equals(raw)) {
            return;
        }
        try {
            JSONObject data = JSONObject.parseObject(raw);

            // 尝试多个可能的字段名
            history.setShopName(getStringField(data, "shop_name", "shopName", "store_name"));
            history.setFreightAmount(getBigDecimalField(data, "freight_amount", "freightAmount", "freight"));
            history.setPriceRetail(getBigDecimalField(data, "price_retail", "priceRetail", "retail_price"));
            history.setPriceAssemble(getBigDecimalField(data, "price_assemble", "priceAssemble", "assemble_price", "group_price"));

            // productName 优先用 commonName，但如果有 product_name 字段则覆盖
            String productName = getStringField(data, "product_name", "productName", "name", "title");
            if (productName != null && !productName.isEmpty()) {
                history.setProductName(productName);
            }
        } catch (Exception e) {
            log.debug("[PriceHistory] 解析 productData 失败，使用默认字段: sku={}", snapshot.getSkuId());
        }
    }

    private String getStringField(JSONObject data, String... candidates) {
        for (String key : candidates) {
            String val = data.getString(key);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }
        return null;
    }

    private BigDecimal getBigDecimalField(JSONObject data, String... candidates) {
        for (String key : candidates) {
            Object val = data.get(key);
            if (val == null) {
                continue;
            }
            if (val instanceof BigDecimal) {
                return (BigDecimal) val;
            }
            try {
                return new BigDecimal(val.toString());
            } catch (NumberFormatException ignored) {
                // try next candidate
            }
        }
        return null;
    }
}
