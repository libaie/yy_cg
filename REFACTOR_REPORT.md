# YyStandardProduct 全链路重构完成报告

## 修改时间
2026-04-14

## 重构内容
将 YyStandardProduct 从旧的24字段结构重构为新的38字段标准模板

## 修改文件清单

### 1. 数据库
| 文件 | 说明 |
|------|------|
| `sql/yy_standard_product_v2.sql` | 新建表结构（38字段） |
| `sql/yy_fusion_group_rename.sql` | 融合分组表字段重命名 generic_name → common_name |

### 2. Java Domain
| 文件 | 说明 |
|------|------|
| `ruoyi-system/.../domain/YyStandardProduct.java` | 实体类（38字段） |
| `ruoyi-system/.../domain/YyProductFusionGroup.java` | 融合分组实体（genericName → commonName） |

### 3. Mapper
| 文件 | 说明 |
|------|------|
| `ruoyi-system/.../mapper/YyStandardProductMapper.java` | Mapper接口（platformSkuId → skuId） |
| `ruoyi-system/.../mapper/yy/YyStandardProductMapper.xml` | XML映射（38字段） |
| `ruoyi-system/.../mapper/yy/YyProductFusionGroupMapper.xml` | 融合分组XML（generic_name → common_name） |

### 4. Service
| 文件 | 说明 |
|------|------|
| `ruoyi-system/.../service/IYyStandardProductService.java` | Service接口 |
| `ruoyi-system/.../service/impl/YyStandardProductServiceImpl.java` | Service实现 |
| `ruoyi-system/.../service/impl/DataFusionServiceImpl.java` | 数据融合服务（全量重写） |

### 5. Controller
| 文件 | 说明 |
|------|------|
| `ruoyi-admin/.../controller/yy/YyStandardProductController.java` | 控制器 |

### 6. 前端
| 文件 | 说明 |
|------|------|
| `ruoyi-ui/src/api/yy/standardProduct.js` | API接口 |
| `ruoyi-ui/src/views/yy/standardProduct/index.vue` | 标准商品页面（38字段） |
| `ruoyi-ui/src/views/yy/fusionGroup/index.vue` | 融合分组页面（更新字段引用） |

## 字段映射关系（旧 → 新）

| 旧字段 | 新字段 | 说明 |
|--------|--------|------|
| platformId | source_platform | 平台ID → 平台编码 |
| platformSkuId | sku_id | SKU标识 |
| platformName | - | 移除（关联查询） |
| genericName | common_name | 通用名 |
| rawData | raw_data_payload | 原始数据 |
| currentPrice | price_current | 当前价 |
| minPrice | - | 移除（使用阶梯价） |
| stock | stock_quantity | 库存 |
| minOrderQty | - | 移除（使用purchase_limits） |
| validUntil | expiration_date | 有效期 |
| supplier | shop_name | 供应商 |
| productUrl | - | 移除 |
| productImage | main_images | 单图 → 多图 |
| originalUnit | unit | 单位 |
| productPlatformFullName | product_name | 平台全名 → 商品名 |
| platformDiscounts | - | 移除（使用activity_details） |
| stockStatus | product_status | 库存状态 → 商品状态 |
| remark | - | 移除 |

## 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| product_id | String | 商品ID |
| barcode | String | 商品条码 |
| brand_name | String | 品牌名称 |
| category_id | String | 分类ID |
| category_name | String | 分类名称 |
| packing_ratio | String | 中包装/件装量 |
| warehouse_stock | JSON | 分仓库存 |
| production_date | String | 生产日期 |
| shelf_life | String | 保质期 |
| is_prescription_drug | Boolean | 是否处方药 |
| medicare_type | String | 医保类型 |
| traceability_code_status | Boolean | 追溯码状态 |
| sales_volume | Number | 销量 |
| price_retail | Number | 零售价 |
| price_step_rules | JSON | 阶梯价规则 |
| price_assemble | Number | 拼团价 |
| is_tax_included | Boolean | 是否含税 |
| freight_amount | Number | 运费 |
| free_shipping_threshold | Number | 免邮门槛 |
| tags | JSON | 商品标签 |
| marketing_tags | JSON | 营销标签 |
| activity_details | JSON | 活动明细 |
| purchase_limits | Object | 限购规则 |

## 数据库升级步骤

```sql
-- 1. 执行新的表结构（注意备份数据）
source yy_standard_product_v2.sql;

-- 2. 执行融合分组字段重命名
source yy_fusion_group_rename.sql;

-- 3. 如需迁移旧数据，可编写迁移脚本
-- INSERT INTO yy_standard_product (sku_id, source_platform, common_name, ...)
-- SELECT platform_sku_id, platform_id, generic_name, ... FROM yy_standard_product_old;
```

## 测试要点

1. **数据融合流程** - 验证新字段映射是否正常工作
2. **融合分组** - 验证 commonName 替换 genericName 后的查询
3. **前端页面** - 验证列表展示和详情弹窗
4. **阶梯价** - 验证 price_step_rules JSON 存储和解析
5. **多图** - 验证 main_images JSON 数组处理
