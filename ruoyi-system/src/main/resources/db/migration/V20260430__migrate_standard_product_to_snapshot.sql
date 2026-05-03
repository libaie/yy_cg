-- V20260430__migrate_standard_product_to_snapshot.sql
-- 从yy_standard_product迁移到yy_product_snapshot
-- 注意：此脚本应在新表创建完成后执行

INSERT INTO yy_product_snapshot (source_platform, sku_id, product_id, source_api,
    drug_id, fusion_confidence, common_name, barcode, approval_number, manufacturer,
    specification, price_current, stock_quantity, raw_data_payload, collected_at, synced_at,
    product_data)
SELECT
    sp.source_platform,
    sp.sku_id,
    sp.product_id,
    sp.source_api,
    sp.fusion_group_id,
    NULL,
    sp.common_name,
    sp.barcode,
    sp.approval_number,
    sp.manufacturer,
    sp.specification,
    sp.price_current,
    sp.stock_quantity,
    sp.raw_data_payload,
    sp.collected_at,
    sp.synced_at,
    JSON_OBJECT(
        'productName', sp.product_name,
        'brandName', sp.brand_name,
        'categoryId', sp.category_id,
        'categoryName', sp.category_name,
        'unit', sp.unit,
        'packingRatio', sp.packing_ratio,
        'productStatus', sp.product_status,
        'salesVolume', sp.sales_volume,
        'shopName', sp.shop_name,
        'priceRetail', sp.price_retail,
        'priceAssemble', sp.price_assemble,
        'isTaxIncluded', sp.is_tax_included,
        'freightAmount', sp.freight_amount,
        'freeShippingThreshold', sp.free_shipping_threshold
    )
FROM yy_standard_product sp
WHERE NOT EXISTS (
    SELECT 1 FROM yy_product_snapshot ps
    WHERE ps.source_platform = sp.source_platform
    AND ps.sku_id = sp.sku_id
);
