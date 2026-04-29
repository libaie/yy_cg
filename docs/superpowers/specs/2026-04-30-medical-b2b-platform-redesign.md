# 医药B2B采购比价平台 - 架构重构设计文档

> 日期: 2026-04-30
> 状态: 自检通过，待用户审核
> 范围: yy模块架构重构 + AI能力层

---

## 1. 背景与动机

### 1.1 现状

基于RuoYi 3.9.2框架的医药采购比价系统，yy模块包含14个实体、16个Service、18个Controller，以及一个Chrome扩展（CDP协议采集引擎）。支持5个B2B平台（药师帮、药京多、1药城、好药师、卖好药）。

### 1.2 核心问题

| 问题 | 现状 | 影响 |
|------|------|------|
| 融合键匹配不准确 | MD5(通用名+规格+厂家+批准文号)，归一化仅去除公司后缀和x/X→* | 不同平台同一药品匹配失败率高 |
| 字段映射扩展性差 | 简单KV映射，无值转换、条件逻辑、校验规则 | 新平台接入需手动配置38+字段 |
| 数据标准化困难 | 厂家名/规格/批准文号在不同平台表达差异大 | 同一药品在不同平台无法自动识别 |
| yy_standard_product臃肿 | 45+列 + 6个JSON列，承担三重职责 | 维护困难，查询性能差 |
| Chrome扩展耦合 | 800+行background.js，平台逻辑与通用引擎混杂 | 新平台接入需改多处代码 |
| 无药品权威参考源 | 完全依赖平台提供的数据做匹配 | 匹配准确率受限于字符串处理 |

### 1.3 设计目标

1. 架构层面重构，划清业务域边界
2. 引入药品主数据 + 多信号融合 + AI匹配，大幅提升融合准确率
3. 平台适配器模式，新平台接入变为配置任务
4. Chrome扩展模块化，支持动态加载平台配置
5. AI能力层：药品匹配、比价顾问、药品评测、智能搜索
6. 使用通义千问/百炼作为LLM服务

---

## 2. 整体架构

### 2.1 三层八域架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        接口层 (ruoyi-admin)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ platform/ │ │ product/ │ │  fusion/ │ │  price/  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │   user/  │ │collection│ │ referral/│ │   ai/    │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                    业务层 (ruoyi-system/yy)                       │
│  ┌─ platform ──────┐  ┌─ product ─────────┐  ┌─ fusion ──────┐ │
│  │ PlatformAdapter  │  │ YyDrugMaster      │  │ FusionEngine  │ │
│  │ AdapterRegistry  │  │ YyProductSnapshot │  │ MatchStrategy │ │
│  └──────────────────┘  └───────────────────┘  └───────────────┘ │
│  ┌─ price ─────────┐  ┌─ user ────────────┐  ┌─ ai ──────────┐ │
│  │ PriceComparison  │  │ YyUser            │  │ AiGateway     │ │
│  │ SalesForecast    │  │ YyMemberTier      │  │ AiMatchEngine │ │
│  └──────────────────┘  └───────────────────┘  └───────────────┘ │
│  ┌─ collection ────┐  ┌─ referral ────────┐  ┌─ common ──────┐ │
│  │ IngestPipeline   │  │ ReferralConfig    │  │ VOs/Constants │ │
│  └──────────────────┘  └───────────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                   数据层 (MySQL + Redis + 通义千问API)             │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 八个业务子域

| 子域 | 职责 | 核心实体/类 |
|------|------|-------------|
| platform | 平台管理、适配器、Token | YyPlatform, YyPlatformApi, PlatformAdapter, AdapterRegistry |
| product | 药品主数据、商品快照、字段映射 | YyDrugMaster, YyProductSnapshot, YyFieldMapping, YyDrugAlias |
| fusion | 多信号融合匹配、审核队列 | YyProductFusionGroup, FusionEngine, MatchStrategy*, YyFusionReview |
| price | 比价、价格趋势、平台活动、C端平台分析 | YyPriceComparison, YyPlatformActivity, CPlatformData |
| user | 用户、会员、平台绑定 | YyUser, YyMemberTier, YyUserPlatform |
| collection | 数据采集入库管道 | DataIngestPipeline |
| referral | 推荐奖励 | YyReferralConfig, YyReferralReward |
| ai | AI匹配、AI比价推荐、AI评测、AI搜索 | AiGateway, AiMatchEngine, AiAdvisor, AiEvaluator, AiSearchEngine |

---

## 3. 包结构重组

### 3.1 当前结构

```
com.ruoyi.yy/
  domain/          (14个类，平铺)
  dto/             (3个类)
  vo/              (6个类)
  mapper/          (14个接口)
  service/         (16个接口)
  service/impl/    (16个实现)
```

### 3.2 目标结构

```
com.ruoyi.yy/
  platform/                    # 平台管理域
    domain/                    # YyPlatform, YyPlatformApi, YyPlatformKeyVault
    adapter/                   # PlatformAdapter接口 + ConfigurablePlatformAdapter
    mapper/
    service/
    service/impl/
  product/                     # 药品与商品域
    domain/                    # YyDrugMaster, YyDrugAlias, YyProductSnapshot
    mapper/
    service/
    service/impl/
  fusion/                      # 数据融合域
    domain/                    # YyProductFusionGroup, YyFusionReview
    engine/                    # FusionEngine, MatchStrategy接口, 4个实现
    mapper/
    service/
    service/impl/
  price/                       # 比价分析域
    domain/                    # YyPriceComparison, YyPlatformActivity
    dto/                       # PriceComparisonDTO
    vo/                        # PriceComparisonVO, PriceTrendVO等
    mapper/
    service/
    service/impl/
  user/                        # 用户会员域
    domain/                    # YyUser, YyUserPlatform, YyMemberTier, YyMemberSubscription
    mapper/
    service/
    service/impl/
  collection/                  # 数据采集域
    dto/                       # YyDataIngestDTO
    service/
    service/impl/
  referral/                    # 推荐奖励域
    domain/                    # YyReferralConfig, YyReferralReward
    mapper/
    service/
    service/impl/
  ai/                          # AI能力域
    gateway/                   # AiGateway (统一AI调用网关)
    match/                     # AiMatchEngine (AI药品匹配)
    advisor/                   # AiAdvisor (AI比价顾问)
    evaluator/                 # AiEvaluator (AI药品评测)
    search/                    # AiSearchEngine (AI智能搜索)
    prompt/                    # PromptTemplate (Prompt模板管理)
    config/                    # AI配置
  common/                      # 共享组件
    vo/                        # 共享VO
    constant/                  # 常量、枚举
    util/                      # 工具类
```

### 3.3 Controller重组

```
com.ruoyi.web.controller.yy/
  platform/                    # YyPlatformController, YyPlatformApiController,
                               # YyPlatformKeyVaultController, YyPlatformDataDecryptController
  product/                     # YyStandardProductController, YyFieldMappingController
  fusion/                      # YyFusionGroupController, YyFusionReviewController(新增)
  price/                       # YyPriceComparisonController, YySalesForecastController, YyCPlatformController
  user/                        # YyLoginController, YyUserController, YyUserPlatformController,
                               # YyMemberTierController, YyMemberSubscriptionController
  collection/                  # YyDataIngestController
  referral/                    # YyReferralConfigController, YyReferralRewardController
  ai/                          # YyAiController(新增) — AI相关接口
```

### 3.4 迁移策略

- 分域逐步迁移，每迁移一个域验证通过后再迁移下一个
- `@RequestMapping` 路径保持不变，前端无需改动
- 迁移顺序: common → platform → product → fusion → collection → price → user → referral → ai

---

## 4. 数据融合引擎

### 4.1 药品主数据表

**与现有yy_product_fusion_group的关系**:

现有的 `yy_product_fusion_group` 表通过MD5融合键将不同平台的同一药品聚合。重构后：
- `yy_drug_master` 成为药品的权威参考源（一个药品一条记录）
- `yy_product_fusion_group` 保留，但其 `fusion_key` 字段改为关联 `yy_drug_master.id`，即融合分组以药品主数据为锚点
- `yy_drug_alias` 缓存各平台商品与药品主数据的映射关系，避免每次入库都重新匹配
- 逐步迁移：先创建yy_drug_master，再将现有fusion_group的数据迁移过来

**yy_drug_master** — 药品权威参考源:

```sql
CREATE TABLE yy_drug_master (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drug_code VARCHAR(64) NOT NULL COMMENT '系统生成的唯一药品编码',
    common_name VARCHAR(200) NOT NULL COMMENT '通用名',
    generic_name VARCHAR(200) DEFAULT NULL COMMENT '化学名/INN名',
    barcode VARCHAR(64) DEFAULT NULL COMMENT '主条码（69码）',
    approval_number VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    manufacturer VARCHAR(200) NOT NULL COMMENT '标准厂家名',
    specification VARCHAR(200) NOT NULL COMMENT '标准规格',
    dosage_form VARCHAR(50) DEFAULT NULL COMMENT '剂型',
    category_l1 VARCHAR(100) DEFAULT NULL COMMENT '一级分类',
    category_l2 VARCHAR(100) DEFAULT NULL COMMENT '二级分类',
    is_prescription TINYINT DEFAULT 0 COMMENT '是否处方药',
    medicare_type VARCHAR(50) DEFAULT NULL COMMENT '医保类型',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_drug_code (drug_code),
    UNIQUE KEY uk_approval_number (approval_number),
    KEY idx_barcode (barcode),
    KEY idx_common_name (common_name),
    KEY idx_manufacturer (manufacturer)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品主数据表';
```

**yy_drug_alias** — 平台别名映射缓存:

```sql
CREATE TABLE yy_drug_alias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drug_id BIGINT NOT NULL COMMENT '关联yy_drug_master.id',
    platform_code VARCHAR(50) NOT NULL COMMENT '平台编码',
    platform_product_name VARCHAR(500) DEFAULT NULL COMMENT '平台商品名',
    platform_manufacturer VARCHAR(200) DEFAULT NULL COMMENT '平台厂家名',
    platform_specification VARCHAR(200) DEFAULT NULL COMMENT '平台规格',
    platform_sku_id VARCHAR(128) DEFAULT NULL COMMENT '平台SKU ID',
    confidence DECIMAL(3,2) DEFAULT 1.00 COMMENT '匹配置信度',
    match_method VARCHAR(20) NOT NULL COMMENT '匹配方式: manual/barcode/approval/fuzzy/ai',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_sku (platform_code, platform_sku_id),
    KEY idx_drug_id (drug_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品平台别名表';
```

**yy_fusion_review** — 融合审核队列:

```sql
CREATE TABLE yy_fusion_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL COMMENT '待匹配的商品快照ID',
    candidate_drug_ids JSON DEFAULT NULL COMMENT '候选药品ID列表',
    match_scores JSON DEFAULT NULL COMMENT '各候选的匹配分数',
    ai_suggestion VARCHAR(500) DEFAULT NULL COMMENT 'AI建议',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核人',
    review_note VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    reviewed_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融合审核队列';
```

### 4.2 多信号融合流程

```
新商品入库
    │
    ▼
检查 yy_drug_alias 缓存 ──→ 命中 → 直接关联drug_id ──→ 完成
    │
    │ 未命中
    ▼
精确匹配:
  ① 69码匹配 (barcode)        → 置信度 1.0
  ② 批准文号匹配 (approval)    → 置信度 0.98
    │
    │ 未命中
    ▼
模糊匹配:
  ③ 通用名+规格+厂家归一化比较  → 置信度 0.7-0.95
     - 厂家别名词典消歧
     - 规格格式标准化
     - Levenshtein距离计算相似度
    │
    │ 未命中或置信度 < 0.8
    ▼
AI匹配:
  ④ 调用通义千问LLM
     Prompt: "以下两个药品信息是否为同一药品？"
     输入: 商品数据 vs 候选列表
     输出: 匹配结果 + 置信度 + 理由
    │
    ▼
结果处理:
  置信度 ≥ 0.95   → 自动接受，创建alias缓存
  0.80 ≤ x < 0.95 → 自动接受，标记待人工复核
  < 0.80          → 进入 yy_fusion_review 人工审核队列
```

### 4.3 MatchStrategy接口

```java
public interface MatchStrategy {
    String getName();
    int getPriority();  // 越高越先执行
    MatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates);
}
```

4个实现（按优先级排序）:

| 实现 | 优先级 | 匹配方式 | 置信度 |
|------|--------|----------|--------|
| BarcodeMatchStrategy | 100 | 69码精确匹配 | 1.0 |
| ApprovalNumberMatchStrategy | 90 | 批准文号精确匹配 | 0.98 |
| FuzzyMatchStrategy | 50 | 归一化字符串模糊匹配 | 0.7-0.95 |
| AiMatchStrategy | 10 | LLM语义匹配（兜底） | 0.5-0.99 |

### 4.4 FusionEngine

```java
@Service
public class FusionEngine {
    @Autowired
    private List<MatchStrategy> strategies;  // Spring自动注入所有实现
    @Autowired
    private YyDrugMasterMapper drugMasterMapper;
    @Autowired
    private YyDrugAliasMapper aliasMapper;
    @Autowired
    private YyFusionReviewMapper reviewMapper;

    public FusionResult fuse(YyProductSnapshot snapshot) {
        // 1. 检查alias缓存
        // 2. 获取候选集
        // 3. 按优先级执行策略
        // 4. 根据置信度处理结果
    }
}
```

---

## 5. 数据模型优化

### 5.1 yy_product_snapshot（替代yy_standard_product）

```sql
CREATE TABLE yy_product_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_platform VARCHAR(50) NOT NULL COMMENT '来源平台编码',
    sku_id VARCHAR(128) NOT NULL COMMENT '平台SKU ID',
    product_id VARCHAR(128) DEFAULT NULL COMMENT '平台商品ID',
    source_api VARCHAR(50) DEFAULT NULL COMMENT '来源API编码(hot/search)',

    -- 融合关联
    drug_id BIGINT DEFAULT NULL COMMENT '关联yy_drug_master.id',
    fusion_confidence DECIMAL(3,2) DEFAULT NULL COMMENT '融合置信度',

    -- 索引/查询字段（从JSON中提取）
    common_name VARCHAR(200) DEFAULT NULL COMMENT '通用名',
    barcode VARCHAR(64) DEFAULT NULL COMMENT '69码',
    approval_number VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    manufacturer VARCHAR(200) DEFAULT NULL COMMENT '厂家',
    specification VARCHAR(200) DEFAULT NULL COMMENT '规格',
    price_current DECIMAL(10,2) DEFAULT NULL COMMENT '当前供货价',
    stock_quantity INT DEFAULT 0 COMMENT '库存数量',

    -- 完整标准化数据
    product_data JSON NOT NULL COMMENT '标准化后的全部商品数据JSON',

    -- 原始数据
    raw_data_payload JSON DEFAULT NULL COMMENT '解密后的原始响应',

    -- 时间戳
    collected_at DATETIME DEFAULT NULL COMMENT '采集时间',
    synced_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_sku (source_platform, sku_id),
    KEY idx_drug_id (drug_id),
    KEY idx_common_name (common_name),
    KEY idx_barcode (barcode),
    KEY idx_approval_number (approval_number),
    KEY idx_price (price_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台商品快照表';
```

**product_data JSON结构**:
```json
{
    "productName": "阿莫西林胶囊",
    "brandName": "联邦",
    "categoryId": "001",
    "categoryName": "抗生素",
    "unit": "盒",
    "packingRatio": "12s*2b",
    "productStatus": 1,
    "warehouseStock": [...],
    "mainImages": [...],
    "minOrderQty": 1,
    "maxOrderQty": 999,
    "productionDate": "2025-01-01",
    "expirationDate": "2027-01-01",
    "shelfLife": "24个月",
    "isPrescriptionDrug": 1,
    "medicareType": "甲类",
    "traceabilityCodeStatus": 1,
    "salesVolume": 1500,
    "shopName": "联邦制药旗舰店",
    "priceRetail": 25.00,
    "priceStepRules": [...],
    "priceAssemble": 18.50,
    "isTaxIncluded": 1,
    "freightAmount": 0,
    "freeShippingThreshold": 99,
    "tags": [...],
    "marketingTags": [...],
    "activityDetails": [...],
    "purchaseLimits": [...]
}
```

### 5.2 yy_price_comparison微调

新增字段:
- `drug_id BIGINT` — 关联药品主数据
- `ai_recommendation_score DECIMAL(3,2)` — AI推荐评分
- `total_cost DECIMAL(10,2)` — 综合到手价（价格+运费-优惠）

### 5.3 数据迁移

从yy_standard_product迁移到yy_product_snapshot:
```sql
INSERT INTO yy_product_snapshot (source_platform, sku_id, product_id, source_api,
    drug_id, fusion_confidence, common_name, barcode, approval_number, manufacturer,
    specification, price_current, stock_quantity, raw_data_payload, collected_at, synced_at,
    product_data)
SELECT source_platform, sku_id, product_id, source_api,
    fusion_group_id, NULL, common_name, barcode, approval_number, manufacturer,
    specification, price_current, stock_quantity, raw_data_payload, collected_at, synced_at,
    JSON_OBJECT(
        'productName', product_name, 'brandName', brand_name,
        'categoryId', category_id, 'categoryName', category_name,
        'unit', unit, 'packingRatio', packing_ratio,
        'productStatus', product_status, 'warehouseStock', warehouse_stock,
        'mainImages', main_images, 'minOrderQty', min_order_qty,
        'maxOrderQty', max_order_qty, 'productionDate', production_date,
        'expirationDate', expiration_date, 'shelfLife', shelf_life,
        'isPrescriptionDrug', is_prescription_drug, 'medicareType', medicare_type,
        'traceabilityCodeStatus', traceability_code_status,
        'salesVolume', sales_volume, 'shopName', shop_name,
        'priceRetail', price_retail, 'priceStepRules', price_step_rules,
        'priceAssemble', price_assemble, 'isTaxIncluded', is_tax_included,
        'freightAmount', freight_amount, 'freeShippingThreshold', free_shipping_threshold,
        'tags', tags, 'marketingTags', marketing_tags,
        'activityDetails', activity_details, 'purchaseLimits', purchase_limits
    )
FROM yy_standard_product;
```

---

## 6. 平台适配器模式

### 6.1 PlatformAdapter接口

```java
public interface PlatformAdapter {
    String getPlatformCode();
    String decrypt(String encryptedData, YyPlatformKeyVault vault, int encryptType);
    JSONArray extractProductArray(String decryptedJson, String entryPath);
    YyProductSnapshot normalizeProduct(JSONObject rawItem, YyPlatform platform);
    List<String> buildSearchKeywords(String drugName);
}
```

### 6.2 实现策略

- `ConfigurablePlatformAdapter` — 通用实现，行为从yy_platform_api + yy_field_mapping表配置驱动
- 特殊平台（如药师帮签名）可继承重写特定方法
- `PlatformAdapterRegistry` — 启动时加载所有活跃平台适配器，运行时通过platformCode获取

### 6.3 字段映射增强

yy_field_mapping表新增字段:

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| transform_rule | VARCHAR(500) | 值转换规则 | `regex:(\\d+\\.?\\d*)` |
| default_value | VARCHAR(200) | 默认值 | `0` |
| validation_rule | VARCHAR(200) | 校验规则 | `required,numeric` |
| description | VARCHAR(500) | 字段说明 | 帮助运营人员理解映射 |

---

## 7. Chrome扩展重构

### 7.1 模块拆分

| 文件 | 职责 |
|------|------|
| engine.js | CDP执行引擎（callCdpFetch、executeDynamicApiBatch等） |
| platform-manager.js | 平台配置加载、宿主Tab管理、Token管理 |
| message-router.js | 消息路由（chrome.runtime.onMessage分发） |
| background.js | 薄入口，组装上述模块 |
| bridge.js | 保持不变 |
| token-monitor.js | 保持不变 |

### 7.2 动态配置加载

扩展启动时从后端API `/yy/platform/configForExtension` 加载全部平台配置:

```json
{
    "platforms": [
        {
            "platformCode": "ysbang",
            "platformName": "药师帮",
            "tokenDomain": "dian2.ysbang.cn",
            "tokenKey": "Token",
            "tokenStorageType": "cookie",
            "homeUrl": "https://dian2.ysbang.cn",
            "apis": [
                {
                    "apiCode": "search",
                    "apiUrl": "https://api.ysbang.cn/wholesale/search",
                    "apiMethod": "POST",
                    "buildBody": "function(kw, page, size) { ... }",
                    "dataEncrypt": 1,
                    "responseType": "json",
                    "pageField": "page",
                    "pageSizeField": "pageSize",
                    "defaultPageSize": 20,
                    "maxPages": 5
                }
            ]
        }
    ]
}
```

### 7.3 新平台接入清单（重构后）

1. yy_platform表加一行
2. yy_platform_api表加几行（每个API端点一行）
3. yy_field_mapping表加映射（可从已有平台复制后微调）
4. manifest.json的host_permissions和content_scripts.matches各加一行域名
5. （可选）yy_drug_alias预置已知商品映射

---

## 8. AI能力层

### 8.1 AI Gateway（统一网关）

```java
@Service
public class AiGateway {
    // 统一AI调用入口
    public AiResponse call(AiRequest request);

    // 模型路由：简单任务用规则引擎，复杂任务调LLM
    // Prompt模板管理：存入数据库，支持运营调整
    // 结果缓存：Redis缓存，减少API调用
    // 用量统计 & 成本控制
}
```

**LLM选择**: 通义千问/百炼

**缓存策略**:
- 相同查询的AI结果缓存到Redis，TTL 24小时
- 药品匹配结果持久化到yy_drug_alias表
- Prompt模板存入数据库，支持运营人员调整

### 8.2 AI药品匹配引擎

增强FusionEngine的第④步匹配策略:

```java
@Service
public class AiMatchStrategy implements MatchStrategy {
    @Override
    public int getPriority() { return 10; }  // 最低优先级，作为兜底

    @Override
    public MatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        // 构建prompt：将商品数据和候选列表发给LLM
        // 要求LLM判断是否为同一药品，返回置信度和理由
        // 解析LLM响应，返回MatchResult
    }
}
```

**Prompt模板**:
```
你是医药行业数据专家。请判断以下药品信息是否指向同一药品。

商品信息:
- 商品名: {productName}
- 通用名: {commonName}
- 厂家: {manufacturer}
- 规格: {specification}
- 69码: {barcode}
- 批准文号: {approvalNumber}

候选药品:
{candidates_list}

请返回JSON格式:
{"matched": true/false, "drug_id": "xxx", "confidence": 0.95, "reason": "xxx"}
```

### 8.3 AI比价顾问

**场景**: 用户搜索某药品，返回多平台价格，AI给出最优采购建议。

**AI能力**:
- 综合成本计算: 价格 + 运费 - 满减 - 会员折扣 → 真实到手价
- 凑单建议: 基于用户历史采购数据，推荐如何凑满减门槛
- 采购时机建议: 分析价格趋势，建议"现在买"还是"等降价"
- 平台组合推荐: 多药品订单的最优平台拆单方案

**实现**: 调用通义千问API，将比价数据、用户历史、活动规则作为prompt。

```java
@Service
public class AiAdvisor {
    public PurchaseAdvice getAdvice(String userId, List<PriceComparisonVO> prices) {
        // 1. 计算每个平台的综合到手价
        // 2. 获取用户历史采购模式
        // 3. 获取当前活动信息
        // 4. 构建prompt调用LLM
        // 5. 返回自然语言建议 + 结构化推荐
    }
}
```

### 8.4 AI药品评测

**场景**: 药店采购员需要了解药品质量、供应商可靠性。

**AI能力**:
- 供应商评分: 基于历史发货速度、退货率、投诉率
- 药品质量评分: 追溯码状态、批准文号有效性、厂家信誉
- 市场热度分析: 基于C端平台销售数据的市场需求趋势
- 竞品对比: 同通用名不同厂家的药品对比

```java
@Service
public class AiEvaluator {
    public ProductEvaluation evaluate(Long drugId, String platformCode) {
        // 1. 收集药品在各平台的数据
        // 2. 收集供应商历史表现数据
        // 3. 收集C端市场数据
        // 4. 调用LLM生成评测报告
        // 5. 返回结构化评分 + 文字评语
    }
}
```

### 8.5 AI智能搜索

**场景**: 用户输入模糊查询（如"感冒药"、"降压"），需要理解意图。

**AI能力**:
- 语义搜索: 理解"退烧药"→ 对乙酰氨基酚、布洛芬等
- 同义词扩展: 自动扩展通用名、商品名、俗称
- 搜索纠错: 处理拼写错误、简称

```java
@Service
public class AiSearchEngine {
    public SearchIntent understandQuery(String query) {
        // 1. 调用LLM理解搜索意图
        // 2. 扩展为标准通用名列表
        // 3. 返回结构化搜索意图
    }
}
```

### 8.6 Prompt模板管理

Prompt模板存入数据库，支持运营人员调整:

```sql
CREATE TABLE yy_ai_prompt_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    scene VARCHAR(50) NOT NULL COMMENT '场景: match/advisor/evaluator/search',
    system_prompt TEXT NOT NULL COMMENT '系统提示词',
    user_prompt_template TEXT NOT NULL COMMENT '用户提示词模板(含占位符)',
    model VARCHAR(50) DEFAULT 'qwen-turbo' COMMENT '使用的模型',
    temperature DECIMAL(3,2) DEFAULT 0.1 COMMENT '温度参数',
    max_tokens INT DEFAULT 1000 COMMENT '最大token数',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Prompt模板表';
```

---

## 9. 实现阶段

### Phase 1: 基础设施 (第1周)

1. 创建新的包结构（空包）
2. 创建 yy_drug_master、yy_drug_alias、yy_fusion_review 表
3. 创建 YyDrugMaster domain/mapper/service
4. 实现 MatchStrategy 接口和 BarcodeMatchStrategy、ApprovalNumberMatchStrategy
5. 创建 FusionEngine 骨架
6. 创建 AiGateway 骨架

### Phase 2: 数据模型迁移 (第2周)

1. 创建 yy_product_snapshot 表（新schema）
2. 编写 yy_standard_product → yy_product_snapshot 迁移脚本
3. 创建 YyProductSnapshot domain/mapper/service
4. 更新 DataFusionServiceImpl 写入新表
5. 创建 yy_fusion_review 审核UI（后台管理页面）

### Phase 3: 平台适配器 + 融合引擎 (第3周)

1. 实现 PlatformAdapter 接口和 ConfigurablePlatformAdapter
2. 实现 PlatformAdapterRegistry
3. 实现 FuzzyMatchStrategy（Levenshtein距离）
4. 实现 AiMatchStrategy（通义千问集成）
5. 将 FusionEngine 接入采集管道
6. 更新 YyDataIngestController

### Phase 4: Chrome扩展重构 (第3-4周)

1. 从 background.js 提取 engine.js
2. 创建 platform-manager.js（动态配置加载）
3. 新增后端API `/yy/platform/configForExtension`
4. 用现有5个平台测试

### Phase 5: 包迁移 (第4周)

1. 将domain类移到新子包
2. 将service接口和实现移到新子包
3. 将controller移到新子包
4. 更新所有@Autowired引用
5. 验证前端API路径不变

### Phase 6: AI能力层 (第5-6周)

1. 实现 AiGateway（通义千问API集成）
2. 创建 yy_ai_prompt_template 表和管理界面
3. 实现 AiMatchEngine（增强融合引擎）
4. 实现 AiAdvisor（比价顾问）
5. 实现 AiEvaluator（药品评测）
6. 实现 AiSearchEngine（智能搜索）
7. C端前端集成AI功能展示

---

## 10. 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 包结构 | ruoyi-system内子包 | 避免Maven模块开销，同时实现域分离 |
| 平台适配器 | DB驱动的ConfigurablePlatformAdapter | 复用现有yy_platform_api配置 |
| 药品主数据 | 独立yy_drug_master表 | 提供跨平台匹配的权威参考 |
| 融合算法 | 多信号(69码>批准文号>模糊>AI) | 直接解决匹配准确率问题 |
| yy_standard_product | 拆分为yy_product_snapshot(关键字段+JSON) | 消除45列臃肿，保留查询性能 |
| Chrome扩展 | 后端动态配置加载 | 新平台变为数据录入任务 |
| LLM服务 | 通义千问/百炼 | 国内合规、中文能力强 |
| Prompt管理 | 数据库存储，运营可调 | 灵活调整AI行为 |
| 迁移策略 | 新旧表并行运行 | 降低风险，旧表保持只读 |

---

## 11. 关键文件清单

实现时需要修改的核心文件:

| 文件 | 操作 |
|------|------|
| `ruoyi-system/.../yy/service/impl/DataFusionServiceImpl.java` | 重构：接入FusionEngine |
| `ruoyi-system/.../yy/domain/YyStandardProduct.java` | 重构：拆分为YyProductSnapshot |
| `ruoyi-system/.../yy/domain/YyFieldMapping.java` | 增强：新增transform_rule等字段 |
| `ruoyi-system/.../yy/domain/YyPlatformApi.java` | 不变 |
| `helpbuy-clone/Extensions/background.js` | 重构：拆分为4个文件 |
| `helpbuy-clone/Extensions/manifest.json` | 微调：新平台域名 |
| `ruoyi-admin/.../web/controller/yy/` | 重组：按子域分包 |

---

## 12. 验证方案

### 12.1 融合引擎验证

1. 准备测试数据：同一药品在3个不同平台的原始数据
2. 验证BarcodeMatchStrategy：69码相同时自动匹配
3. 验证ApprovalNumberMatchStrategy：批准文号相同时自动匹配
4. 验证FuzzyMatchStrategy：厂家名不同时仍能匹配（如同仁堂 vs 北京同仁堂股份有限公司）
5. 验证AiMatchStrategy：极端情况下LLM兜底匹配
6. 验证yy_fusion_review队列：低置信度商品进入人工审核

### 12.2 平台适配器验证

1. 用现有5个平台的配置测试ConfigurablePlatformAdapter
2. 验证字段映射增强（transform_rule）
3. 验证新平台接入流程

### 12.3 Chrome扩展验证

1. 验证动态配置加载
2. 验证5个平台的数据采集不受影响
3. 验证新平台接入只需数据库配置+manifest.json加域名

### 12.4 AI能力验证

1. 验证AiGateway调用通义千问API
2. 验证Prompt模板管理
3. 验证AI匹配对fusion准确率的提升
4. 验证AI比价顾问的建议质量
5. 验证AI药品评测的输出
6. 验证AI智能搜索的语义理解能力
