# 医药B2B采购比价平台 - 架构重构设计文档

> 日期: 2026-04-30
> 状态: 全面审查通过 (/autoplan 4-phase review)
> 范围: yy模块架构重构 + AI能力层（缩减至5个核心模块）
> 审查结果: 5 P0, 10 P1, 7 P2/P3 — 详见审查报告

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
5. 医药知识库：自建 + NMPA/医保目录/药智网等外部数据源，RAG架构
6. AI能力层（Phase 1: 5个核心模块）：AiMatchEngine（药品匹配）、AiAdvisor（比价顾问）、AiDataCleaner（数据清洗）、AiSearchEngine（智能搜索）、AiEvaluator（药品评测）
7. 使用通义千问/百炼作为LLM服务（需基准测试对比DeepSeek）

> **审查修订**: 原计划20个AI模块缩减至5个核心模块。剩余15个模块待Phase 1核心模块采用率验证后再实施。

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
| ai | AI能力层（20个AI模块） | AiGateway, AiMatchEngine, AiAdvisor, AiEvaluator, AiSearchEngine, AiPurchasePlanner, AiComplianceChecker, AiMarketIntelligence, AiCrossSell, AiImageSearch, AiPricingAdvisor, AiInventoryOptimizer, AiChatAssistant, AiOperationAssistant, AiDataCleaner, AiReportEngine, AiNegotiationAdvisor, AiAnomalyDetector, AiUserProfile |

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
  ai/                          # AI能力域（20个AI模块）
    gateway/                   # AiGateway (统一AI调用网关)
    match/                     # AiMatchEngine (AI药品匹配)
    advisor/                   # AiAdvisor (AI比价顾问)
    evaluator/                 # AiEvaluator (AI药品评测)
    search/                    # AiSearchEngine (AI智能搜索)
    purchase/                  # AiPurchasePlanner (AI采购计划)
    compliance/                # AiComplianceChecker (AI合规检查)
    intelligence/              # AiMarketIntelligence (AI市场情报)
    crosssell/                 # AiCrossSell (AI关联推荐)
    imagesearch/               # AiImageSearch (AI图片识别)
    pricing/                   # AiPricingAdvisor (AI定价建议)
    inventory/                 # AiInventoryOptimizer (AI库存优化)
    chat/                      # AiChatAssistant (AI智能客服)
    operation/                 # AiOperationAssistant (AI自动化运营)
    cleaner/                   # AiDataCleaner (AI数据清洗)
    report/                    # AiReportEngine (AI自然语言报表)
    negotiation/               # AiNegotiationAdvisor (AI谈判助手)
    anomaly/                   # AiAnomalyDetector (AI异常检测)
    profile/                   # AiUserProfile (AI用户画像)
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

## 8. 医药知识库

### 8.0.1 知识库架构

采用RAG（检索增强生成）架构，为所有AI模块提供知识支撑：

```
┌─────────────────────────────────────────────────────────────┐
│                    医药知识库 (RAG架构)                        │
│                                                              │
│  ┌─ 结构化数据层 ──────────────────────────────────────────┐ │
│  │  yy_drug_master (药品主数据：编码、通用名、厂家、规格)    │ │
│  │  yy_drug_monograph (药品专论：适应症、禁忌、相互作用)     │ │
│  │  yy_compliance_data (合规数据：批准文号、GSP/GMP资质)     │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ 向量检索层（可选，初期可用MySQL全文索引替代）────────────┐ │
│  │  Milvus/Qdrant/Chroma                                    │ │
│  │  存储药品专论的向量嵌入，支持语义搜索                      │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ 外部数据源 ────────────────────────────────────────────┐ │
│  │  NMPA药监局 (批准文号、厂家、资质) — 免费爬取/API         │ │
│  │  国家医保目录 (医保分类、支付标准) — 免费下载              │ │
│  │  国家基本药物目录 — 免费下载                              │ │
│  │  药智网 API (批文查询、中标价) — 基础版免费               │ │
│  │  丁香园用药助手 (药物相互作用) — 付费                     │ │
│  │  B2B平台采集数据 (说明书、详情页) — 已有采集能力          │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 8.0.2 数据获取策略

**第1层：免费公开数据（立即可用）**
- NMPA国家药监局药品数据库 → 批准文号、厂家、规格、剂型
- 国家医保目录 → 医保分类、支付标准
- 国家基本药物目录 → 基药标识
- Wikidata/Wikipedia → 药品通用知识

**第2层：采集积累数据（随使用增长）**
- 各B2B平台采集的商品数据 → 价格、库存、销量
- 药品说明书（从平台商品详情页采集）→ 适应症、用法、禁忌
- 用户行为数据 → 搜索、采购、评价

**第3层：专业数据库（按需接入）**
- 药智网API → 批文查询、中标价、一致性评价
- 丁香园用药助手API → 专业药品信息、药物相互作用
- 米内网 → 市场数据、销售排名
- OpenFDA / DrugBank → 国际药品数据

**第4层：AI生成+人工审核**
- LLM基于已有数据生成药品专论摘要
- 人工药剂师审核校正
- 用户反馈纠错

### 8.0.3 知识库数据表

**yy_drug_monograph** — 药品专论知识库:

```sql
CREATE TABLE yy_drug_monograph (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drug_id BIGINT NOT NULL COMMENT '关联yy_drug_master',
    indications TEXT COMMENT '适应症',
    contraindications TEXT COMMENT '禁忌症',
    dosage_administration TEXT COMMENT '用法用量',
    adverse_reactions TEXT COMMENT '不良反应',
    drug_interactions TEXT COMMENT '药物相互作用',
    special_populations TEXT COMMENT '特殊人群(孕妇/儿童/老人)',
    storage_conditions VARCHAR(200) COMMENT '储存条件',
    pharmacology TEXT COMMENT '药理作用',
    clinical_evidence TEXT COMMENT '临床证据',
    summary TEXT COMMENT 'AI生成的摘要',
    source VARCHAR(50) COMMENT '数据来源: nmpa/manual/wiki/llm',
    confidence DECIMAL(3,2) DEFAULT 1.00 COMMENT '数据置信度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_drug (drug_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品专论知识库';
```

**yy_compliance_data** — 合规数据:

```sql
CREATE TABLE yy_compliance_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    approval_number VARCHAR(100) NOT NULL COMMENT '批准文号',
    drug_name VARCHAR(200) COMMENT '药品名称',
    manufacturer VARCHAR(200) COMMENT '生产厂家',
    specification VARCHAR(200) COMMENT '规格',
    dosage_form VARCHAR(50) COMMENT '剂型',
    approval_date DATE COMMENT '批准日期',
    validity_period DATE COMMENT '有效期',
    gmp_certified TINYINT DEFAULT 0 COMMENT 'GMP认证',
    gmp_expiry DATE COMMENT 'GMP有效期',
    consistency_evaluation TINYINT DEFAULT 0 COMMENT '一致性评价通过',
    medical_insurance_type VARCHAR(50) COMMENT '医保类型',
    essential_drug TINYINT DEFAULT 0 COMMENT '是否基药',
    data_source VARCHAR(50) COMMENT '数据来源: nmpa/yaozhi/manual',
    last_synced_at DATETIME COMMENT '最近同步时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval (approval_number),
    KEY idx_manufacturer (manufacturer)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规数据表';
```

### 8.0.4 知识库初始化流程

1. **NMPA数据导入**: 爬取NMPA药品数据库，批量导入yy_compliance_data
2. **医保目录导入**: 下载国家医保目录Excel，导入yy_drug_master.medicare_type
3. **B2B数据回填**: 从已采集的yy_product_snapshot中提取说明书信息，填充yy_drug_monograph
4. **LLM增强**: 对已有数据调用LLM生成结构化摘要
5. **持续更新**: Chrome扩展采集详情页时同步采集说明书，定时同步NMPA数据

---

## 9. AI能力层

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
    scene VARCHAR(50) NOT NULL COMMENT '场景: match/advisor/evaluator/search/purchase/compliance/intelligence/crosssell/imagesearch/pricing/inventory/chat',
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

### 8.7 AI采购计划自动生成

**场景**: 药店每周/每月需要制定采购计划，目前依赖人工经验。

**AI能力**:
- 基于历史销售数据 + 季节性因素（感冒药冬季需求高、防暑药夏季需求高）
- 结合当前库存水位和安全库存阈值
- 考虑药品效期，避免过量采购导致过期损耗
- 自动生成每周/每月采购清单，包含建议采购量和最优平台
- 支持人工调整后重新计算

```java
@Service
public class AiPurchasePlanner {
    public PurchasePlan generatePlan(String pharmacyId, String period) {
        // 1. 获取历史销售数据（近3-6个月）
        // 2. 分析季节性趋势和品类关联
        // 3. 获取当前库存和效期信息
        // 4. 调用LLM生成采购计划
        // 5. 返回结构化采购清单 + 自然语言说明
    }
}
```

**新增数据表**:
```sql
CREATE TABLE yy_purchase_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_period VARCHAR(20) NOT NULL COMMENT '计划周期(2026-W18/2026-05)',
    plan_data JSON NOT NULL COMMENT '采购计划明细JSON',
    ai_summary TEXT COMMENT 'AI生成的采购建议摘要',
    status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/confirmed/completed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_period (user_id, plan_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI采购计划表';
```

### 8.8 AI合规检查

**场景**: 医药行业监管严格，药店需要确保采购的药品和供应商资质合规。

**AI能力**:
- 批准文号真伪验证（对接国家药监局数据库）
- 供应商GSP/GMP资质有效期检查
- 追溯码状态异常预警
- 医保目录变动影响分析（某药品被移出医保 → 影响采购策略）
- 处方药/非处方药分类合规检查

```java
@Service
public class AiComplianceChecker {
    public ComplianceReport check(Long drugId, String platformCode) {
        // 1. 验证批准文号格式和有效性
        // 2. 检查供应商资质
        // 3. 检查追溯码状态
        // 4. 检查医保分类
        // 5. 生成合规报告
    }
}
```

**新增数据表**:
```sql
CREATE TABLE yy_compliance_check (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drug_id BIGINT COMMENT '药品ID',
    platform_code VARCHAR(50) COMMENT '平台编码',
    check_type VARCHAR(50) NOT NULL COMMENT '检查类型: approval/supplier/traceability/medicalcare',
    check_result VARCHAR(20) NOT NULL COMMENT 'pass/warning/fail',
    detail JSON COMMENT '检查详情',
    checked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_drug (drug_id),
    KEY idx_result (check_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI合规检查记录表';
```

### 8.9 AI市场情报

**场景**: 医药行业政策变动频繁（集采、医保谈判），价格波动大，需要及时掌握市场动态。

**AI能力**:
- 行业政策监控：集采扩面、医保谈判结果、GSP新规
- 价格趋势分析：基于历史比价数据，预测药品价格走势
- 平台促销规律分析：各平台的促销周期、力度、品类偏好
- 新药上市提醒：同品类新药上市可能影响现有采购策略
- 竞品动态：同区域药店的采购行为分析

```java
@Service
public class AiMarketIntelligence {
    public MarketReport generateReport(String category, String region) {
        // 1. 收集政策新闻（可通过爬虫或API）
        // 2. 分析价格趋势数据
        // 3. 分析平台促销历史
        // 4. 调用LLM生成市场情报报告
        // 5. 返回结构化报告 + 关键提醒
    }
}
```

### 8.10 AI药品关联推荐

**场景**: 药店采购时往往只关注急需药品，遗漏了关联品类的补充。

**AI能力**:
- 基于药店采购数据的关联分析（Apriori/FP-Growth算法）
- 季节性组合推荐（流感季：抗病毒+退烧+维生素C套餐）
- 疾病场景套餐（高血压：降压药+血压计+低钠盐）
- 帮助药店发现遗漏的热销品类，提升客单价

```java
@Service
public class AiCrossSell {
    public List<CrossSellRecommendation> recommend(String userId, List<Long> cartDrugIds) {
        // 1. 获取用户购物车/历史采购
        // 2. 基于关联规则挖掘推荐商品
        // 3. 结合季节性和地区因素
        // 4. 调用LLM生成推荐理由
        // 5. 返回推荐列表
    }
}
```

### 8.11 AI图片识别购药

**场景**: 药店店员看到竞品或顾客带来的药品，想快速找到采购渠道和最低价。

**AI能力**:
- 拍照识别药品包装（通用名、厂家、规格）
- 扫描药品条码（69码）直接匹配
- 识别结果自动搜索全网最低价
- 支持模糊拍照（通过药品外观特征识别）

```java
@Service
public class AiImageSearch {
    public ImageSearchResult search(byte[] imageData) {
        // 1. 调用通义千问VL（视觉语言模型）识别药品信息
        // 2. 提取通用名、厂家、规格、69码
        // 3. 通过FusionEngine匹配药品主数据
        // 4. 搜索各平台价格
        // 5. 返回识别结果 + 比价结果
    }
}
```

**技术方案**: 使用通义千问VL（视觉理解模型）进行药品包装识别，结合69码条码扫描。

### 8.12 AI定价建议

**场景**: 药店采购后需要制定零售价，目前主要靠经验。

**AI能力**:
- 分析同区域同品类药品的零售价分布
- 基于采购成本 + 目标毛利率，建议最优零售定价
- 医保支付价与市场价的差异分析
- 特殊药品（集采药品）的定价约束提醒
- 动态定价建议：临近效期的药品降价促销

```java
@Service
public class AiPricingAdvisor {
    public PricingAdvice getAdvice(Long drugId, BigDecimal purchasePrice, String region) {
        // 1. 获取该药品的市场零售价分布
        // 2. 获取医保支付价
        // 3. 计算毛利率区间
        // 4. 调用LLM生成定价建议
        // 5. 返回建议零售价 + 理由
    }
}
```

### 8.13 AI库存优化

**场景**: 药店库存管理直接影响资金周转和药品效期损耗。

**AI能力**:
- 基于销售预测 + 补货周期 + 资金约束，优化库存周转率
- 效期预警：临近过期的药品优先推荐促销或调拨
- 缺货风险预测：哪些药品可能断货，建议提前备货
- 滞销品识别：长期不动销的药品建议退换或促销
- 安全库存动态调整：根据销售波动自动调整安全库存阈值

```java
@Service
public class AiInventoryOptimizer {
    public InventoryAdvice optimize(String userId) {
        // 1. 获取当前库存数据
        // 2. 分析销售速度和波动
        // 3. 检查效期分布
        // 4. 调用LLM生成优化建议
        // 5. 返回：需补货清单、需促销清单、需退货清单
    }
}
```

**新增数据表**:
```sql
CREATE TABLE yy_inventory_alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    alert_type VARCHAR(20) NOT NULL COMMENT '预警类型: low_stock/expiry_risk/slow_moving/overstock',
    severity VARCHAR(10) NOT NULL COMMENT '严重程度: low/medium/high',
    current_quantity INT COMMENT '当前库存',
    suggested_action VARCHAR(50) COMMENT '建议操作: reorder/promote/return/adjust',
    ai_detail TEXT COMMENT 'AI分析详情',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/resolved/ignored',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_type (user_id, alert_type),
    KEY idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI库存预警表';
```

### 8.14 AI智能客服/采购助手

**场景**: 药店采购员在使用平台时有各种问题，需要即时解答。

**AI能力**:
- 对话式采购："帮我找一下最便宜的阿莫西林，要现货的"
- 自动回答药品相关问题（用法、适应症、禁忌、相互作用）
- 采购异常处理（价格异常波动提醒、订单问题自动排查）
- 平台功能引导（如何绑定账号、如何使用比价功能）
- 多轮对话支持，记住上下文

```java
@Service
public class AiChatAssistant {
    public ChatResponse chat(String userId, String message, String sessionId) {
        // 1. 获取对话历史
        // 2. 判断意图（采购/咨询/投诉/引导）
        // 3. 根据意图调用对应AI模块
        // 4. 调用LLM生成回复
        // 5. 返回回复 + 可能的操作建议
    }
}
```

**技术方案**: 使用通义千问的多轮对话能力，结合Function Calling实现工具调用（搜索药品、查询价格、下单等）。

### 8.15 AI自动化运营

**场景**: 平台运营人员需要持续产出营销内容和促销方案。

**AI能力**:
- 自动生成药品营销文案（适合C端展示的商品描述、卖点提炼）
- 促销方案生成：基于库存+销售数据，推荐促销组合和力度
- 节日/季节性营销日历自动生成（流感季、换季、节假日）
- 会员活动策划建议

```java
@Service
public class AiOperationAssistant {
    public MarketingContent generateCopy(Long drugId, String style) { ... }
    public PromotionPlan generatePromotion(String category, String period) { ... }
    public MarketingCalendar generateCalendar(int year, int month) { ... }
}
```

### 8.16 AI数据清洗与标准化

**场景**: 历史采集数据中存在大量脏数据（错别字、格式不一致、缺失值）。

**AI能力**:
- 批量标准化厂家名（"同仁堂" vs "北京同仁堂股份有限公司"）
- 规格格式统一（"0.25g×12片" vs "0.25g*12s"）
- 识别并修复数据不一致（同一药品不同平台的通用名不同）
- 缺失字段智能填充（基于已有字段推断缺失的69码、批准文号等）

```java
@Service
public class AiDataCleaner {
    public CleanResult cleanProductData(List<YyProductSnapshot> snapshots) {
        // 1. 批量发送给LLM进行标准化
        // 2. 对比知识库中的标准数据
        // 3. 生成清洗建议
        // 4. 自动或人工确认后执行清洗
    }
}
```

### 8.17 AI自然语言报表

**场景**: 管理员需要分析数据但不熟悉SQL或BI工具。

**AI能力**:
- 自然语言查询："上个月阿莫西林在各平台的价格变化趋势" → 自动生成图表
- 异常发现："哪些药品这个月销量下降了超过20%" → 自动分析
- 定时报告：每周自动生成采购分析报告
- 支持多种图表类型（折线图、柱状图、饼图）

```java
@Service
public class AiReportEngine {
    public ReportResult generateReport(String naturalLanguageQuery) {
        // 1. 调用LLM将自然语言转换为SQL/数据查询
        // 2. 执行查询获取数据
        // 3. 调用LLM生成分析结论
        // 4. 返回数据 + 图表配置 + 文字分析
    }
}
```

### 8.18 AI供应商谈判助手

**场景**: 大型药店需要与供应商谈判采购价格和条件。

**AI能力**:
- 分析历史采购量和价格，计算议价空间
- 对比同类供应商的价格和服务
- 生成谈判策略建议（批量折扣、账期、返利）
- 市场价格基准参考

```java
@Service
public class AiNegotiationAdvisor {
    public NegotiationStrategy advise(String userId, Long drugId, String supplierCode) {
        // 1. 分析用户历史采购数据
        // 2. 对比市场价格
        // 3. 计算议价空间
        // 4. 生成谈判策略
    }
}
```

### 8.19 AI异常检测与预警

**场景**: 平台运营需要及时发现异常情况。

**AI能力**:
- 价格异常波动预警（某药品突然涨价/降价超过阈值）
- 库存异常预警（某平台突然大量缺货）
- 订单异常检测（异常采购模式、疑似刷单）
- 供应商异常预警（发货延迟增加、退货率上升）

```java
@Service
public class AiAnomalyDetector {
    public List<AnomalyAlert> detect() {
        // 1. 定时扫描价格、库存、订单数据
        // 2. 基于统计模型检测异常
        // 3. 调用LLM分析异常原因
        // 4. 生成预警通知
    }
}
```

**新增数据表**:
```sql
CREATE TABLE yy_anomaly_alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alert_type VARCHAR(30) NOT NULL COMMENT 'price/stock/order/supplier',
    target_type VARCHAR(30) NOT NULL COMMENT 'drug/platform/supplier',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    severity VARCHAR(10) NOT NULL COMMENT 'low/medium/high/critical',
    description VARCHAR(500) NOT NULL COMMENT '异常描述',
    detail JSON COMMENT '异常详情',
    ai_analysis TEXT COMMENT 'AI分析原因',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/acknowledged/resolved',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_type_status (alert_type, status),
    KEY idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI异常预警表';
```

### 8.20 AI用户画像与精准营销

**场景**: 不同类型的药店有不同的采购需求，需要精准推荐。

**AI能力**:
- 基于采购行为的药店分群（大型连锁/社区药店/诊所/中医馆）
- 个性化推荐：根据药店类型推荐适合的药品组合
- 流失预警：识别可能不再使用平台的用户
- 生命周期管理：新用户引导、活跃用户激励、沉默用户唤醒

```java
@Service
public class AiUserProfile {
    public UserProfile analyze(String userId) {
        // 1. 分析用户采购历史
        // 2. 聚类分群
        // 3. 生成用户画像标签
        // 4. 个性化推荐策略
    }
    public List<String> predictChurn() {
        // 识别流失风险用户
    }
}
```

**新增数据表**:
```sql
CREATE TABLE yy_chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    messages JSON NOT NULL COMMENT '对话消息历史',
    intent VARCHAR(50) COMMENT '识别的意图',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/closed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session (session_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服会话表';
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

### Phase 6: 医药知识库 + AI基础 (第5-6周)

1. 创建 yy_drug_monograph、yy_compliance_data 表
2. NMPA药品数据爬取和导入脚本
3. 国家医保目录数据导入
4. 从已有B2B采集数据中提取说明书信息
5. 实现 AiGateway（通义千问API集成 + 模型路由 + 缓存）
6. 创建 yy_ai_prompt_template 表和管理界面

### Phase 7: AI核心模块 (第7-8周)

1. 实现 AiMatchEngine（增强融合引擎）
2. 实现 AiAdvisor（比价顾问）
3. 实现 AiEvaluator（药品评测）+ 知识库RAG
4. 实现 AiSearchEngine（智能搜索）
5. 实现 AiComplianceChecker（合规检查）+ NMPA数据对接
6. 实现 AiDataCleaner（数据清洗标准化）
7. C端前端集成AI功能展示

### Phase 8: AI业务模块 (第9-10周)

1. 实现 AiPurchasePlanner（采购计划自动生成）+ yy_purchase_plan表
2. 实现 AiMarketIntelligence（市场情报）
3. 实现 AiCrossSell（关联推荐）
4. 实现 AiPricingAdvisor（定价建议）
5. 实现 AiInventoryOptimizer（库存优化）+ yy_inventory_alert表
6. 实现 AiImageSearch（图片识别购药）+ 通义千问VL集成
7. C端前端集成业务AI功能

### Phase 9: AI高级模块 (第11-12周)

1. 实现 AiChatAssistant（智能客服）+ yy_chat_session表 + Function Calling + 知识库RAG
2. 实现 AiOperationAssistant（自动化运营）
3. 实现 AiReportEngine（自然语言报表）
4. 实现 AiNegotiationAdvisor（供应商谈判助手）
5. 实现 AiAnomalyDetector（异常检测预警）+ yy_anomaly_alert表
6. 实现 AiUserProfile（用户画像与精准营销）
7. C端和后台前端集成全部AI功能

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

### 12.4 AI能力验证 - 核心模块

1. 验证AiGateway调用通义千问API + 结果缓存
2. 验证Prompt模板管理
3. 验证AI匹配对fusion准确率的提升
4. 验证AI比价顾问的建议质量
5. 验证AI药品评测的输出
6. 验证AI智能搜索的语义理解能力

### 12.5 AI能力验证 - 扩展模块

1. 验证AI采购计划的合理性（与人工计划对比）
2. 验证AI合规检查的准确性（批准文号、供应商资质）
3. 验证AI市场情报的时效性和准确性
4. 验证AI关联推荐的转化率
5. 验证AI图片识别的准确率（通义千问VL）
6. 验证AI定价建议与市场价的偏差
7. 验证AI库存预警的及时性
8. 验证AI智能客服的多轮对话和Function Calling

---

## 13. 审查结果与修订项

### 13.1 P0 必须修复项（实施前）

| # | 问题 | 来源 | 修复方案 |
|---|------|------|----------|
| 1 | `YyProductFusionGroup.setGenericName()` 抛出 `UnsupportedOperationException` | Eng CQ-10 | 实现 setter（1行代码） |
| 2 | `handleAppendFetch()` 引用未定义变量 `apiCode`/`rawData` | Eng CQ-8 | 从调用方传入变量 |
| 3 | 整个代码库零自动化测试 | Eng TR-1 | 重构前先为 `ingest()` 添加集成测试 |
| 4 | N+1 查询：200产品批次产生550次DB往返 | Eng PR-1 | 批量化：`SELECT ... WHERE fusion_key IN (...)` + MyBatis `<foreach>` |
| 5 | LLM调用无熔断/超时/降级 | Eng AR-5 | 5s超时、指数退避、熔断器（3次失败→跳过5分钟）、降级到审核队列 |

### 13.2 P1 实施中修复项

| # | 问题 | 修复方案 |
|---|------|----------|
| 1 | `@Transactional` 缺少 `rollbackFor = Exception.class` | 添加注解参数 |
| 2 | `hashCode()` 降级可产生负数 key | 移除降级，让 MD5 异常传播 |
| 3 | `YyDataIngestDTO` 无输入校验 | 添加 `@Valid`/`@NotBlank` 注解 |
| 4 | 域间循环依赖风险 | 定义单向依赖 DAG：`ai→fusion→product→platform` |
| 5 | 策略链无早停优化；候选集获取策略未定义 | 添加 `stopOnMatch()`；按归一化通用名前缀索引 |
| 6 | 无 LLM 测试策略 | MockAiGateway 单元测试 + 金标准数据集夜间回归 |
| 7 | JSON 列每次列表查询都需解析 | 高频展示字段保留为独立列（不止6个） |
| 8 | AI 调用延迟 2-10s 影响实时搜索 | Redis 预计算搜索扩展；激进超时（搜索3s，批处理10s） |
| 9 | 缓存策略未定义 | L1 内存 + L2 Redis + L3 alias 表；归一化缓存键 |
| 10 | AI 聊天机器人无边界定义（法律风险） | 定义允许的主题范围和拒绝模式 |

### 13.3 已知风险（接受并监控）

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Chrome MV3 限制收紧 | 数据采集管道可能中断 | 记录为已知风险，Phase 2 评估替代方案 |
| 通义千问 API 成本 | 大量 AI 调用可能超预算 | 每日预算限制 + 告警 |
| 平台封禁数据采集 | 用户账号可能被封 | 速率限制 + 用户告知 |
| 缓存别名累积错误映射 | 匹配准确率下降 | 定期重新验证 + 用户反馈按钮 |

### 13.4 延后项

| 项目 | 延后原因 |
|------|----------|
| 15个AI模块（采购计划、合规检查、市场情报等） | 先测量5个核心模块的采用率 |
| 完整 RAG 知识库 | 不阻塞核心比价价值 |
| NMPA 数据集成 | 非用户侧必须功能 |
| 服务端采集替代方案 | Chrome 扩展现有资产可继续使用 |
| Vue 2 框架迁移 | 与架构重构无关，独立评估 |
| UI 规范文档 | 后端架构先行，UI 规范作为配套文档单独编写 |
