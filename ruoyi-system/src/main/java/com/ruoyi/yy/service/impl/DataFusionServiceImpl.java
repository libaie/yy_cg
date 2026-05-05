package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.decrypt.DataDecryptUtil;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.dto.YyDataIngestDTO;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import com.ruoyi.yy.service.impl.YyPriceSnapshotServiceImpl;
import com.ruoyi.yy.mapper.YyStandardProductMapper;
import com.ruoyi.yy.service.IDataFusionService;
import com.ruoyi.yy.service.IYyFieldMappingService;
import com.ruoyi.yy.service.IYyPlatformKeyVaultService;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.yy.service.IYyProductFusionGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据融合服务实现 v2.0
 * 
 * 核心流程：
 * 1. 根据 platformCode 查找平台 -> 获取 KeyVault -> 解密
 * 2. 加载该平台的字段映射规则
 * 3. 将解密后的 JSON 按映射规则转为标准商品列表
 * 4. 计算 fusion_key，查找/创建融合分组
 * 5. 入库 yy_standard_product（upsert）
 * 6. 更新 yy_product_fusion_group 聚合
 */
@Service
public class DataFusionServiceImpl implements IDataFusionService {

    private static final Logger log = LoggerFactory.getLogger(DataFusionServiceImpl.class);

    @Autowired
    private IYyPlatformService yyPlatformService;

    @Autowired
    private IYyPlatformKeyVaultService yyPlatformKeyVaultService;

    @Autowired
    private IYyFieldMappingService yyFieldMappingService;

    @Autowired
    private YyStandardProductMapper yyStandardProductMapper;

    @Autowired
    private IYyProductFusionGroupService yyProductFusionGroupService;

    @Autowired
    private YyFusionEngineImpl fusionEngine;

    @Autowired
    private YyProductSnapshotMapper productSnapshotMapper;

    @Autowired(required = false)
    private YyPriceSnapshotServiceImpl priceSnapshotService;

    // ========== 标准字段名常量 ==========
    private static final String F_PRODUCT_ID = "product_id";
    private static final String F_SKU_ID = "sku_id";
    private static final String F_SOURCE_PLATFORM = "source_platform";
    private static final String F_RAW_DATA_PAYLOAD = "raw_data_payload";
    private static final String F_BARCODE = "barcode";
    private static final String F_PRODUCT_NAME = "product_name";
    private static final String F_COMMON_NAME = "common_name";
    private static final String F_BRAND_NAME = "brand_name";
    private static final String F_MANUFACTURER = "manufacturer";
    private static final String F_APPROVAL_NUMBER = "approval_number";
    private static final String F_CATEGORY_ID = "category_id";
    private static final String F_CATEGORY_NAME = "category_name";
    private static final String F_SPECIFICATION = "specification";
    private static final String F_UNIT = "unit";
    private static final String F_PACKING_RATIO = "packing_ratio";
    private static final String F_MIN_ORDER_QTY = "min_order_qty";
    private static final String F_MAX_ORDER_QTY = "max_order_qty";
    private static final String F_PRODUCT_STATUS = "product_status";
    private static final String F_STOCK_QUANTITY = "stock_quantity";
    private static final String F_WAREHOUSE_STOCK = "warehouse_stock";
    private static final String F_MAIN_IMAGES = "main_images";
    private static final String F_PRODUCTION_DATE = "production_date";
    private static final String F_EXPIRATION_DATE = "expiration_date";
    private static final String F_SHELF_LIFE = "shelf_life";
    private static final String F_IS_PRESCRIPTION_DRUG = "is_prescription_drug";
    private static final String F_MEDICARE_TYPE = "medicare_type";
    private static final String F_TRACEABILITY_CODE_STATUS = "traceability_code_status";
    private static final String F_SALES_VOLUME = "sales_volume";
    private static final String F_SHOP_NAME = "shop_name";
    private static final String F_PRICE_RETAIL = "price_retail";
    private static final String F_PRICE_CURRENT = "price_current";
    private static final String F_PRICE_STEP_RULES = "price_step_rules";
    private static final String F_PRICE_ASSEMBLE = "price_assemble";
    private static final String F_IS_TAX_INCLUDED = "is_tax_included";
    private static final String F_FREIGHT_AMOUNT = "freight_amount";
    private static final String F_FREE_SHIPPING_THRESHOLD = "free_shipping_threshold";
    private static final String F_TAGS = "tags";
    private static final String F_MARKETING_TAGS = "marketing_tags";
    private static final String F_ACTIVITY_DETAILS = "activity_details";
    private static final String F_PURCHASE_LIMITS = "purchase_limits";

    // 需要转为 BigDecimal 的字段
    private static final Set<String> DECIMAL_FIELDS = new HashSet<>(Arrays.asList(
        F_PRICE_RETAIL, F_PRICE_CURRENT, F_PRICE_ASSEMBLE, F_FREIGHT_AMOUNT, F_FREE_SHIPPING_THRESHOLD
    ));

    // 需要转为 Integer 的字段
    private static final Set<String> INTEGER_FIELDS = new HashSet<>(Arrays.asList(
        F_STOCK_QUANTITY, F_SALES_VOLUME, F_MIN_ORDER_QTY, F_MAX_ORDER_QTY
    ));

    // 需要转为 Boolean (Integer 0/1) 的字段
    private static final Set<String> BOOLEAN_FIELDS = new HashSet<>(Arrays.asList(
        F_IS_PRESCRIPTION_DRUG, F_TRACEABILITY_CODE_STATUS, F_IS_TAX_INCLUDED
    ));

    // JSON 数组字段（存储为 JSON 字符串）
    private static final Set<String> JSON_ARRAY_FIELDS = new HashSet<>(Arrays.asList(
        F_WAREHOUSE_STOCK, F_MAIN_IMAGES, F_PRICE_STEP_RULES, F_TAGS, F_MARKETING_TAGS, F_ACTIVITY_DETAILS
    ));

    // JSON 对象字段
    private static final Set<String> JSON_OBJECT_FIELDS = new HashSet<>(Arrays.asList(
        F_PURCHASE_LIMITS
    ));

    // 需要做日期标准化的字段
    private static final Set<String> DATE_FIELDS = new HashSet<>(Arrays.asList(
        F_PRODUCTION_DATE, F_EXPIRATION_DATE
    ));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ingest(YyDataIngestDTO dto) {
        String platformCode = dto.getPlatformCode();

        try {
            return doIngest(dto, platformCode);
        } catch (Exception e) {
            log.warn("实时数据融合失败 platform={}: {}. 降级回退到历史缓存快照.", platformCode, e.getMessage());
            log.debug("降级详细堆栈", e);
            return degradedFallback(platformCode);
        }
    }

    /**
     * 核心融合处理逻辑（从 ingest 抽离，便于 try-catch 包裹）
     */
    private Map<String, Object> doIngest(YyDataIngestDTO dto, String platformCode) {
        String encryptedData = dto.getEncryptData();
        Integer dataEncryptType = dto.getDataEncryptType();

        // ====== Step 1: 解密 ======
        String decryptedJson = decryptData(platformCode, encryptedData, dataEncryptType);
        if (decryptedJson == null) {
            return buildResult(0, 0, 0, 0, "解密失败");
        }

        // ====== Step 2: 加载映射规则 ======
        YyPlatform platform = yyPlatformService.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            return buildResult(0, 0, 0, 0, "平台不存在: " + platformCode);
        }

        Map<String, String> fieldMappings = yyFieldMappingService.getPlatformMappings(platform.getPId());
        if (fieldMappings.isEmpty()) {
            return buildResult(0, 0, 0, 0, "该平台未配置字段映射规则，请先在映射配置页面设置");
        }

        // ====== Step 3: 解析 JSON 并映射为标准商品 ======
        String entryPath = yyFieldMappingService.getEntryPath(platform.getPId());
        List<YyStandardProduct> products = parseAndMap(decryptedJson, fieldMappings, platform, entryPath);
        if (products.isEmpty()) {
            return buildResult(0, 0, 0, 0, "未解析到商品数据");
        }

        // 标记数据来源 API
        String apiCode = dto.getApiCode();

        // ====== Step 4 & 5 & 6: 融合 + 入库 + 聚合 ======
        int newGroups = 0;
        int updatedGroups = 0;
        Set<Long> touchedGroupIds = new HashSet<>();

        for (YyStandardProduct product : products) {
            // 计算融合键
            String fusionKey = generateFusionKey(product);
            product.setFusionKey(fusionKey);

            // 查找或创建融合分组
            YyProductFusionGroup group = yyProductFusionGroupService.selectByFusionKey(fusionKey);
            boolean isNewGroup = (group == null);
            if (isNewGroup) {
                group = new YyProductFusionGroup();
                group.setFusionKey(fusionKey);
                group.setCommonName(product.getCommonName());
                group.setSpecification(product.getSpecification());
                group.setManufacturer(product.getManufacturer());
                group.setApprovalNumber(product.getApprovalNumber());
                group.setStatus(1);
                yyProductFusionGroupService.insertYyProductFusionGroup(group);
                newGroups++;
            } else {
                updatedGroups++;
            }

            // 关联融合分组
            product.setFusionGroupId(group.getId());

            // 标记来源 API
            if (apiCode != null && !apiCode.isEmpty()) {
                product.setSourceApi(apiCode);
            }

            // 解析采集时间
            if (dto.getCollectedAt() != null && !dto.getCollectedAt().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    product.setCollectedAt(sdf.parse(dto.getCollectedAt()));
                } catch (Exception e) {
                    product.setCollectedAt(new Date());
                }
            } else {
                product.setCollectedAt(new Date());
            }

            // 保留原始数据
            product.setRawDataPayload(buildRawJson(product, decryptedJson));

            // Upsert 入库（source_platform + sku_id 唯一）
            yyStandardProductMapper.upsertYyStandardProduct(product);

            touchedGroupIds.add(group.getId());
        }

        // ====== Step 5b: 创建快照并执行融合匹配 ======
        List<YyProductSnapshot> snapshots = new ArrayList<>();
        for (YyStandardProduct product : products) {
            YyProductSnapshot snapshot = new YyProductSnapshot();
            snapshot.setSourcePlatform(platformCode);
            snapshot.setSkuId(product.getSkuId());
            snapshot.setProductId(product.getProductId());
            snapshot.setSourceApi(apiCode);
            snapshot.setCommonName(product.getCommonName());
            snapshot.setBarcode(product.getBarcode());
            snapshot.setApprovalNumber(product.getApprovalNumber());
            snapshot.setManufacturer(product.getManufacturer());
            snapshot.setSpecification(product.getSpecification());
            snapshot.setPriceCurrent(product.getPriceCurrent());
            snapshot.setStockQuantity(product.getStockQuantity());
            snapshot.setProductData(product.getRawDataPayload() != null ? product.getRawDataPayload() : "{}");
            snapshot.setCollectedAt(product.getCollectedAt() != null ? product.getCollectedAt() : new Date());
            snapshots.add(snapshot);
        }

        // 批量查询已存在的快照（去重）
        List<String> skuIds = snapshots.stream()
            .map(YyProductSnapshot::getSkuId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
        final Set<String> existingSkus;
        if (!skuIds.isEmpty()) {
            List<YyProductSnapshot> existing = productSnapshotMapper
                .selectYyProductSnapshotByPlatformAndSkuIds(platformCode, skuIds);
            existingSkus = existing.stream()
                .map(YyProductSnapshot::getSkuId)
                .collect(java.util.stream.Collectors.toSet());
        } else {
            existingSkus = new HashSet<>();
        }

        // 只插入新快照
        List<YyProductSnapshot> newSnapshots = snapshots.stream()
            .filter(s -> s.getSkuId() == null || !existingSkus.contains(s.getSkuId()))
            .collect(java.util.stream.Collectors.toList());
        if (!newSnapshots.isEmpty()) {
            productSnapshotMapper.batchInsertYyProductSnapshot(newSnapshots);
        }

        // 对每个快照执行融合匹配
        for (YyProductSnapshot snapshot : snapshots) {
            com.ruoyi.yy.domain.YyFusionResult fusionResult = fusionEngine.fuse(snapshot);
            if (fusionResult.isMatched()) {
                snapshot.setDrugId(fusionResult.getDrugId());
                snapshot.setFusionConfidence(fusionResult.getConfidence());
                log.info("Product fused: sku={} → drug_id={} via {} conf={}",
                    snapshot.getSkuId(), fusionResult.getDrugId(),
                    fusionResult.getMatchMethod(), fusionResult.getConfidence());
            } else {
                log.info("Product needs review: sku={}", snapshot.getSkuId());
            }
        }

        // ====== Step 6b: 双写价格历史（每个采集到的快照都写入时间序列） ======
        if (priceSnapshotService != null) {
            for (YyProductSnapshot snapshot : snapshots) {
                priceSnapshotService.appendPriceHistory(snapshot);
            }
        }

        // ====== Step 7: 更新所有受影响的融合分组聚合 ======
        for (Long groupId : touchedGroupIds) {
            updateAggregation(groupId);
        }

        return buildResult(products.size(), touchedGroupIds.size(), newGroups, updatedGroups, "融合成功");
    }

    /**
     * 降级回退：实时融合失败时，查询 yy_product_snapshot 历史缓存作为兜底数据。
     * 返回标记 {@code degraded: true} 的结果，前端可据此展示"数据可能不是最新"的提示。
     */
    private Map<String, Object> degradedFallback(String platformCode) {
        List<YyProductSnapshot> snapshots = productSnapshotMapper.selectLatestByPlatform(platformCode, 100);

        if (snapshots.isEmpty()) {
            return buildResult(0, 0, 0, 0, "平台数据获取失败，且无历史缓存可用");
        }

        // 将快照转为 StandardProduct 列表，标记 degraded=true
        List<YyStandardProduct> degradedProducts = new ArrayList<>();
        for (YyProductSnapshot snapshot : snapshots) {
            YyStandardProduct p = new YyStandardProduct();
            p.setSourcePlatform(snapshot.getSourcePlatform());
            p.setSkuId(snapshot.getSkuId());
            p.setProductId(snapshot.getProductId());
            p.setSourceApi(snapshot.getSourceApi());
            p.setCommonName(snapshot.getCommonName());
            p.setBarcode(snapshot.getBarcode());
            p.setApprovalNumber(snapshot.getApprovalNumber());
            p.setManufacturer(snapshot.getManufacturer());
            p.setSpecification(snapshot.getSpecification());
            p.setPriceCurrent(snapshot.getPriceCurrent());
            p.setStockQuantity(snapshot.getStockQuantity());
            p.setCollectedAt(snapshot.getCollectedAt());
            p.setDegraded(true);
            degradedProducts.add(p);
        }

        Map<String, Object> result = buildResult(degradedProducts.size(), 0, 0, 0,
            "实时采集失败(" + platformCode + ")，已回退到历史缓存数据");

        // 附加降级元信息
        result.put("degraded", true);
        result.put("degradedSource", "yy_product_snapshot");
        result.put("degradedProducts", degradedProducts);

        log.info("降级回退完成 platform={}: 返回 {} 条缓存快照.", platformCode, degradedProducts.size());
        return result;
    }

    @Override
    public Map<String, Object> mapOnly(String platformCode, String decryptedJson, String entryPath) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> mappedList = new ArrayList<>();

        YyPlatform platform = yyPlatformService.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            log.warn("mapOnly: 平台不存在 {}", platformCode);
            result.put("products", mappedList);
            result.put("message", "平台不存在: " + platformCode);
            return result;
        }

        Map<String, String> fieldMappings = yyFieldMappingService.getPlatformMappings(platform.getPId());
        if (fieldMappings.isEmpty()) {
            log.warn("mapOnly: 平台 {} 未配置字段映射", platformCode);
            result.put("products", mappedList);
            result.put("message", "该平台未配置字段映射");
            return result;
        }

        // 如果前端没传 entryPath，从 DB 加载
        if (entryPath == null || entryPath.isEmpty()) {
            entryPath = yyFieldMappingService.getEntryPath(platform.getPId());
        }

        List<YyStandardProduct> products = parseAndMap(decryptedJson, fieldMappings, platform, entryPath);

        // 统计信息
        int totalParsed = 0;
        int discarded = 0;
        Set<String> hitFields = new HashSet<>();

        // 转为 Map 列表，方便前端直接展示
        for (YyStandardProduct p : products) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", p.getProductId());
            map.put("skuId", p.getSkuId());
            map.put("sourceApi", p.getSourceApi());
            map.put("barcode", p.getBarcode());
            map.put("productName", p.getProductName());
            map.put("commonName", p.getCommonName());
            map.put("brandName", p.getBrandName());
            map.put("manufacturer", p.getManufacturer());
            map.put("approvalNumber", p.getApprovalNumber());
            map.put("categoryId", p.getCategoryId());
            map.put("categoryName", p.getCategoryName());
            map.put("specification", p.getSpecification());
            map.put("unit", p.getUnit());
            map.put("packingRatio", p.getPackingRatio());
            map.put("productStatus", p.getProductStatus());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("productionDate", p.getProductionDate());
            map.put("expirationDate", p.getExpirationDate());
            map.put("shelfLife", p.getShelfLife());
            map.put("isPrescriptionDrug", p.getIsPrescriptionDrug());
            map.put("medicareType", p.getMedicareType());
            map.put("traceabilityCodeStatus", p.getTraceabilityCodeStatus());
            map.put("salesVolume", p.getSalesVolume());
            map.put("shopName", p.getShopName());
            map.put("priceRetail", p.getPriceRetail());
            map.put("priceCurrent", p.getPriceCurrent());
            map.put("priceAssemble", p.getPriceAssemble());
            map.put("isTaxIncluded", p.getIsTaxIncluded());
            map.put("freightAmount", p.getFreightAmount());
            map.put("freeShippingThreshold", p.getFreeShippingThreshold());
            map.put("mainImages", p.getMainImages());
            map.put("minOrderQty", p.getMinOrderQty());
            map.put("maxOrderQty", p.getMaxOrderQty());
            map.put("tags", p.getTags());
            map.put("marketingTags", p.getMarketingTags());
            map.put("activityDetails", p.getActivityDetails());
            map.put("purchaseLimits", p.getPurchaseLimits());
            // 统计命中字段
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getValue() != null) {
                    hitFields.add(e.getKey());
                }
            }
            map.values().removeIf(Objects::isNull);
            mappedList.add(map);
        }

        result.put("products", mappedList);
        result.put("mappedCount", mappedList.size());
        result.put("configuredFields", fieldMappings.size());
        result.put("hitFields", hitFields.size());
        result.put("missFields", fieldMappings.size() - hitFields.size());
        result.put("hitFieldNames", hitFields);
        result.put("message", mappedList.isEmpty() ? "未解析到商品数据" : "映射成功");

        return result;
    }

    /**
     * 解密数据（复用现有解密逻辑）
     */
    private String decryptData(String platformCode, String encryptedData, Integer dataEncryptType) {
        try {
            YyPlatform platform = yyPlatformService.selectYyPlatformByCode(platformCode);
            if (platform == null) {
                log.error("平台不存在: {}", platformCode);
                return null;
            }

            YyPlatformKeyVault vault = yyPlatformKeyVaultService.selectYyPlatformKeyVaultByPlatformId(platform.getPId());
            if (vault == null) {
                log.error("密钥配置不存在，平台ID: {}", platform.getPId());
                return null;
            }

            // 查询加密类型字典
            List<com.ruoyi.common.core.domain.entity.SysDictData> encryptTypeList =
                DictUtils.getDictCache("yy_platform_encrypt_type");
            if (encryptTypeList == null || encryptTypeList.isEmpty()) {
                log.error("字典配置不存在: yy_platform_encrypt_type");
                return null;
            }

            Map<String, String> encryptTypeMap = new HashMap<>();
            for (com.ruoyi.common.core.domain.entity.SysDictData d : encryptTypeList) {
                encryptTypeMap.put(d.getDictValue(), d.getDictLabel());
            }

            String encryptTypeLabel = encryptTypeMap.get(String.valueOf(dataEncryptType));
            if (encryptTypeLabel == null) {
                log.error("不支持的加密类型: {}", dataEncryptType);
                return null;
            }

            return decryptByType(encryptedData, encryptTypeLabel, vault);
        } catch (Exception e) {
            log.error("解密异常", e);
            return null;
        }
    }

    /**
     * 根据加密类型解密
     */
    private String decryptByType(String encryptedData, String encryptTypeLabel, YyPlatformKeyVault vault) {
        try {
            switch (encryptTypeLabel) {
                case "AES-128-ECB":
                    return DataDecryptUtil.decryptAES128ECB(encryptedData, vault.getSymmetricKey());
                case "AES-128-CBC":
                    return DataDecryptUtil.decryptAES128CBC(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "AES-128-GCM":
                    return DataDecryptUtil.decryptAES128GCM(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "DES-ECB":
                    return DataDecryptUtil.decryptDES(encryptedData, vault.getSymmetricKey());
                case "RSA-PRIVATE-KEY":
                    return DataDecryptUtil.decryptRSA(encryptedData, vault.getRsaPrivateKey());
                case "RSA-PUBLIC-KEY":
                    return DataDecryptUtil.decryptRSAWithPublicKey(encryptedData, vault.getRsaPublicKey());
                case "SM4-ECB":
                    return DataDecryptUtil.decryptSM4ECB(encryptedData, vault.getSymmetricKey());
                case "SM4-CBC":
                    return DataDecryptUtil.decryptSM4CBC(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "BASE64":
                    byte[] decoded = com.ruoyi.common.utils.sign.Base64.decode(encryptedData);
                    return decoded != null ? new String(decoded, "UTF-8") : null;
                default:
                    log.error("不支持的加密类型: {}", encryptTypeLabel);
                    return null;
            }
        } catch (Exception e) {
            log.error("解密失败: {}", encryptTypeLabel, e);
            return null;
        }
    }

    /**
     * 解析 JSON 并映射为标准商品列表
     * @param entryPath 数据入口路径，如 "data.wholesales"，空则自动检测常见字段名
     */
    private List<YyStandardProduct> parseAndMap(String decryptedJson, Map<String, String> fieldMappings, YyPlatform platform, String entryPath) {
        List<YyStandardProduct> result = new ArrayList<>();

        try {
            Object parsed = JSON.parse(decryptedJson);

            // 找到商品数组
            JSONArray itemArray = null;

            if (parsed instanceof JSONArray) {
                // 直接是数组
                itemArray = (JSONArray) parsed;
            } else if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;

                if (entryPath != null && !entryPath.isEmpty()) {
                    // 按指定路径定位：支持点号嵌套，如 "data.wholesales"
                    Object target = extractValue(obj, entryPath);
                    if (target instanceof JSONArray) {
                        itemArray = (JSONArray) target;
                    } else if (target instanceof JSONObject) {
                        // 路径指向单个对象，包装为单元素数组
                        itemArray = new JSONArray();
                        itemArray.add(target);
                    }
                }

                if (itemArray == null) {
                    // 未指定 entryPath 或路径未命中，自动检测常见字段名
                    String[] arrayKeys = {"items", "products", "goods", "data", "list", "result", "rows"};
                    for (String key : arrayKeys) {
                        Object val = obj.get(key);
                        if (val instanceof JSONArray) {
                            itemArray = (JSONArray) val;
                            break;
                        }
                    }
                }

                if (itemArray == null) {
                    // 仍找不到，当作单个对象处理
                    itemArray = new JSONArray();
                    itemArray.add(obj);
                }
            }

            // 映射每个 item
            if (itemArray != null) {
                for (int i = 0; i < itemArray.size(); i++) {
                    Object element = itemArray.get(i);
                    if (element instanceof JSONObject) {
                        YyStandardProduct product = mapItem((JSONObject) element, fieldMappings, platform);
                        if (product != null) {
                            result.add(product);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON失败", e);
        }

        return result;
    }

    /**
     * 将单个 JSON 对象按映射规则转为标准商品
     */
    private YyStandardProduct mapItem(JSONObject item, Map<String, String> fieldMappings, YyPlatform platform) {
        YyStandardProduct product = new YyStandardProduct();
        product.setSourcePlatform(platform.getPlatformCode());

        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String standardField = entry.getKey();
            String platformField = entry.getValue();

            // 支持一对多映射：
            // 分号 ";" = 合并语义（所有路径的数组合并为一个）
            // 逗号 "," = 备用语义（按顺序取第一个有值的字段）
            // 可混用：如 "tags.firstTags;tags.activityTags;brand,brandName"
            Object value = null;
            
            if (platformField.contains(";")) {
                // 合并模式：按分号分组，每组内逗号分隔为 first-match
                String[] mergeGroups = platformField.split(";");
                JSONArray merged = new JSONArray();
                boolean hasScalar = false;
                Object scalarValue = null;
                
                for (String group : mergeGroups) {
                    group = group.trim();
                    if (group.isEmpty()) continue;
                    
                    // 组内逗号分隔：first-match
                    String[] fallbacks = group.split(",");
                    Object groupValue = null;
                    for (String field : fallbacks) {
                        field = field.trim();
                        if (field.isEmpty()) continue;
                        groupValue = extractValue(item, field);
                        if (groupValue != null && !groupValue.toString().isEmpty()) break;
                    }
                    if (groupValue == null) continue;
                    
                    // 尝试作为数组合并
                    if (groupValue instanceof JSONArray) {
                        merged.addAll((JSONArray) groupValue);
                    } else if (groupValue instanceof String) {
                        try {
                            Object parsed = JSON.parse((String) groupValue);
                            if (parsed instanceof JSONArray) {
                                merged.addAll((JSONArray) parsed);
                            } else {
                                hasScalar = true;
                                scalarValue = groupValue;
                            }
                        } catch (Exception e) {
                            hasScalar = true;
                            scalarValue = groupValue;
                        }
                    } else {
                        hasScalar = true;
                        scalarValue = groupValue;
                    }
                }
                
                if (!merged.isEmpty()) {
                    value = merged;
                } else if (hasScalar) {
                    value = scalarValue;
                }
            } else {
                // 备用模式：逗号分隔，按顺序取第一个有值的字段
                String[] fallbacks = platformField.split("[,|]");
                for (String field : fallbacks) {
                    field = field.trim();
                    if (field.isEmpty()) continue;
                    value = extractValue(item, field);
                    if (value != null && !value.toString().isEmpty()) break;
                }
            }
            if (value == null) continue;

            setStandardField(product, standardField, value);
        }

        // 必须至少有 common_name 或 product_name
        if ((product.getCommonName() == null || product.getCommonName().isEmpty()) 
            && (product.getProductName() == null || product.getProductName().isEmpty())) {
            log.warn("丢弃条目: common_name 和 product_name 都为空, platform={}, 原始数据前200字符: {}",
                platform.getPlatformCode(),
                item.toJSONString().length() > 200 ? item.toJSONString().substring(0, 200) : item.toJSONString());
            return null;
        }

        return product;
    }

    /**
     * 从 JSON 对象中提取值（支持嵌套路径和数组下标）
     *
     * 支持语法：
     *   field                    - 直接取值
     *   a.b.c                    - 点号嵌套
     *   a.b[0].c                 - 数组下标
     *   a.b[-1]                  - 数组倒数第一个
     *   a.b[*].c                 - 数组所有元素取 c，返回逗号分隔字符串
     *   a.b[*]                   - 数组展开，返回逗号分隔字符串
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\w+|\\[\\d+\\]|\\[\\*\\])");

    private Object extractValue(JSONObject item, String fieldPath) {
        List<String> tokens = tokenize(fieldPath);
        if (tokens.isEmpty()) return null;

        Object current = item;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            current = resolveToken(current, token, tokens, i, 0);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * 从某个中间节点继续解析剩余 token（用于 [*] 展开后继续取子字段）
     */
    private Object extractFrom(Object node, List<String> tokens, int startIdx) {
        return extractFrom(node, tokens, startIdx, 0);
    }

    private Object extractFrom(Object node, List<String> tokens, int startIdx, int depth) {
        if (depth > 5) return null; // 防止极端路径导致栈溢出
        Object current = node;
        for (int i = startIdx; i < tokens.size(); i++) {
            String token = tokens.get(i);
            current = resolveToken(current, token, tokens, i, depth + 1);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * 解析单个 token：字段名、[n] 下标、[*] 展开
     */
    private Object resolveToken(Object current, String token, List<String> tokens, int tokenIdx, int depth) {
        if (token.startsWith("[") && token.endsWith("]")) {
            // 数组访问
            if (!(current instanceof JSONArray)) {
                return null;
            }
            JSONArray arr = (JSONArray) current;
            String inner = token.substring(1, token.length() - 1);

            if ("*".equals(inner)) {
                // 展开数组：取所有元素
                JSONArray collected = new JSONArray();
                int nextIdx = tokenIdx + 1;
                for (int j = 0; j < arr.size(); j++) {
                    if (nextIdx < tokens.size()) {
                        Object val = extractFrom(arr.get(j), tokens, nextIdx, depth + 1);
                        if (val != null) collected.add(val);
                    } else {
                        collected.add(arr.get(j));
                    }
                }
                return joinArrayValues(collected);
            } else {
                // 数字下标
                int idx;
                try {
                    idx = Integer.parseInt(inner);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (idx < 0) idx = arr.size() + idx;
                if (idx < 0 || idx >= arr.size()) return null;
                return arr.get(idx);
            }
        } else {
            // 对象字段访问
            if (current instanceof JSONObject) {
                return ((JSONObject) current).get(token);
            }
            // 如果是 JSON 字符串，先 parse 再取子字段
            if (current instanceof String) {
                String str = (String) current;
                try {
                    Object parsed = JSON.parse(str);
                    if (parsed instanceof JSONObject) {
                        return ((JSONObject) parsed).get(token);
                    }
                    if (parsed instanceof JSONArray) {
                        // 字符串解析为数组但要取对象字段，无法继续
                        return null;
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * 将路径字符串拆分为 token 列表
     * 如 "joinCarMap.discountTags[0].tagTitle" → ["joinCarMap", "discountTags", "[0]", "tagTitle"]
     */
    private List<String> tokenize(String fieldPath) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(fieldPath);
        while (m.find()) {
            String token = m.group().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * 将 JSONArray 转为逗号分隔字符串
     */
    private String joinArrayValues(JSONArray arr) {
        if (arr == null || arr.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(",");
            Object v = arr.get(i);
            if (v instanceof JSONObject || v instanceof JSONArray) {
                sb.append(v.toString());
            } else {
                sb.append(String.valueOf(v));
            }
        }
        return sb.toString();
    }

    /**
     * 设置标准字段值（自动类型转换）
     */
    private void setStandardField(YyStandardProduct product, String fieldName, Object value) {
        if (value == null) return;

        String strValue = value.toString().trim();
        if (strValue.isEmpty()) return;

        try {
            if (DECIMAL_FIELDS.contains(fieldName)) {
                // 金额清洗：去货币符号、单位、千分位逗号
                strValue = strValue.replaceAll("[¥￥$€£元角分厘/盒瓶袋支片粒条]", "")
                                   .replace(",", "");
                // 提取第一个合法数字
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+\\.?\\d*").matcher(strValue);
                if (m.find()) {
                    BigDecimal bd = new BigDecimal(m.group());
                    if (F_PRICE_RETAIL.equals(fieldName)) product.setPriceRetail(bd);
                    else if (F_PRICE_CURRENT.equals(fieldName)) product.setPriceCurrent(bd);
                    else if (F_PRICE_ASSEMBLE.equals(fieldName)) product.setPriceAssemble(bd);
                    else if (F_FREIGHT_AMOUNT.equals(fieldName)) product.setFreightAmount(bd);
                    else if (F_FREE_SHIPPING_THRESHOLD.equals(fieldName)) product.setFreeShippingThreshold(bd);
                }
            } else if (INTEGER_FIELDS.contains(fieldName)) {
                // 整数清洗：提取数字部分
                strValue = strValue.replaceAll("[^\\d-]", "");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+").matcher(strValue);
                if (m.find()) {
                    int intVal = Integer.parseInt(m.group());
                    if (intVal < 0) intVal = 0;
                    if (F_STOCK_QUANTITY.equals(fieldName)) product.setStockQuantity(intVal);
                    else if (F_SALES_VOLUME.equals(fieldName)) product.setSalesVolume(intVal);
                    else if (F_MIN_ORDER_QTY.equals(fieldName)) product.setMinOrderQty(intVal);
                    else if (F_MAX_ORDER_QTY.equals(fieldName)) product.setMaxOrderQty(intVal);
                }
            } else if (BOOLEAN_FIELDS.contains(fieldName)) {
                // Boolean 字段转为 0/1
                int boolVal = 0;
                if ("true".equalsIgnoreCase(strValue) || "1".equals(strValue) || "是".equals(strValue)) {
                    boolVal = 1;
                }
                if (F_IS_PRESCRIPTION_DRUG.equals(fieldName)) product.setIsPrescriptionDrug(boolVal);
                else if (F_TRACEABILITY_CODE_STATUS.equals(fieldName)) product.setTraceabilityCodeStatus(boolVal);
                else if (F_IS_TAX_INCLUDED.equals(fieldName)) product.setIsTaxIncluded(boolVal);
            } else if (DATE_FIELDS.contains(fieldName)) {
                // 日期标准化
                String normalized = normalizeDate(strValue);
                if (F_PRODUCTION_DATE.equals(fieldName)) product.setProductionDate(normalized);
                else if (F_EXPIRATION_DATE.equals(fieldName)) product.setExpirationDate(normalized);
            } else if (JSON_ARRAY_FIELDS.contains(fieldName)) {
                // JSON 数组字段：如果是对象/数组直接转JSON字符串
                // 如果是单个 JSONObject，自动包装为 JSONArray（兼容 [0] 下标取值）
                if (value instanceof JSONArray) {
                    strValue = value.toString();
                } else if (value instanceof JSONObject) {
                    JSONArray wrapped = new JSONArray();
                    wrapped.add(value);
                    strValue = wrapped.toString();
                }
                if (F_WAREHOUSE_STOCK.equals(fieldName)) product.setWarehouseStock(strValue);
                else if (F_MAIN_IMAGES.equals(fieldName)) product.setMainImages(strValue);
                else if (F_PRICE_STEP_RULES.equals(fieldName)) product.setPriceStepRules(strValue);
                else if (F_TAGS.equals(fieldName)) product.setTags(strValue);
                else if (F_MARKETING_TAGS.equals(fieldName)) product.setMarketingTags(strValue);
                else if (F_ACTIVITY_DETAILS.equals(fieldName)) product.setActivityDetails(strValue);
            } else if (JSON_OBJECT_FIELDS.contains(fieldName)) {
                // JSON 对象字段
                if (value instanceof JSONObject) {
                    strValue = value.toString();
                }
                if (F_PURCHASE_LIMITS.equals(fieldName)) product.setPurchaseLimits(strValue);
            } else {
                // String 类型
                strValue = strValue.replaceAll("\\s+", " ").trim();
                if (F_PRODUCT_ID.equals(fieldName)) product.setProductId(strValue);
                else if (F_SKU_ID.equals(fieldName)) product.setSkuId(strValue);
                else if (F_BARCODE.equals(fieldName)) product.setBarcode(strValue);
                else if (F_PRODUCT_NAME.equals(fieldName)) product.setProductName(strValue);
                else if (F_COMMON_NAME.equals(fieldName)) product.setCommonName(strValue);
                else if (F_BRAND_NAME.equals(fieldName)) product.setBrandName(strValue);
                else if (F_MANUFACTURER.equals(fieldName)) product.setManufacturer(strValue);
                else if (F_APPROVAL_NUMBER.equals(fieldName)) product.setApprovalNumber(strValue);
                else if (F_CATEGORY_ID.equals(fieldName)) product.setCategoryId(strValue);
                else if (F_CATEGORY_NAME.equals(fieldName)) product.setCategoryName(strValue);
                else if (F_SPECIFICATION.equals(fieldName)) product.setSpecification(strValue);
                else if (F_UNIT.equals(fieldName)) product.setUnit(strValue);
                else if (F_PACKING_RATIO.equals(fieldName)) product.setPackingRatio(strValue);
                else if (F_PRODUCT_STATUS.equals(fieldName)) product.setProductStatus(strValue);
                else if (F_SHELF_LIFE.equals(fieldName)) product.setShelfLife(strValue);
                else if (F_MEDICARE_TYPE.equals(fieldName)) product.setMedicareType(strValue);
                else if (F_SHOP_NAME.equals(fieldName)) product.setShopName(strValue);
            }
        } catch (Exception e) {
            log.warn("字段转换失败: {} = {}", fieldName, value);
        }
    }

    /**
     * 日期格式标准化 → yyyy-MM-dd
     */
    private String normalizeDate(String raw) {
        if (raw == null || raw.isEmpty()) return raw;

        if (raw.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}.*")) {
            String d = raw.substring(0, 10).replace("/", "-");
            String[] parts = d.split("-");
            if (parts.length == 3) {
                return parts[0] + "-" + String.format("%02d", Integer.parseInt(parts[1]))
                     + "-" + String.format("%02d", Integer.parseInt(parts[2]));
            }
            return d;
        }
        if (raw.matches("\\d{8}")) {
            return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        }
        if (raw.matches("\\d{10,13}")) {
            long ts = Long.parseLong(raw);
            if (ts > 9999999999L) ts = ts / 1000;
            java.time.LocalDate date = java.time.LocalDate.ofEpochDay(ts / 86400);
            return date.toString();
        }
        return raw;
    }

    /**
     * 生成融合键：MD5(通用名 + 规格 + 厂家 + 批准文号)
     */
    private String generateFusionKey(YyStandardProduct product) {
        String raw = normalizeForFusionKey(product.getCommonName())
                + "|" + normalizeForFusionKey(product.getSpecification())
                + "|" + normalizeForFusionKey(product.getManufacturer())
                + "|" + Optional.ofNullable(product.getApprovalNumber()).orElse("");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate fusion key for: " + raw, e);
        }
    }

    /**
     * 融合键字段标准化
     */
    private String normalizeForFusionKey(String value) {
        if (value == null || value.isEmpty()) return "";
        String s = value.trim();
        s = s.replace("×", "*").replace("x", "*").replace("X", "*");
        s = s.replaceAll("(有限公司|有限责任公司|股份公司|股份有限公司|集团|药业|制药|医药)$", "");
        s = s.replaceAll("[()（）\\[\\]【】]", "");
        s = s.replaceAll("\\s+", "");
        return s;
    }

    /**
     * 更新融合分组的聚合信息
     */
    private void updateAggregation(Long groupId) {
        List<YyStandardProduct> products = yyStandardProductMapper.selectByFusionGroupId(groupId);
        if (products.isEmpty()) return;

        YyProductFusionGroup group = yyProductFusionGroupService.selectYyProductFusionGroupById(groupId);
        if (group == null) return;

        // 计算平台数
        int platformCount = (int) products.stream()
            .map(YyStandardProduct::getSourcePlatform)
            .filter(Objects::nonNull)
            .distinct().count();
        group.setPlatformCount(platformCount);

        // 计算最低价
        BigDecimal minPrice = products.stream()
            .map(YyStandardProduct::getPriceCurrent)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);
        group.setMinPrice(minPrice);

        // 计算总库存
        int totalStock = products.stream()
            .mapToInt(p -> p.getStockQuantity() != null ? p.getStockQuantity() : 0)
            .sum();
        group.setTotalStock(totalStock);

        yyProductFusionGroupService.updateYyProductFusionGroup(group);
    }

    /**
     * 构建原始数据 JSON
     */
    private String buildRawJson(YyStandardProduct product, String fullDecryptedJson) {
        if (fullDecryptedJson.length() > 2000) {
            return fullDecryptedJson.substring(0, 2000) + "...[truncated]";
        }
        return fullDecryptedJson;
    }

    /**
     * 构建结果统计
     */
    private Map<String, Object> buildResult(int total, int groupCount, int newGroups, int updatedGroups, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("groupCount", groupCount);
        result.put("newGroups", newGroups);
        result.put("updatedGroups", updatedGroups);
        result.put("message", message);
        return result;
    }
}
