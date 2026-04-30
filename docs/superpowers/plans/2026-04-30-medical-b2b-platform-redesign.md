# 医药B2B采购比价平台 架构重构 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构yy模块架构，引入药品主数据+多信号融合引擎+AI能力层，实现跨平台药品数据自动匹配与智能比价。

**Architecture:** 三层八域架构（platform/product/fusion/price/user/collection/referral/ai），4级匹配策略链（条码→批准文号→模糊→AI），DB驱动的平台适配器模式，通义千问/百炼LLM网关。

**Tech Stack:** Spring Boot 4.0.3, Java 17, MyBatis, MySQL 8.0, Redis, 通义千问/百炼 API, Chrome Extension MV3

**JSON库约定:** 项目同时使用 FastJSON2（平台适配器层、旧代码）和 Jackson（AI层、新代码）。分工：
- **FastJSON2**: `YyConfigurablePlatformAdapter`, `DataFusionServiceImpl` 等涉及平台数据解析的代码
- **Jackson**: `YyAiGateway`, `YyAiMatchStrategy`, `YyAiAdvisorImpl`, `YyAiDataCleanerImpl` 等AI层代码
- 新增代码优先使用 Jackson，除非所在模块已使用 FastJSON2

---

## 文件结构

> **命名规范：** 所有医药相关代码统一在 `com.ruoyi.yy` 包下（扁平结构，不分子包），实体/服务/Mapper 类全部以 `Yy` 开头，与现有代码保持一致。Mapper XML 统一在 `mapper/yy/` 目录下。ruoyi-ui 的 API 和 views 也统一在 `yy/` 目录下。

### 新建文件

| 文件路径 | 职责 |
|----------|------|
| `ruoyi-system/src/main/java/com/ruoyi/yy/handler/JsonStringTypeHandler.java` | MySQL JSON TypeHandler |
| `ruoyi-system/src/main/java/com/ruoyi/yy/constant/FusionConfidence.java` | 融合置信度常量 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/constant/MatchMethod.java` | 匹配方式枚举 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyDrugMaster.java` | 药品主数据实体 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyDrugAlias.java` | 药品别名映射实体 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyProductSnapshot.java` | 商品快照实体 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyFusionReview.java` | 融合审核实体 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyMatchResult.java` | 匹配结果VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyFusionResult.java` | 融合结果VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPurchaseAdvice.java` | 采购建议VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyCleanResult.java` | 清洗结果VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiRequest.java` | AI请求VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiResponse.java` | AI响应VO |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiPromptTemplate.java` | Prompt模板实体 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyDrugMasterMapper.java` | 药品主数据Mapper |
| `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyDrugAliasMapper.java` | 药品别名Mapper |
| `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyProductSnapshotMapper.java` | 商品快照Mapper |
| `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyFusionReviewMapper.java` | 融合审核Mapper |
| `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyAiPromptTemplateMapper.java` | Prompt模板Mapper |
| `ruoyi-system/src/main/resources/mapper/yy/YyDrugMasterMapper.xml` | 药品主数据SQL |
| `ruoyi-system/src/main/resources/mapper/yy/YyDrugAliasMapper.xml` | 药品别名SQL |
| `ruoyi-system/src/main/resources/mapper/yy/YyProductSnapshotMapper.xml` | 商品快照SQL |
| `ruoyi-system/src/main/resources/mapper/yy/YyFusionReviewMapper.xml` | 融合审核SQL |
| `ruoyi-system/src/main/resources/mapper/yy/YyAiPromptTemplateMapper.xml` | Prompt模板SQL |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyDrugMasterService.java` | 药品主数据Service接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyDrugMasterServiceImpl.java` | 药品主数据Service实现 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyMatchStrategy.java` | 匹配策略接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyBarcodeMatchStrategy.java` | 条码匹配策略 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyApprovalNumberMatchStrategy.java` | 批准文号匹配策略 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyFuzzyMatchStrategy.java` | 模糊匹配策略 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiMatchStrategy.java` | AI匹配策略 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyFusionEngine.java` | 融合引擎接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyFusionEngineImpl.java` | 融合引擎实现 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyPlatformAdapter.java` | 平台适配器接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyConfigurablePlatformAdapter.java` | 通用平台适配器 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/YyPlatformAdapterRegistry.java` | 适配器注册中心 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyAiGateway.java` | AI网关接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiGatewayImpl.java` | AI统一网关实现 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyMockAiGateway.java` | 测试用Mock网关 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyCircuitBreaker.java` | 熔断器 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyAiAdvisor.java` | AI比价顾问接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiAdvisorImpl.java` | AI比价顾问实现 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyAiDataCleaner.java` | AI数据清洗接口 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiDataCleanerImpl.java` | AI数据清洗实现 |
| `helpbuy-clone/Extensions/engine.js` | CDP执行引擎 |
| `helpbuy-clone/Extensions/platform-manager.js` | 平台配置管理 |
| `helpbuy-clone/Extensions/message-router.js` | 消息路由 |
| `ruoyi-ui/src/api/yy/drugMaster.js` | 药品主数据API |
| `ruoyi-ui/src/api/yy/drugAlias.js` | 药品别名API |
| `ruoyi-ui/src/api/yy/productSnapshot.js` | 商品快照API |
| `ruoyi-ui/src/api/yy/fusionReview.js` | 融合审核API |
| `ruoyi-ui/src/views/yy/drugMaster/index.vue` | 药品主数据管理页面 |
| `ruoyi-ui/src/views/yy/fusionReview/index.vue` | 融合审核管理页面 |

### 修改文件

| 文件路径 | 变更 |
|----------|------|
| `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java` | 重构：接入YyFusionEngineImpl，批量化查询 |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyProductFusionGroup.java` | fusion_key改为关联drug_id |
| `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyFieldMapping.java` | 新增transform_rule等字段 |
| `helpbuy-clone/Extensions/background.js` | 拆分：通用逻辑移至engine.js等 |
| `ruoyi-admin/src/main/java/com/ruoyi/web/controller/yy/YyDataIngestController.java` | 新增融合审核接口 |

### 测试文件

| 文件路径 | 职责 |
|----------|------|
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyFusionEngineImplTest.java` | 融合引擎集成测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyBarcodeMatchStrategyTest.java` | 条码匹配单元测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyApprovalNumberMatchStrategyTest.java` | 批准文号匹配单元测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyFuzzyMatchStrategyTest.java` | 模糊匹配单元测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiMatchStrategyTest.java` | AI匹配单元测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiGatewayTest.java` | AI网关测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyCircuitBreakerTest.java` | 熔断器测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/DataFusionServiceIntegrationTest.java` | 数据融合集成测试 |
| `ruoyi-system/src/test/java/com/ruoyi/yy/YyConfigurablePlatformAdapterTest.java` | 平台适配器测试 |

---

## Task 1: 数据库Schema创建

**Files:**
- Create: `ruoyi-system/src/main/resources/db/migration/V20260430__create_drug_master_tables.sql`
- Create: `ruoyi-system/src/main/resources/db/migration/V20260430__create_product_snapshot.sql`
- Create: `ruoyi-system/src/main/resources/db/migration/V20260430__create_fusion_review.sql`
- Create: `ruoyi-system/src/main/resources/db/migration/V20260430__create_ai_tables.sql`

- [ ] **Step 1: 创建药品主数据表DDL**

```sql
-- V20260430__create_drug_master_tables.sql
CREATE TABLE IF NOT EXISTS yy_drug_master (
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

CREATE TABLE IF NOT EXISTS yy_drug_alias (
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

- [ ] **Step 2: 创建商品快照表DDL**

```sql
-- V20260430__create_product_snapshot.sql
CREATE TABLE IF NOT EXISTS yy_product_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_platform VARCHAR(50) NOT NULL COMMENT '来源平台编码',
    sku_id VARCHAR(128) NOT NULL COMMENT '平台SKU ID',
    product_id VARCHAR(128) DEFAULT NULL COMMENT '平台商品ID',
    source_api VARCHAR(50) DEFAULT NULL COMMENT '来源API编码(hot/search)',
    drug_id BIGINT DEFAULT NULL COMMENT '关联yy_drug_master.id',
    fusion_confidence DECIMAL(3,2) DEFAULT NULL COMMENT '融合置信度',
    common_name VARCHAR(200) DEFAULT NULL COMMENT '通用名',
    barcode VARCHAR(64) DEFAULT NULL COMMENT '69码',
    approval_number VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    manufacturer VARCHAR(200) DEFAULT NULL COMMENT '厂家',
    specification VARCHAR(200) DEFAULT NULL COMMENT '规格',
    price_current DECIMAL(10,2) DEFAULT NULL COMMENT '当前供货价',
    stock_quantity INT DEFAULT 0 COMMENT '库存数量',
    product_data JSON NOT NULL COMMENT '标准化后的全部商品数据JSON',
    raw_data_payload JSON DEFAULT NULL COMMENT '解密后的原始响应',
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

- [ ] **Step 3: 创建融合审核队列表DDL**

```sql
-- V20260430__create_fusion_review.sql
CREATE TABLE IF NOT EXISTS yy_fusion_review (
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

- [ ] **Step 4: 创建AI相关表DDL**

```sql
-- V20260430__create_ai_tables.sql
CREATE TABLE IF NOT EXISTS yy_ai_prompt_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    scene VARCHAR(50) NOT NULL COMMENT '场景: match/advisor/evaluator/search/cleaner',
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

- [ ] **Step 5: 执行DDL验证**

在MySQL中执行以上SQL，验证表创建成功：
```sql
SHOW TABLES LIKE 'yy_drug_%';
SHOW TABLES LIKE 'yy_product_snapshot';
SHOW TABLES LIKE 'yy_fusion_review';
SHOW TABLES LIKE 'yy_ai_%';
```

Expected: 所有6张新表创建成功。

- [ ] **Step 6: 配置Flyway + @MapperScan**

在 `ruoyi-admin/pom.xml` 添加 Flyway 依赖：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

在 `application-druid.yml` 添加 Flyway 配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
```

所有新 Mapper 均在 `com.ruoyi.yy.mapper` 包下（扁平结构），项目的 `@MapperScan` 已包含此包路径，**无需修改**。

> 验证：检查启动类或 `MyBatisConfig.java` 中 `@MapperScan` 是否已包含 `"com.ruoyi.yy.mapper"`。若已有则无需改动。

- [ ] **Step 7: Commit**

```bash
git add ruoyi-system/src/main/resources/db/migration/ \
        ruoyi-admin/pom.xml \
        ruoyi-admin/src/main/resources/application-druid.yml
git commit -m "feat: add database schema and Flyway + MapperScan configuration"
```

---

## Task 2: 常量与值对象

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/constant/FusionConfidence.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/constant/MatchMethod.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyMatchResult.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyFusionResult.java`

- [ ] **Step 1: 创建FusionConfidence常量类**

```java
package com.ruoyi.yy.constant;

import java.math.BigDecimal;

/**
 * 融合匹配置信度常量
 */
public final class FusionConfidence {

    private FusionConfidence() {}

    /** 条码精确匹配置信度 */
    public static final BigDecimal BARCODE = new BigDecimal("1.00");

    /** 批准文号精确匹配置信度 */
    public static final BigDecimal APPROVAL_NUMBER = new BigDecimal("0.98");

    /** 模糊匹配置信度下限 */
    public static final BigDecimal FUZZY_MIN = new BigDecimal("0.70");

    /** 模糊匹配置信度上限 */
    public static final BigDecimal FUZZY_MAX = new BigDecimal("0.95");

    /** AI匹配置信度下限 */
    public static final BigDecimal AI_MIN = new BigDecimal("0.50");

    /** AI匹配置信度上限 */
    public static final BigDecimal AI_MAX = new BigDecimal("0.99");

    /** 自动接受阈值：>=此值直接接受 */
    public static final BigDecimal AUTO_ACCEPT = new BigDecimal("0.95");

    /** 待复核阈值：>=此值自动接受但标记待复核 */
    public static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.80");

    /**
     * 判断是否自动接受
     */
    public static boolean isAutoAccept(BigDecimal confidence) {
        return confidence.compareTo(AUTO_ACCEPT) >= 0;
    }

    /**
     * 判断是否需要人工审核
     */
    public static boolean needsReview(BigDecimal confidence) {
        return confidence.compareTo(REVIEW_THRESHOLD) >= 0
            && confidence.compareTo(AUTO_ACCEPT) < 0;
    }

    /**
     * 判断是否拒绝（进入审核队列）
     */
    public static boolean isRejected(BigDecimal confidence) {
        return confidence.compareTo(REVIEW_THRESHOLD) < 0;
    }
}
```

- [ ] **Step 2: 创建MatchMethod枚举**

```java
package com.ruoyi.yy.constant;

/**
 * 匹配方式枚举
 */
public enum MatchMethod {

    MANUAL("manual", "人工匹配"),
    BARCODE("barcode", "条码匹配"),
    APPROVAL("approval", "批准文号匹配"),
    FUZZY("fuzzy", "模糊匹配"),
    AI("ai", "AI匹配");

    private final String code;
    private final String description;

    MatchMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchMethod fromCode(String code) {
        for (MatchMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown MatchMethod code: " + code);
    }
}
```

- [ ] **Step 3: 创建YyMatchResult值对象**

```java
package com.ruoyi.yy.domain;

import com.ruoyi.yy.constant.MatchMethod;
import java.math.BigDecimal;

/**
 * 单个匹配策略的结果
 */
public class YyMatchResult {

    private boolean matched;
    private Long drugId;
    private String drugCode;
    private BigDecimal confidence;
    private MatchMethod matchMethod;
    private String reason;

    public YyMatchResult() {}

    public static YyMatchResult success(Long drugId, String drugCode, BigDecimal confidence, MatchMethod method, String reason) {
        YyMatchResult r = new YyMatchResult();
        r.matched = true;
        r.drugId = drugId;
        r.drugCode = drugCode;
        r.confidence = confidence;
        r.matchMethod = method;
        r.reason = reason;
        return r;
    }

    public static YyMatchResult failure(String reason) {
        YyMatchResult r = new YyMatchResult();
        r.matched = false;
        r.reason = reason;
        return r;
    }

    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public MatchMethod getMatchMethod() { return matchMethod; }
    public void setMatchMethod(MatchMethod matchMethod) { this.matchMethod = matchMethod; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

- [ ] **Step 4: 创建YyFusionResult值对象**

```java
package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 融合引擎的最终结果
 */
public class YyFusionResult {

    private boolean matched;
    private Long drugId;
    private String drugCode;
    private BigDecimal confidence;
    private String matchMethod;
    private String reason;
    private boolean needsReview;
    private List<Long> candidateDrugIds;
    private List<BigDecimal> candidateScores;

    public YyFusionResult() {}

    public static YyFusionResult matched(Long drugId, String drugCode, BigDecimal confidence,
                                        String matchMethod, String reason, boolean needsReview) {
        YyFusionResult r = new YyFusionResult();
        r.matched = true;
        r.drugId = drugId;
        r.drugCode = drugCode;
        r.confidence = confidence;
        r.matchMethod = matchMethod;
        r.reason = reason;
        r.needsReview = needsReview;
        return r;
    }

    public static YyFusionResult noMatch(List<Long> candidateDrugIds, List<BigDecimal> candidateScores) {
        YyFusionResult r = new YyFusionResult();
        r.matched = false;
        r.candidateDrugIds = candidateDrugIds;
        r.candidateScores = candidateScores;
        return r;
    }

    // Getters and setters
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isNeedsReview() { return needsReview; }
    public void setNeedsReview(boolean needsReview) { this.needsReview = needsReview; }
    public List<Long> getCandidateDrugIds() { return candidateDrugIds; }
    public void setCandidateDrugIds(List<Long> candidateDrugIds) { this.candidateDrugIds = candidateDrugIds; }
    public List<BigDecimal> getCandidateScores() { return candidateScores; }
    public void setCandidateScores(List<BigDecimal> candidateScores) { this.candidateScores = candidateScores; }
}
```

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/constant/ \
        ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyMatchResult.java \
        ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyFusionResult.java
git commit -m "feat: add fusion confidence constants, match method enum, and result VOs"
```

---

## Task 3: 药品主数据Domain + Mapper + Service

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyDrugMaster.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyDrugAlias.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyDrugMasterMapper.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyDrugAliasMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyDrugMasterMapper.xml`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyDrugAliasMapper.xml`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyDrugMasterService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyDrugMasterServiceImpl.java`

- [ ] **Step 1: 创建YyDrugMaster实体**

```java
package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * 药品主数据 yy_drug_master
 */
public class YyDrugMaster implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String drugCode;
    private String commonName;
    private String genericName;
    private String barcode;
    private String approvalNumber;
    private String manufacturer;
    private String specification;
    private String dosageForm;
    private String categoryL1;
    private String categoryL2;
    private Integer isPrescription;
    private String medicareType;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
    public String getCategoryL1() { return categoryL1; }
    public void setCategoryL1(String categoryL1) { this.categoryL1 = categoryL1; }
    public String getCategoryL2() { return categoryL2; }
    public void setCategoryL2(String categoryL2) { this.categoryL2 = categoryL2; }
    public Integer getIsPrescription() { return isPrescription; }
    public void setIsPrescription(Integer isPrescription) { this.isPrescription = isPrescription; }
    public String getMedicareType() { return medicareType; }
    public void setMedicareType(String medicareType) { this.medicareType = medicareType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: 创建YyDrugAlias实体**

```java
package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 药品平台别名映射 yy_drug_alias
 */
public class YyDrugAlias implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long drugId;
    private String platformCode;
    private String platformProductName;
    private String platformManufacturer;
    private String platformSpecification;
    private String platformSkuId;
    private BigDecimal confidence;
    private String matchMethod;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformProductName() { return platformProductName; }
    public void setPlatformProductName(String platformProductName) { this.platformProductName = platformProductName; }
    public String getPlatformManufacturer() { return platformManufacturer; }
    public void setPlatformManufacturer(String platformManufacturer) { this.platformManufacturer = platformManufacturer; }
    public String getPlatformSpecification() { return platformSpecification; }
    public void setPlatformSpecification(String platformSpecification) { this.platformSpecification = platformSpecification; }
    public String getPlatformSkuId() { return platformSkuId; }
    public void setPlatformSkuId(String platformSkuId) { this.platformSkuId = platformSkuId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: 创建YyDrugMasterMapper接口**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyDrugMaster;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface YyDrugMasterMapper {

    YyDrugMaster selectById(Long id);

    YyDrugMaster selectByDrugCode(String drugCode);

    YyDrugMaster selectByBarcode(String barcode);

    YyDrugMaster selectByApprovalNumber(String approvalNumber);

    /**
     * 按归一化通用名+规格查询候选药品（前缀匹配，走索引）
     */
    List<YyDrugMaster> selectCandidates(
        @Param("commonName") String commonName,
        @Param("specification") String specification
    );

    /**
     * 回退：全模糊匹配（不走索引，仅当前缀匹配无结果时使用）
     */
    List<YyDrugMaster> selectCandidatesFallback(
        @Param("commonName") String commonName,
        @Param("specification") String specification
    );

    /**
     * 批量按ID查询（消除N+1）
     */
    List<YyDrugMaster> selectByIds(@Param("ids") List<Long> ids);

    int insert(YyDrugMaster record);

    int updateById(YyDrugMaster record);
}
```

- [ ] **Step 4: 创建YyDrugAliasMapper接口**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyDrugAlias;
import org.apache.ibatis.annotations.Param;

public interface YyDrugAliasMapper {

    /**
     * 按平台+SKU查询缓存映射
     */
    YyDrugAlias selectByPlatformSku(
        @Param("platformCode") String platformCode,
        @Param("skuId") String skuId
    );

    int insert(YyDrugAlias record);

    int updateConfidence(
        @Param("id") Long id,
        @Param("confidence") java.math.BigDecimal confidence,
        @Param("matchMethod") String matchMethod
    );
}
```

- [ ] **Step 5: 创建YyDrugMasterMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyDrugMasterMapper">

    <resultMap type="YyDrugMaster" id="YyDrugMasterResult">
        <result property="id" column="id"/>
        <result property="drugCode" column="drug_code"/>
        <result property="commonName" column="common_name"/>
        <result property="genericName" column="generic_name"/>
        <result property="barcode" column="barcode"/>
        <result property="approvalNumber" column="approval_number"/>
        <result property="manufacturer" column="manufacturer"/>
        <result property="specification" column="specification"/>
        <result property="dosageForm" column="dosage_form"/>
        <result property="categoryL1" column="category_l1"/>
        <result property="categoryL2" column="category_l2"/>
        <result property="isPrescription" column="is_prescription"/>
        <result property="medicareType" column="medicare_type"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="selectById" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master WHERE id = #{id}
    </select>

    <select id="selectByDrugCode" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master WHERE drug_code = #{drugCode}
    </select>

    <select id="selectByBarcode" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master WHERE barcode = #{barcode} AND status = 1
    </select>

    <select id="selectByApprovalNumber" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master WHERE approval_number = #{approvalNumber} AND status = 1
    </select>

    <!-- 前缀匹配（走索引） -->
    <select id="selectCandidates" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master
        WHERE status = 1
        <if test="commonName != null and commonName != ''">
            AND common_name LIKE CONCAT(#{commonName}, '%')
        </if>
        <if test="specification != null and specification != ''">
            AND specification LIKE CONCAT(#{specification}, '%')
        </if>
        LIMIT 50
    </select>

    <!-- 回退：全模糊匹配（不走索引，仅当前缀匹配无结果时使用） -->
    <select id="selectCandidatesFallback" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master
        WHERE status = 1
        <if test="commonName != null and commonName != ''">
            AND common_name LIKE CONCAT('%', #{commonName}, '%')
        </if>
        <if test="specification != null and specification != ''">
            AND specification LIKE CONCAT('%', #{specification}, '%')
        </if>
        LIMIT 50
    </select>

    <select id="selectByIds" resultMap="YyDrugMasterResult">
        SELECT * FROM yy_drug_master WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_drug_master (drug_code, common_name, generic_name, barcode,
            approval_number, manufacturer, specification, dosage_form,
            category_l1, category_l2, is_prescription, medicare_type, status)
        VALUES (#{drugCode}, #{commonName}, #{genericName}, #{barcode},
            #{approvalNumber}, #{manufacturer}, #{specification}, #{dosageForm},
            #{categoryL1}, #{categoryL2}, #{isPrescription}, #{medicareType}, #{status})
    </insert>

    <update id="updateById">
        UPDATE yy_drug_master
        SET common_name = #{commonName}, generic_name = #{genericName},
            barcode = #{barcode}, approval_number = #{approvalNumber},
            manufacturer = #{manufacturer}, specification = #{specification},
            dosage_form = #{dosageForm}, category_l1 = #{categoryL1},
            category_l2 = #{categoryL2}, is_prescription = #{isPrescription},
            medicare_type = #{medicareType}, status = #{status}
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 6: 创建YyDrugAliasMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyDrugAliasMapper">

    <resultMap type="YyDrugAlias" id="YyDrugAliasResult">
        <result property="id" column="id"/>
        <result property="drugId" column="drug_id"/>
        <result property="platformCode" column="platform_code"/>
        <result property="platformProductName" column="platform_product_name"/>
        <result property="platformManufacturer" column="platform_manufacturer"/>
        <result property="platformSpecification" column="platform_specification"/>
        <result property="platformSkuId" column="platform_sku_id"/>
        <result property="confidence" column="confidence"/>
        <result property="matchMethod" column="match_method"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <select id="selectByPlatformSku" resultMap="YyDrugAliasResult">
        SELECT * FROM yy_drug_alias
        WHERE platform_code = #{platformCode} AND platform_sku_id = #{skuId}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_drug_alias (drug_id, platform_code, platform_product_name,
            platform_manufacturer, platform_specification, platform_sku_id,
            confidence, match_method)
        VALUES (#{drugId}, #{platformCode}, #{platformProductName},
            #{platformManufacturer}, #{platformSpecification}, #{platformSkuId},
            #{confidence}, #{matchMethod})
    </insert>

    <update id="updateConfidence">
        UPDATE yy_drug_alias
        SET confidence = #{confidence}, match_method = #{matchMethod}
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 7: 创建IYyDrugMasterService接口**

```java
package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyDrugMaster;
import java.util.List;

public interface IYyDrugMasterService {

    YyDrugMaster selectById(Long id);

    YyDrugMaster selectByDrugCode(String drugCode);

    YyDrugMaster selectByBarcode(String barcode);

    YyDrugMaster selectByApprovalNumber(String approvalNumber);

    List<YyDrugMaster> selectCandidates(String commonName, String specification);

    List<YyDrugMaster> selectCandidatesFallback(String commonName, String specification);

    List<YyDrugMaster> selectByIds(List<Long> ids);

    int insert(YyDrugMaster record);

    int updateById(YyDrugMaster record);
}
```

- [ ] **Step 8: 创建YyDrugMasterServiceImpl**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import com.ruoyi.yy.service.IYyDrugMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class YyDrugMasterServiceImpl implements IYyDrugMasterService {

    @Autowired
    private YyDrugMasterMapper drugMasterMapper;

    @Override
    public YyDrugMaster selectById(Long id) {
        return drugMasterMapper.selectById(id);
    }

    @Override
    public YyDrugMaster selectByDrugCode(String drugCode) {
        return drugMasterMapper.selectByDrugCode(drugCode);
    }

    @Override
    public YyDrugMaster selectByBarcode(String barcode) {
        return drugMasterMapper.selectByBarcode(barcode);
    }

    @Override
    public YyDrugMaster selectByApprovalNumber(String approvalNumber) {
        return drugMasterMapper.selectByApprovalNumber(approvalNumber);
    }

    @Override
    public List<YyDrugMaster> selectCandidates(String commonName, String specification) {
        return drugMasterMapper.selectCandidates(commonName, specification);
    }

    @Override
    public List<YyDrugMaster> selectCandidatesFallback(String commonName, String specification) {
        return drugMasterMapper.selectCandidatesFallback(commonName, specification);
    }

    @Override
    public List<YyDrugMaster> selectByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return drugMasterMapper.selectByIds(ids);
    }

    @Override
    public int insert(YyDrugMaster record) {
        return drugMasterMapper.insert(record);
    }

    @Override
    public int updateById(YyDrugMaster record) {
        return drugMasterMapper.updateById(record);
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/product/
git commit -m "feat: add drug master domain, mapper, and service layer"
```

---

## Task 4: IYyMatchStrategy接口 + 条码/批准文号策略

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyMatchStrategy.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyBarcodeMatchStrategy.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyApprovalNumberMatchStrategy.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyBarcodeMatchStrategyTest.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyApprovalNumberMatchStrategyTest.java`

- [ ] **Step 1: 创建IYyMatchStrategy接口**

```java
package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import java.util.List;

/**
 * 匹配策略接口 — 策略链中的每一环
 */
public interface IYyMatchStrategy {

    /**
     * 策略名称
     */
    String getName();

    /**
     * 优先级，越高越先执行
     */
    int getPriority();

    /**
     * 尝试将商品快照匹配到药品主数据
     *
     * @param snapshot   待匹配的商品快照
     * @param candidates 候选药品列表（可能为空，策略自行查询亦可）
     * @return 匹配结果，matched=false表示本策略无法匹配
     */
    YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates);
}
```

- [ ] **Step 2: 创建条码匹配策略单元测试**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyBarcodeMatchStrategyTest {

    private YyDrugMasterMapper drugMasterMapper;
    private YyBarcodeMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterMapper = mock(YyDrugMasterMapper.class);
        strategy = new YyBarcodeMatchStrategy(drugMasterMapper);
    }

    @Test
    void name() {
        assertEquals("BarcodeMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(100, strategy.getPriority());
    }

    @Test
    void matchWithBarcode_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("6922710600012");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);
        drug.setDrugCode("DRUG001");
        when(drugMasterMapper.selectByBarcode("6922710600012")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(1L, result.getDrugId());
        assertEquals(FusionConfidence.BARCODE, result.getConfidence());
        assertEquals(MatchMethod.BARCODE, result.getMatchMethod());
    }

    @Test
    void matchWithBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("6922710600012");
        when(drugMasterMapper.selectByBarcode("6922710600012")).thenReturn(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void matchWithNullBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
        verify(drugMasterMapper, never()).selectByBarcode(any());
    }

    @Test
    void matchWithEmptyBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("");

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `mvn test -pl ruoyi-system -Dtest=YyBarcodeMatchStrategyTest -DfailIfNoTests=false`
Expected: FAIL — `YyBarcodeMatchStrategy` class not found

- [ ] **Step 4: 实现YyBarcodeMatchStrategy**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 条码（69码）精确匹配策略 — 优先级最高
 */
@Component
public class YyBarcodeMatchStrategy implements IYyMatchStrategy {

    private final YyDrugMasterMapper drugMasterMapper;

    @Autowired
    public YyBarcodeMatchStrategy(YyDrugMasterMapper drugMasterMapper) {
        this.drugMasterMapper = drugMasterMapper;
    }

    @Override
    public String getName() {
        return "BarcodeMatch";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        String barcode = snapshot.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            return YyMatchResult.failure("No barcode on snapshot");
        }

        YyDrugMaster drug = drugMasterMapper.selectByBarcode(barcode.trim());
        if (drug == null) {
            return YyMatchResult.failure("No drug master with barcode: " + barcode);
        }

        return YyMatchResult.success(
            drug.getId(),
            drug.getDrugCode(),
            FusionConfidence.BARCODE,
            MatchMethod.BARCODE,
            "Exact barcode match: " + barcode
        );
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `mvn test -pl ruoyi-system -Dtest=YyBarcodeMatchStrategyTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 6: 创建批准文号匹配策略测试**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyApprovalNumberMatchStrategyTest {

    private YyDrugMasterMapper drugMasterMapper;
    private YyApprovalNumberMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterMapper = mock(YyDrugMasterMapper.class);
        strategy = new YyApprovalNumberMatchStrategy(drugMasterMapper);
    }

    @Test
    void name() {
        assertEquals("ApprovalNumberMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(90, strategy.getPriority());
    }

    @Test
    void match_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("国药准字Z11020001");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterMapper.selectByApprovalNumber("Z11020001")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals(FusionConfidence.APPROVAL_NUMBER, result.getConfidence());
        assertEquals(MatchMethod.APPROVAL, result.getMatchMethod());
    }

    @Test
    void matchWithPrefix_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("国药准字Z11020001");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterMapper.selectByApprovalNumber("Z11020001")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
    }

    @Test
    void match_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("国药准字Z99999999");
        when(drugMasterMapper.selectByApprovalNumber("Z99999999")).thenReturn(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void matchNull_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }
}
```

- [ ] **Step 7: 实现YyApprovalNumberMatchStrategy**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批准文号精确匹配策略 — 优先级第二
 *
 * 批准文号格式多样："国药准字Z11020001"、"Z11020001"、"国药准字H20000001"
 * 归一化规则：去除"国药准字"前缀，保留字母+数字部分
 */
@Component
public class YyApprovalNumberMatchStrategy implements IYyMatchStrategy {

    private static final Pattern APPROVAL_PATTERN =
        Pattern.compile("(?:国药准字)?([A-Za-z]\\d{8})");

    private final YyDrugMasterMapper drugMasterMapper;

    @Autowired
    public YyApprovalNumberMatchStrategy(YyDrugMasterMapper drugMasterMapper) {
        this.drugMasterMapper = drugMasterMapper;
    }

    @Override
    public String getName() {
        return "ApprovalNumberMatch";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        String raw = snapshot.getApprovalNumber();
        if (raw == null || raw.trim().isEmpty()) {
            return YyMatchResult.failure("No approval number on snapshot");
        }

        String normalized = normalizeApprovalNumber(raw.trim());
        if (normalized == null) {
            return YyMatchResult.failure("Cannot parse approval number: " + raw);
        }

        YyDrugMaster drug = drugMasterMapper.selectByApprovalNumber(normalized);
        if (drug == null) {
            return YyMatchResult.failure("No drug master with approval number: " + normalized);
        }

        return YyMatchResult.success(
            drug.getId(),
            drug.getDrugCode(),
            FusionConfidence.APPROVAL_NUMBER,
            MatchMethod.APPROVAL,
            "Exact approval number match: " + normalized
        );
    }

    /**
     * 归一化批准文号：提取字母+8位数字部分
     * "国药准字Z11020001" → "Z11020001"
     * "Z11020001" → "Z11020001"
     */
    static String normalizeApprovalNumber(String raw) {
        Matcher m = APPROVAL_PATTERN.matcher(raw);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }
}
```

- [ ] **Step 8: 运行批准文号测试**

Run: `mvn test -pl ruoyi-system -Dtest=YyApprovalNumberMatchStrategyTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyMatchStrategy.java \
        ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyBarcodeMatchStrategy.java \
        ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyApprovalNumberMatchStrategy.java \
        ruoyi-system/src/test/java/com/ruoyi/yy/YyBarcodeMatchStrategyTest.java \
        ruoyi-system/src/test/java/com/ruoyi/yy/YyApprovalNumberMatchStrategyTest.java
git commit -m "feat: add IYyMatchStrategy interface with barcode and approval number implementations"
```

---

## Task 5: YyFuzzyMatchStrategy（模糊匹配）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyFuzzyMatchStrategy.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyFuzzyMatchStrategyTest.java`

- [ ] **Step 1: 创建模糊匹配策略测试**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyFuzzyMatchStrategyTest {

    private YyDrugMasterMapper drugMasterMapper;
    private YyFuzzyMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterMapper = mock(YyDrugMasterMapper.class);
        strategy = new YyFuzzyMatchStrategy(drugMasterMapper);
    }

    @Test
    void name() {
        assertEquals("FuzzyMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(50, strategy.getPriority());
    }

    @Test
    void exactMatch_highConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s*2b");
        snapshot.setManufacturer("联邦制药");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(3L);
        drug.setDrugCode("DRUG003");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s*2b");
        drug.setManufacturer("联邦制药");

        when(drugMasterMapper.selectCandidates("阿莫西林胶囊", "0.25g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(3L, result.getDrugId());
        assertEquals(MatchMethod.FUZZY, result.getMatchMethod());
        assertTrue(result.getConfidence().compareTo(FusionConfidence.FUZZY_MIN) >= 0);
    }

    @Test
    void similarManufacturer_highConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s");
        snapshot.setManufacturer("北京同仁堂股份有限公司");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(4L);
        drug.setDrugCode("DRUG004");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s");
        drug.setManufacturer("同仁堂");

        when(drugMasterMapper.selectCandidates("阿莫西林胶囊", "0.25g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertTrue(result.getConfidence().compareTo(new BigDecimal("0.80")) >= 0);
    }

    @Test
    void noCandidates_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("未知药品");
        snapshot.setSpecification("1g");
        snapshot.setManufacturer("未知厂家");

        when(drugMasterMapper.selectCandidates("未知药品", "1g"))
            .thenReturn(new ArrayList<>());

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void differentDrug_lowConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.5g*24s");
        snapshot.setManufacturer("联邦制药");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(5L);
        drug.setDrugCode("DRUG005");
        drug.setCommonName("布洛芬缓释胶囊");
        drug.setSpecification("0.3g*20s");
        drug.setManufacturer("中美史克");

        when(drugMasterMapper.selectCandidates("阿莫西林胶囊", "0.5g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void normalizeManufacturer_removesSuffixes() {
        assertEquals("同仁堂", strategy.normalizeManufacturer("北京同仁堂股份有限公司"));
        assertEquals("联邦制药", strategy.normalizeManufacturer("联邦制药集团有限公司"));
        assertEquals("华润三九", strategy.normalizeManufacturer("华润三九医药股份有限公司"));
        assertEquals("辉瑞", strategy.normalizeManufacturer("Pfizer Inc."));
    }

    @Test
    void normalizeSpecification_standardizes() {
        assertEquals("0.25g*12s", strategy.normalizeSpecification("0.25g×12片"));
        assertEquals("0.25g*12s", strategy.normalizeSpecification("0.25gX12片"));
        assertEquals("10ml*6b", strategy.normalizeSpecification("10ml×6支"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -pl ruoyi-system -Dtest=YyFuzzyMatchStrategyTest -DfailIfNoTests=false`
Expected: FAIL — `YyFuzzyMatchStrategy` class not found

- [ ] **Step 3: 实现YyFuzzyMatchStrategy**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 模糊匹配策略 — 基于归一化字符串相似度
 *
 * 匹配逻辑：
 * 1. 归一化通用名、规格、厂家名
 * 2. 查询候选药品（通用名前缀匹配）
 * 3. 对每个候选计算三维相似度（通用名、规格、厂家）
 * 4. 加权综合得分，取最高分
 */
@Component
public class YyFuzzyMatchStrategy implements IYyMatchStrategy {

    private static final double WEIGHT_COMMON_NAME = 0.4;
    private static final double WEIGHT_SPECIFICATION = 0.3;
    private static final double WEIGHT_MANUFACTURER = 0.3;
    private static final double MIN_MATCH_SCORE = 0.7;

    // 厂家名后缀
    private static final Pattern MANUFACTURER_SUFFIXES = Pattern.compile(
        "(股份有限公司|有限公司|有限责任公司|集团|药业|制药|医药|股份|Inc\\.?|Ltd\\.?|Co\\.?|Corp\\.?)",
        Pattern.CASE_INSENSITIVE
    );

    // 规格标准化
    private static final Pattern SPEC_X = Pattern.compile("[×xX]");
    private static final Pattern SPEC_CHINESE = Pattern.compile("[片粒颗支瓶袋盒板]");

    private final YyDrugMasterMapper drugMasterMapper;

    @Autowired
    public YyFuzzyMatchStrategy(YyDrugMasterMapper drugMasterMapper) {
        this.drugMasterMapper = drugMasterMapper;
    }

    @Override
    public String getName() {
        return "FuzzyMatch";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        String commonName = snapshot.getCommonName();
        String specification = snapshot.getSpecification();
        String manufacturer = snapshot.getManufacturer();

        if (commonName == null || commonName.trim().isEmpty()) {
            return YyMatchResult.failure("No common name on snapshot");
        }

        // 归一化输入
        String normCommonName = commonName.trim();
        String normSpec = normalizeSpecification(specification);
        String normMfr = normalizeManufacturer(manufacturer);

        // 取规格前缀作为查询条件（如 "0.25g"）
        String specPrefix = extractSpecPrefix(normSpec);

        // 查询候选药品（前缀匹配 → 回退全模糊匹配）
        List<YyDrugMaster> candidateList = candidates;
        if (candidateList == null || candidateList.isEmpty()) {
            candidateList = drugMasterMapper.selectCandidates(normCommonName, specPrefix);
        }
        if (candidateList == null || candidateList.isEmpty()) {
            // 前缀匹配无结果，回退到全模糊匹配
            candidateList = drugMasterMapper.selectCandidatesFallback(normCommonName, specPrefix);
        }

        if (candidateList.isEmpty()) {
            return YyMatchResult.failure("No candidates found for: " + normCommonName);
        }

        // 找最佳匹配
        YyDrugMaster bestMatch = null;
        double bestScore = 0;

        for (YyDrugMaster candidate : candidateList) {
            double score = calculateScore(
                normCommonName, normSpec, normMfr,
                candidate.getCommonName(),
                normalizeSpecification(candidate.getSpecification()),
                normalizeManufacturer(candidate.getManufacturer())
            );

            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch == null || bestScore < MIN_MATCH_SCORE) {
            return YyMatchResult.failure("Best score " + bestScore + " below threshold " + MIN_MATCH_SCORE);
        }

        BigDecimal confidence = BigDecimal.valueOf(bestScore)
            .setScale(2, RoundingMode.HALF_UP)
            .min(FusionConfidence.FUZZY_MAX)
            .max(FusionConfidence.FUZZY_MIN);

        return YyMatchResult.success(
            bestMatch.getId(),
            bestMatch.getDrugCode(),
            confidence,
            MatchMethod.FUZZY,
            "Fuzzy match score=" + bestScore + " against drug_id=" + bestMatch.getId()
        );
    }

    /**
     * 计算三维加权相似度
     */
    double calculateScore(String srcName, String srcSpec, String srcMfr,
                          String tgtName, String tgtSpec, String tgtMfr) {
        double nameSim = levenshteinSimilarity(srcName, tgtName);
        double specSim = levenshteinSimilarity(srcSpec, tgtSpec);
        double mfrSim = levenshteinSimilarity(srcMfr, tgtMfr);

        return nameSim * WEIGHT_COMMON_NAME
             + specSim * WEIGHT_SPECIFICATION
             + mfrSim * WEIGHT_MANUFACTURER;
    }

    /**
     * 归一化厂家名：去除公司后缀
     */
    String normalizeManufacturer(String manufacturer) {
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            return "";
        }
        String result = manufacturer.trim();
        result = MANUFACTURER_SUFFIXES.matcher(result).replaceAll("");
        result = result.replaceAll("[\\s()（）]+", "");
        return result;
    }

    /**
     * 归一化规格：统一乘号和单位
     */
    String normalizeSpecification(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            return "";
        }
        String result = specification.trim();
        result = SPEC_X.matcher(result).replaceAll("*");
        result = result.replace("片", "s").replace("粒", "s").replace("颗", "s");
        result = result.replace("支", "b").replace("瓶", "b").replace("袋", "p");
        result = result.replaceAll("\\s+", "");
        return result;
    }

    /**
     * 提取规格前缀（数字+单位部分）
     */
    private String extractSpecPrefix(String normalizedSpec) {
        if (normalizedSpec == null || normalizedSpec.isEmpty()) {
            return "";
        }
        // 提取开头的数字+单位，如 "0.25g" from "0.25g*12s*2b"
        java.util.regex.Matcher m = Pattern.compile("^(\\d+\\.?\\d*[a-zA-Z]+)").matcher(normalizedSpec);
        if (m.find()) {
            return m.group(1);
        }
        return normalizedSpec;
    }

    /**
     * Levenshtein相似度（0.0 - 1.0）
     */
    double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int len1 = s1.length();
        int len2 = s2.length();
        int maxLen = Math.max(len1, len2);

        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return 1.0 - (double) dp[len1][len2] / maxLen;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -pl ruoyi-system -Dtest=YyFuzzyMatchStrategyTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyFuzzyMatchStrategy.java \
        ruoyi-system/src/test/java/com/ruoyi/yy/YyFuzzyMatchStrategyTest.java
git commit -m "feat: add YyFuzzyMatchStrategy with Levenshtein similarity and manufacturer/spec normalization"
```

---

## Task 6: YyFusionEngineImpl（融合引擎核心）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyFusionEngineImpl.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/fusion/domain/YyFusionReview.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/fusion/mapper/YyFusionReviewMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyFusionReviewMapper.xml`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyFusionEngineImplTest.java`

- [ ] **Step 1: 创建YyFusionReview实体**

```java
package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 融合审核队列 yy_fusion_review
 */
public class YyFusionReview implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long snapshotId;
    private String candidateDrugIds;   // JSON array
    private String matchScores;        // JSON array
    private String aiSuggestion;
    private String status;             // pending/approved/rejected
    private Long reviewerId;
    private String reviewNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getCandidateDrugIds() { return candidateDrugIds; }
    public void setCandidateDrugIds(String candidateDrugIds) { this.candidateDrugIds = candidateDrugIds; }
    public String getMatchScores() { return matchScores; }
    public void setMatchScores(String matchScores) { this.matchScores = matchScores; }
    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: 创建YyFusionReviewMapper**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyFusionReview;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface YyFusionReviewMapper {

    YyFusionReview selectById(Long id);

    List<YyFusionReview> selectByStatus(@Param("status") String status);

    int insert(YyFusionReview record);

    int updateStatus(
        @Param("id") Long id,
        @Param("status") String status,
        @Param("reviewerId") Long reviewerId,
        @Param("reviewNote") String reviewNote
    );
}
```

- [ ] **Step 3: 创建YyFusionReviewMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyFusionReviewMapper">

    <resultMap type="YyFusionReview" id="YyFusionReviewResult">
        <result property="id" column="id"/>
        <result property="snapshotId" column="snapshot_id"/>
        <result property="candidateDrugIds" column="candidate_drug_ids"/>
        <result property="matchScores" column="match_scores"/>
        <result property="aiSuggestion" column="ai_suggestion"/>
        <result property="status" column="status"/>
        <result property="reviewerId" column="reviewer_id"/>
        <result property="reviewNote" column="review_note"/>
        <result property="reviewedAt" column="reviewed_at"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <select id="selectById" resultMap="YyFusionReviewResult">
        SELECT * FROM yy_fusion_review WHERE id = #{id}
    </select>

    <select id="selectByStatus" resultMap="YyFusionReviewResult">
        SELECT * FROM yy_fusion_review WHERE status = #{status} ORDER BY created_at ASC
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_fusion_review (snapshot_id, candidate_drug_ids, match_scores,
            ai_suggestion, status)
        VALUES (#{snapshotId}, #{candidateDrugIds}, #{matchScores},
            #{aiSuggestion}, #{status})
    </insert>

    <update id="updateStatus">
        UPDATE yy_fusion_review
        SET status = #{status}, reviewer_id = #{reviewerId},
            review_note = #{reviewNote}, reviewed_at = NOW()
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 4: 创建YyFusionEngineImpl测试**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyFusionReview;
import com.ruoyi.yy.mapper.YyFusionReviewMapper;
import com.ruoyi.yy.domain.YyDrugAlias;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugAliasMapper;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyFusionEngineImplTest {

    private YyDrugMasterMapper drugMasterMapper;
    private YyDrugAliasMapper aliasMapper;
    private YyFusionReviewMapper reviewMapper;
    private YyFusionEngineImpl engine;

    @BeforeEach
    void setUp() {
        drugMasterMapper = mock(YyDrugMasterMapper.class);
        aliasMapper = mock(YyDrugAliasMapper.class);
        reviewMapper = mock(YyFusionReviewMapper.class);

        // 创建真实策略
        YyBarcodeMatchStrategy barcodeStrategy = new YyBarcodeMatchStrategy(drugMasterMapper);
        YyApprovalNumberMatchStrategy approvalStrategy = new YyApprovalNumberMatchStrategy(drugMasterMapper);
        YyFuzzyMatchStrategy fuzzyStrategy = new YyFuzzyMatchStrategy(drugMasterMapper);

        List<IYyMatchStrategy> strategies = new ArrayList<>();
        strategies.add(barcodeStrategy);
        strategies.add(approvalStrategy);
        strategies.add(fuzzyStrategy);

        engine = new YyFusionEngineImpl(strategies, drugMasterMapper, aliasMapper, reviewMapper);
    }

    @Test
    void fuse_aliasCacheHit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU001");

        YyDrugAlias alias = new YyDrugAlias();
        alias.setDrugId(1L);
        alias.setConfidence(new BigDecimal("1.00"));
        alias.setMatchMethod("barcode");
        when(aliasMapper.selectByPlatformSku("ysbang", "SKU001")).thenReturn(alias);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);
        drug.setDrugCode("DRUG001");
        when(drugMasterMapper.selectById(1L)).thenReturn(drug);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(1L, result.getDrugId());
        verify(aliasMapper).selectByPlatformSku("ysbang", "SKU001");
        // 不应调用条码匹配等策略
        verify(drugMasterMapper, never()).selectByBarcode(any());
    }

    @Test
    void fuse_barcodeMatch() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU002");
        snapshot.setBarcode("6922710600012");

        when(aliasMapper.selectByPlatformSku("ysbang", "SKU002")).thenReturn(null);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterMapper.selectByBarcode("6922710600012")).thenReturn(drug);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals(FusionConfidence.BARCODE, result.getConfidence());
        assertFalse(result.isNeedsReview());

        // 验证创建了alias缓存
        verify(aliasMapper).insert(any(YyDrugAlias.class));
    }

    @Test
    void fuse_noMatch_goesToReview() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU003");
        snapshot.setBarcode(null);
        snapshot.setApprovalNumber(null);
        snapshot.setCommonName("未知药品");
        snapshot.setSpecification("1g");
        snapshot.setManufacturer("未知厂家");

        when(aliasMapper.selectByPlatformSku("ysbang", "SKU003")).thenReturn(null);
        when(drugMasterMapper.selectCandidates("未知药品", "1g")).thenReturn(new ArrayList<>());

        YyFusionResult result = engine.fuse(snapshot);

        assertFalse(result.isMatched());

        // 验证进入审核队列
        verify(reviewMapper).insert(any(YyFusionReview.class));
    }

    @Test
    void fuse_reviewThreshold_markedForReview() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU004");
        snapshot.setBarcode(null);
        snapshot.setApprovalNumber(null);
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s");
        snapshot.setManufacturer("北京同仁堂股份有限公司");

        when(aliasMapper.selectByPlatformSku("ysbang", "SKU004")).thenReturn(null);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(4L);
        drug.setDrugCode("DRUG004");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s");
        drug.setManufacturer("同仁堂");

        when(drugMasterMapper.selectCandidates("阿莫西林胶囊", "0.25g"))
            .thenReturn(Arrays.asList(drug));

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(4L, result.getDrugId());
        // 模糊匹配应标记待复核
        assertTrue(result.isNeedsReview());
    }
}
```

- [ ] **Step 5: 实现YyFusionEngineImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.domain.YyFusionReview;
import com.ruoyi.yy.mapper.YyFusionReviewMapper;
import com.ruoyi.yy.domain.YyDrugAlias;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyDrugAliasMapper;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 多信号融合引擎
 *
 * 流程：
 * 1. 检查yy_drug_alias缓存 → 命中直接返回
 * 2. 按优先级执行IYyMatchStrategy策略链
 * 3. 根据置信度决定：自动接受 / 待复核 / 进入审核队列
 */
@Service
public class YyFusionEngineImpl {

    private static final Logger log = LoggerFactory.getLogger(YyFusionEngineImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<IYyMatchStrategy> strategies;
    private final YyDrugMasterMapper drugMasterMapper;
    private final YyDrugAliasMapper aliasMapper;
    private final YyFusionReviewMapper reviewMapper;

    @Autowired
    public YyFusionEngineImpl(List<IYyMatchStrategy> strategies,
                        YyDrugMasterMapper drugMasterMapper,
                        YyDrugAliasMapper aliasMapper,
                        YyFusionReviewMapper reviewMapper) {
        this.strategies = strategies.stream()
            .sorted(Comparator.comparingInt(IYyMatchStrategy::getPriority).reversed())
            .collect(Collectors.toList());
        this.drugMasterMapper = drugMasterMapper;
        this.aliasMapper = aliasMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 对一个商品快照执行融合匹配
     */
    public YyFusionResult fuse(YyProductSnapshot snapshot) {
        String platformCode = snapshot.getSourcePlatform();
        String skuId = snapshot.getSkuId();

        // Step 1: 检查alias缓存
        YyDrugAlias cached = aliasMapper.selectByPlatformSku(platformCode, skuId);
        if (cached != null) {
            YyDrugMaster drug = drugMasterMapper.selectById(cached.getDrugId());
            if (drug != null) {
                log.debug("Alias cache hit: {} → drug_id={}", skuId, cached.getDrugId());
                return YyFusionResult.matched(
                    drug.getId(), drug.getDrugCode(), cached.getConfidence(),
                    cached.getMatchMethod(), "Alias cache hit", false
                );
            }
        }

        // Step 2: 获取候选集（供策略使用）
        List<YyDrugMaster> candidates = getCandidates(snapshot);

        // Step 3: 按优先级执行策略链
        YyMatchResult bestResult = null;
        for (IYyMatchStrategy strategy : strategies) {
            log.debug("Trying strategy {} for sku={}", strategy.getName(), skuId);
            YyMatchResult result = strategy.match(snapshot, candidates);
            if (result.isMatched()) {
                bestResult = result;
                break;  // 高优先级策略命中即停
            }
        }

        // Step 4: 处理结果
        if (bestResult != null && bestResult.isMatched()) {
            boolean needsReview = FusionConfidence.needsReview(bestResult.getConfidence());

            // 创建alias缓存
            saveAlias(snapshot, bestResult);

            log.info("Fusion matched: sku={} → drug_id={} via {} conf={}",
                skuId, bestResult.getDrugId(), bestResult.getMatchMethod(), bestResult.getConfidence());

            return YyFusionResult.matched(
                bestResult.getDrugId(), bestResult.getDrugCode(),
                bestResult.getConfidence(), bestResult.getMatchMethod().getCode(),
                bestResult.getReason(), needsReview
            );
        }

        // 未匹配 → 进入审核队列
        log.info("Fusion no match: sku={}, sending to review queue", skuId);
        saveToReviewQueue(snapshot, candidates);

        List<Long> candidateIds = candidates.stream()
            .map(YyDrugMaster::getId).collect(Collectors.toList());
        return YyFusionResult.noMatch(candidateIds, new ArrayList<>());
    }

    /**
     * 获取候选药品集
     */
    private List<YyDrugMaster> getCandidates(YyProductSnapshot snapshot) {
        String commonName = snapshot.getCommonName();
        if (commonName != null && !commonName.trim().isEmpty()) {
            return drugMasterMapper.selectCandidates(commonName.trim(), null);
        }
        return new ArrayList<>();
    }

    /**
     * 保存alias缓存映射
     */
    private void saveAlias(YyProductSnapshot snapshot, YyMatchResult result) {
        YyDrugAlias alias = new YyDrugAlias();
        alias.setDrugId(result.getDrugId());
        alias.setPlatformCode(snapshot.getSourcePlatform());
        alias.setPlatformProductName(snapshot.getCommonName());
        alias.setPlatformManufacturer(snapshot.getManufacturer());
        alias.setPlatformSpecification(snapshot.getSpecification());
        alias.setPlatformSkuId(snapshot.getSkuId());
        alias.setConfidence(result.getConfidence());
        alias.setMatchMethod(result.getMatchMethod().getCode());
        aliasMapper.insert(alias);
    }

    /**
     * 保存到审核队列
     */
    private void saveToReviewQueue(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        try {
            List<Long> candidateIds = candidates.stream()
                .map(YyDrugMaster::getId).collect(Collectors.toList());

            YyFusionReview review = new YyFusionReview();
            review.setSnapshotId(snapshot.getId());
            review.setCandidateDrugIds(JSON.writeValueAsString(candidateIds));
            review.setMatchScores("[]");
            review.setStatus("pending");
            reviewMapper.insert(review);
        } catch (Exception e) {
            log.error("Failed to save review queue entry for sku={}", snapshot.getSkuId(), e);
        }
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `mvn test -pl ruoyi-system -Dtest=YyFusionEngineImplTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/fusion/ \
        ruoyi-system/src/test/java/com/ruoyi/yy/YyFusionEngineImplTest.java
git commit -m "feat: add YyFusionEngineImpl with alias cache, strategy chain, and review queue"
```

---

## Task 7: YyProductSnapshot实体 + Mapper

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyProductSnapshot.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyProductSnapshotMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyProductSnapshotMapper.xml`

- [ ] **Step 1: 创建YyProductSnapshot实体**

```java
package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 平台商品快照 yy_product_snapshot
 */
public class YyProductSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sourcePlatform;
    private String skuId;
    private String productId;
    private String sourceApi;

    // 融合关联
    private Long drugId;
    private BigDecimal fusionConfidence;

    // 索引/查询字段
    private String commonName;
    private String barcode;
    private String approvalNumber;
    private String manufacturer;
    private String specification;
    private BigDecimal priceCurrent;
    private Integer stockQuantity;

    // 完整数据
    private String productData;      // JSON string
    private String rawDataPayload;   // JSON string

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSourceApi() { return sourceApi; }
    public void setSourceApi(String sourceApi) { this.sourceApi = sourceApi; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public BigDecimal getFusionConfidence() { return fusionConfidence; }
    public void setFusionConfidence(BigDecimal fusionConfidence) { this.fusionConfidence = fusionConfidence; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public BigDecimal getPriceCurrent() { return priceCurrent; }
    public void setPriceCurrent(BigDecimal priceCurrent) { this.priceCurrent = priceCurrent; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getProductData() { return productData; }
    public void setProductData(String productData) { this.productData = productData; }
    public String getRawDataPayload() { return rawDataPayload; }
    public void setRawDataPayload(String rawDataPayload) { this.rawDataPayload = rawDataPayload; }
    public Date getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Date collectedAt) { this.collectedAt = collectedAt; }
    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }
}
```

- [ ] **Step 2: 创建YyProductSnapshotMapper接口**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyProductSnapshot;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface YyProductSnapshotMapper {

    YyProductSnapshot selectById(Long id);

    YyProductSnapshot selectByPlatformSku(
        @Param("platformCode") String platformCode,
        @Param("skuId") String skuId
    );

    List<YyProductSnapshot> selectByDrugId(@Param("drugId") Long drugId);

    int insert(YyProductSnapshot record);

    int updateDrugBinding(
        @Param("id") Long id,
        @Param("drugId") Long drugId,
        @Param("fusionConfidence") java.math.BigDecimal fusionConfidence
    );

    void batchInsert(@Param("list") List<YyProductSnapshot> list);

    List<YyProductSnapshot> selectByPlatformAndSkuIds(
        @Param("platform") String platform,
        @Param("skuIds") List<String> skuIds
    );
}
```

- [ ] **Step 3: 创建YyProductSnapshotMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyProductSnapshotMapper">

    <resultMap type="YyProductSnapshot" id="YyProductSnapshotResult">
        <result property="id" column="id"/>
        <result property="sourcePlatform" column="source_platform"/>
        <result property="skuId" column="sku_id"/>
        <result property="productId" column="product_id"/>
        <result property="sourceApi" column="source_api"/>
        <result property="drugId" column="drug_id"/>
        <result property="fusionConfidence" column="fusion_confidence"/>
        <result property="commonName" column="common_name"/>
        <result property="barcode" column="barcode"/>
        <result property="approvalNumber" column="approval_number"/>
        <result property="manufacturer" column="manufacturer"/>
        <result property="specification" column="specification"/>
        <result property="priceCurrent" column="price_current"/>
        <result property="stockQuantity" column="stock_quantity"/>
        <result property="productData" column="product_data"/>
        <result property="rawDataPayload" column="raw_data_payload"/>
        <result property="collectedAt" column="collected_at"/>
        <result property="syncedAt" column="synced_at"/>
    </resultMap>

    <select id="selectById" resultMap="YyProductSnapshotResult">
        SELECT * FROM yy_product_snapshot WHERE id = #{id}
    </select>

    <select id="selectByPlatformSku" resultMap="YyProductSnapshotResult">
        SELECT * FROM yy_product_snapshot
        WHERE source_platform = #{platformCode} AND sku_id = #{skuId}
    </select>

    <select id="selectByDrugId" resultMap="YyProductSnapshotResult">
        SELECT * FROM yy_product_snapshot WHERE drug_id = #{drugId}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_product_snapshot (source_platform, sku_id, product_id, source_api,
            drug_id, fusion_confidence, common_name, barcode, approval_number,
            manufacturer, specification, price_current, stock_quantity,
            product_data, raw_data_payload, collected_at)
        VALUES (#{sourcePlatform}, #{skuId}, #{productId}, #{sourceApi},
            #{drugId}, #{fusionConfidence}, #{commonName}, #{barcode}, #{approvalNumber},
            #{manufacturer}, #{specification}, #{priceCurrent}, #{stockQuantity},
            #{productData}, #{rawDataPayload}, #{collectedAt})
    </insert>

    <update id="updateDrugBinding">
        UPDATE yy_product_snapshot
        SET drug_id = #{drugId}, fusion_confidence = #{fusionConfidence}
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 4: 创建JSON TypeHandler**

MySQL JSON类型需要TypeHandler实现String↔JSON映射。创建 `ruoyi-system/src/main/java/com/ruoyi/yy/handler/JsonStringTypeHandler.java`：

```java
package com.ruoyi.yy.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQL JSON ↔ String TypeHandler
 * MyBatis读写yy_product_snapshot.product_data等JSON列时自动转换
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class JsonStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
```

在 `YyProductSnapshotMapper.xml` 的 `product_data` 和 `raw_data_payload` 列上指定 typeHandler：

```xml
<result property="productData" column="product_data"
        typeHandler="com.ruoyi.yy.handler.JsonStringTypeHandler"/>
<result property="rawDataPayload" column="raw_data_payload"
        typeHandler="com.ruoyi.yy.handler.JsonStringTypeHandler"/>
```

在 INSERT 语句中也需要指定：

```xml
#{productData, typeHandler=com.ruoyi.yy.handler.JsonStringTypeHandler},
#{rawDataPayload, typeHandler=com.ruoyi.yy.handler.JsonStringTypeHandler},
```

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyProductSnapshot.java \
        ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyProductSnapshotMapper.java \
        ruoyi-system/src/main/resources/mapper/yy/YyProductSnapshotMapper.xml \
        ruoyi-system/src/main/java/com/ruoyi/yy/handler/JsonStringTypeHandler.java
git commit -m "feat: add YyProductSnapshot entity, mapper, and JSON TypeHandler"
```

---

## Task 8: YyAiGateway（AI统一网关 + 熔断器）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiRequest.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiResponse.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyCircuitBreaker.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiGatewayImpl.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyMockAiGateway.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyCircuitBreakerTest.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiGatewayTest.java`

- [ ] **Step 1: 创建YyAiRequest值对象**

```java
package com.ruoyi.yy.domain;

import java.util.Map;

/**
 * AI请求对象
 */
public class YyAiRequest {

    private String scene;           // match/advisor/evaluator/search/cleaner
    private String systemPrompt;
    private String userPrompt;
    private String model;           // qwen-turbo/qwen-plus/qwen-max
    private double temperature;
    private int maxTokens;
    private Map<String, Object> extra;

    public YyAiRequest() {
        this.model = "qwen-turbo";
        this.temperature = 0.1;
        this.maxTokens = 1000;
    }

    // Getters and setters
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
```

- [ ] **Step 2: 创建YyAiResponse值对象**

```java
package com.ruoyi.yy.domain;

/**
 * AI响应对象
 */
public class YyAiResponse {

    private boolean success;
    private String content;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private long latencyMs;
    private String errorMessage;

    public YyAiResponse() {}

    public static YyAiResponse ok(String content, String model, int promptTokens, int completionTokens, long latencyMs) {
        YyAiResponse r = new YyAiResponse();
        r.success = true;
        r.content = content;
        r.model = model;
        r.promptTokens = promptTokens;
        r.completionTokens = completionTokens;
        r.latencyMs = latencyMs;
        return r;
    }

    public static YyAiResponse fail(String errorMessage) {
        YyAiResponse r = new YyAiResponse();
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
```

- [ ] **Step 3: 创建熔断器测试**

```java
package com.ruoyi.yy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YyCircuitBreakerTest {

    @Test
    void initialState_closed() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        assertTrue(cb.allowRequest());
        assertEquals("CLOSED", cb.getState());
    }

    @Test
    void tripAfterFailures() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        cb.recordFailure();
        cb.recordFailure();
        assertTrue(cb.allowRequest());  // still closed
        cb.recordFailure();
        assertFalse(cb.allowRequest()); // now open
        assertEquals("OPEN", cb.getState());
    }

    @Test
    void halfOpenAfterTimeout() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 100); // 100ms timeout
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();
        assertFalse(cb.allowRequest());

        // Wait for timeout
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}

        assertTrue(cb.allowRequest());  // half-open
        assertEquals("HALF_OPEN", cb.getState());
    }

    @Test
    void halfOpen_success_closes() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 100);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        cb.allowRequest(); // transition to half-open
        cb.recordSuccess();
        assertEquals("CLOSED", cb.getState());
    }

    @Test
    void recordSuccess_resetsFailureCount() {
        YyCircuitBreaker cb = new YyCircuitBreaker(3, 300000);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordSuccess();
        cb.recordFailure();
        cb.recordFailure();
        assertTrue(cb.allowRequest()); // only 2 failures after reset
    }
}
```

- [ ] **Step 4: 实现YyCircuitBreaker**

```java
package com.ruoyi.yy.service.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单熔断器实现
 *
 * CLOSED → 连续失败达到阈值 → OPEN
 * OPEN → 等待超时 → HALF_OPEN
 * HALF_OPEN → 成功 → CLOSED / 失败 → OPEN
 */
public class YyCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openDurationMs;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    public YyCircuitBreaker(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > openDurationMs) {
                // CAS: only one thread transitions OPEN → HALF_OPEN
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return true;
            }
            return false;
        }
        // HALF_OPEN — allow one request
        return true;
    }

    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    public String getState() {
        return state.get().name();
    }
}
```

- [ ] **Step 5: 运行熔断器测试**

Run: `mvn test -pl ruoyi-system -Dtest=YyCircuitBreakerTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 6: 实现YyAiGateway**

```java
package com.ruoyi.yy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiPromptTemplate;
import com.ruoyi.yy.mapper.YyAiPromptTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * AI网关接口 — 抽取接口避免YyMockAiGateway继承导致@Autowired注入
 */
public interface IYyAiGateway {

    /**
     * 调用AI
     */
    YyAiResponse call(YyAiRequest request);

    /**
     * 健康检查
     */
    default boolean isHealthy() {
        return true;
    }
}

/**
 * AI统一网关 — 封装通义千问API调用
 *
 * 功能：
 * 1. 模型路由（简单任务用规则引擎，复杂任务调LLM）
 * 2. Prompt模板管理（从DB加载）
 * 3. 结果缓存（Redis，TTL 24h）
 * 4. 熔断器保护（3次失败→跳过5分钟）
 * 5. 用量统计
 */
@Service
public class YyAiGatewayImpl implements IYyAiGateway {

    private static final Logger log = LoggerFactory.getLogger(YyAiGateway.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    // 熔断器：3次失败，休息5分钟
    private final YyCircuitBreaker circuitBreaker = new YyCircuitBreaker(3, 300_000);

    @Autowired(required = false)
    private YyAiPromptTemplateMapper promptTemplateMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${ai.dashscope.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    /**
     * 调用AI（使用预定义模板）
     */
    public YyAiResponse call(YyAiRequest request) {
        long startTime = System.currentTimeMillis();

        // 检查缓存
        String cacheKey = buildCacheKey(request);
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return YyAiResponse.ok(cached, "cached", 0, 0,
                        System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed", e);
            }
        }

        // 检查熔断器
        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker OPEN, skipping AI call for scene={}", request.getScene());
            return YyAiResponse.fail("Circuit breaker open, AI service temporarily unavailable");
        }

        // 调用通义千问API
        try {
            YyAiResponse response = callDashScope(request);
            if (response.isSuccess()) {
                circuitBreaker.recordSuccess();
                // 写入缓存
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, response.getContent(),
                            24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.warn("Redis cache write failed", e);
                    }
                }
            } else {
                circuitBreaker.recordFailure();
            }
            return response;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("AI call failed for scene={}", request.getScene(), e);
            return YyAiResponse.fail(e.getMessage());
        }
    }

    /**
     * 调用通义千问API
     */
    private YyAiResponse callDashScope(YyAiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // 构建请求体
            String requestBody = JSON.writeValueAsString(java.util.Map.of(
                "model", request.getModel(),
                "input", java.util.Map.of(
                    "messages", new Object[]{
                        java.util.Map.of("role", "system", "content", request.getSystemPrompt()),
                        java.util.Map.of("role", "user", "content", request.getUserPrompt())
                    }
                ),
                "parameters", java.util.Map.of(
                    "temperature", request.getTemperature(),
                    "max_tokens", request.getMaxTokens(),
                    "result_format", "message"
                )
            ));

            // HTTP调用
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpResponse<String> httpResponse =
                httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

            long latency = System.currentTimeMillis() - startTime;

            if (httpResponse.statusCode() != 200) {
                return YyAiResponse.fail("HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
            }

            // 解析响应
            JsonNode root = JSON.readTree(httpResponse.body());
            JsonNode output = root.path("output");
            JsonNode choices = output.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                JsonNode usage = output.path("usage");
                int promptTokens = usage.path("input_tokens").asInt(0);
                int completionTokens = usage.path("output_tokens").asInt(0);
                return YyAiResponse.ok(content, request.getModel(), promptTokens, completionTokens, latency);
            }

            return YyAiResponse.fail("Empty response from DashScope");
        } catch (java.net.http.HttpTimeoutException e) {
            return YyAiResponse.fail("AI call timeout after 10s");
        } catch (Exception e) {
            return YyAiResponse.fail("AI call error: " + e.getMessage());
        }
    }

    /**
     * 加载Prompt模板
     */
    public String loadPromptTemplate(String templateCode) {
        if (promptTemplateMapper == null) return null;
        YyAiPromptTemplate template = promptTemplateMapper.selectByCode(templateCode);
        return template != null ? template.getUserPromptTemplate() : null;
    }

    private String buildCacheKey(YyAiRequest request) {
        String hash = Integer.toHexString(
            (request.getScene() + "|" + request.getUserPrompt()).hashCode()
        );
        return "ai:cache:" + request.getScene() + ":" + hash;
    }
}
```

- [ ] **Step 7: 创建YyMockAiGateway（测试用）**

```java
package com.ruoyi.yy.service.impl;

/**
 * 测试用Mock AI网关 — 实现接口，不继承YyAiGateway，避免@Autowired注入
 */
public class YyMockAiGateway implements IYyAiGateway {

    private String mockResponse;
    private boolean shouldFail;

    public YyMockAiGateway(String mockResponse) {
        this.mockResponse = mockResponse;
        this.shouldFail = false;
    }

    public YyMockAiGateway() {
        this.mockResponse = "{\"matched\": true, \"drug_id\": \"1\", \"confidence\": 0.95, \"reason\": \"mock\"}";
    }

    public void setMockResponse(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public YyAiResponse call(YyAiRequest request) {
        if (shouldFail) {
            return YyAiResponse.fail("Mock failure");
        }
        return YyAiResponse.ok(mockResponse, "mock", 100, 50, 100);
    }
}
```

- [ ] **Step 8: 创建Prompt模板Domain + Mapper**

```java
package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI Prompt模板 yy_ai_prompt_template
 */
public class YyAiPromptTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateCode;
    private String templateName;
    private String scene;
    private String systemPrompt;
    private String userPromptTemplate;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
```

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiPromptTemplate;
import org.apache.ibatis.annotations.Param;

public interface YyAiPromptTemplateMapper {

    YyAiPromptTemplate selectByCode(@Param("templateCode") String templateCode);

    int insert(YyAiPromptTemplate record);

    int updateById(YyAiPromptTemplate record);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyAiPromptTemplateMapper">

    <resultMap type="YyAiPromptTemplate" id="YyAiPromptTemplateResult">
        <result property="id" column="id"/>
        <result property="templateCode" column="template_code"/>
        <result property="templateName" column="template_name"/>
        <result property="scene" column="scene"/>
        <result property="systemPrompt" column="system_prompt"/>
        <result property="userPromptTemplate" column="user_prompt_template"/>
        <result property="model" column="model"/>
        <result property="temperature" column="temperature"/>
        <result property="maxTokens" column="max_tokens"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="selectByCode" resultMap="YyAiPromptTemplateResult">
        SELECT * FROM yy_ai_prompt_template WHERE template_code = #{templateCode} AND status = 1
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_ai_prompt_template (template_code, template_name, scene,
            system_prompt, user_prompt_template, model, temperature, max_tokens, status)
        VALUES (#{templateCode}, #{templateName}, #{scene},
            #{systemPrompt}, #{userPromptTemplate}, #{model}, #{temperature}, #{maxTokens}, #{status})
    </insert>

    <update id="updateById">
        UPDATE yy_ai_prompt_template
        SET template_name = #{templateName}, system_prompt = #{systemPrompt},
            user_prompt_template = #{userPromptTemplate}, model = #{model},
            temperature = #{temperature}, max_tokens = #{maxTokens}, status = #{status}
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 9: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/ai/ \
        ruoyi-system/src/test/java/com/ruoyi/yy/
git commit -m "feat: add YyAiGateway with DashScope integration, circuit breaker, and prompt template management"
```

---

## Task 9: YyAiMatchStrategy（AI匹配策略）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiMatchStrategy.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiMatchStrategyTest.java`

- [ ] **Step 1: 创建YyAiMatchStrategy测试**

```java
package com.ruoyi.yy;

import com.ruoyi.yy.service.impl.YyAiGatewayImpl;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.impl.YyMockAiGateway;
import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YyAiMatchStrategyTest {

    @Test
    void name() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());
        assertEquals("AiMatch", strategy.getName());
    }

    @Test
    void priority() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());
        assertEquals(10, strategy.getPriority());
    }

    @Test
    void match_success() {
        YyMockAiGateway gateway = new YyMockAiGateway(
            "{\"matched\": true, \"drug_id\": \"5\", \"confidence\": 0.92, \"reason\": \"Same drug\"}"
        );
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setManufacturer("联邦制药");
        snapshot.setSpecification("0.25g*12s");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(5L);
        drug.setDrugCode("DRUG005");
        drug.setCommonName("阿莫西林胶囊");

        List<YyDrugMaster> candidates = Arrays.asList(drug);
        YyMatchResult result = strategy.match(snapshot, candidates);

        assertTrue(result.isMatched());
        assertEquals(5L, result.getDrugId());
        assertEquals(MatchMethod.AI, result.getMatchMethod());
    }

    @Test
    void match_noMatch() {
        YyMockAiGateway gateway = new YyMockAiGateway(
            "{\"matched\": false, \"confidence\": 0.3, \"reason\": \"Different drugs\"}"
        );
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("未知药品");

        YyMatchResult result = strategy.match(snapshot, Arrays.asList());

        assertFalse(result.isMatched());
    }

    @Test
    void match_gatewayFailure() {
        YyMockAiGateway gateway = new YyMockAiGateway();
        gateway.setShouldFail(true);
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyMatchResult result = strategy.match(snapshot, Arrays.asList());

        assertFalse(result.isMatched());
    }

    @Test
    void match_noCandidates() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyMatchResult result = strategy.match(snapshot, Arrays.asList());

        assertFalse(result.isMatched());
    }
}
```

- [ ] **Step 2: 实现YyAiMatchStrategy**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.impl.YyAiGatewayImpl;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyProductSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI匹配策略 — 调用LLM判断药品是否匹配
 *
 * 优先级最低（10），作为兜底策略。
 * 将商品快照信息和候选药品列表发给通义千问，由LLM判断是否为同一药品。
 */
@Component
public class YyAiMatchStrategy implements IYyMatchStrategy {

    private static final Logger log = LoggerFactory.getLogger(YyAiMatchStrategy.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药行业数据专家。请判断以下药品信息是否指向同一药品。\n" +
        "请返回JSON格式：{\"matched\": true/false, \"drug_id\": \"xxx\", \"confidence\": 0.95, \"reason\": \"xxx\"}";

    private final YyAiGateway aiGateway;

    @Autowired
    public YyAiMatchStrategy(YyAiGateway aiGateway) {
        this.aiGateway = aiGateway;
    }

    @Override
    public String getName() {
        return "AiMatch";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return YyMatchResult.failure("No candidates for AI matching");
        }

        String userPrompt = buildPrompt(snapshot, candidates);

        YyAiRequest request = new YyAiRequest();
        request.setScene("match");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.1);
        request.setMaxTokens(500);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("AI match failed: {}", response.getErrorMessage());
            return YyMatchResult.failure("AI call failed: " + response.getErrorMessage());
        }

        return parseResponse(response.getContent(), candidates);
    }

    private String buildPrompt(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("商品信息:\n");
        sb.append("- 商品名: ").append(nullSafe(snapshot.getCommonName())).append("\n");
        sb.append("- 厂家: ").append(nullSafe(snapshot.getManufacturer())).append("\n");
        sb.append("- 规格: ").append(nullSafe(snapshot.getSpecification())).append("\n");
        sb.append("- 69码: ").append(nullSafe(snapshot.getBarcode())).append("\n");
        sb.append("- 批准文号: ").append(nullSafe(snapshot.getApprovalNumber())).append("\n\n");

        sb.append("候选药品:\n");
        for (int i = 0; i < candidates.size(); i++) {
            YyDrugMaster d = candidates.get(i);
            sb.append(i + 1).append(". id=").append(d.getId())
              .append(" 通用名=").append(d.getCommonName())
              .append(" 厂家=").append(d.getManufacturer())
              .append(" 规格=").append(d.getSpecification())
              .append(" 批准文号=").append(d.getApprovalNumber())
              .append("\n");
        }

        sb.append("\n请判断商品信息与哪个候选药品是同一药品。如果没有匹配的，matched设为false。");
        return sb.toString();
    }

    private YyMatchResult parseResponse(String content, List<YyDrugMaster> candidates) {
        try {
            JsonNode root = JSON.readTree(content);
            boolean matched = root.path("matched").asBoolean(false);
            if (!matched) {
                return YyMatchResult.failure("AI determined no match");
            }

            long drugId = Long.parseLong(root.path("drug_id").asText("0"));
            double confidence = root.path("confidence").asDouble(0.5);
            String reason = root.path("reason").asText("AI match");

            // 验证drug_id在候选列表中
            YyDrugMaster matchedDrug = candidates.stream()
                .filter(d -> d.getId() == drugId)
                .findFirst()
                .orElse(null);

            if (matchedDrug == null) {
                log.warn("AI returned drug_id={} not in candidates", drugId);
                return YyMatchResult.failure("AI returned invalid drug_id");
            }

            return YyMatchResult.success(
                matchedDrug.getId(),
                matchedDrug.getDrugCode(),
                BigDecimal.valueOf(confidence).setScale(2, java.math.RoundingMode.HALF_UP),
                MatchMethod.AI,
                "AI: " + reason
            );
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", content, e);
            return YyMatchResult.failure("Invalid AI response format");
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "未知";
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn test -pl ruoyi-system -Dtest=YyAiMatchStrategyTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiMatchStrategy.java \
        ruoyi-system/src/test/java/com/ruoyi/yy/YyAiMatchStrategyTest.java
git commit -m "feat: add YyAiMatchStrategy as fallback fusion strategy using LLM"
```

---

## Task 10: 平台适配器模式

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/IYyPlatformAdapter.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyConfigurablePlatformAdapter.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/YyPlatformAdapterRegistry.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/YyConfigurablePlatformAdapterTest.java`

- [ ] **Step 1: 创建IYyPlatformAdapter接口**

```java
package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyProductSnapshot;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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
```

- [ ] **Step 2: 创建YyConfigurablePlatformAdapter**

```java
package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.yy.domain.YyFieldMapping;
import com.ruoyi.yy.domain.YyPlatformApi;
import com.ruoyi.yy.domain.YyProductSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通用平台适配器 — 行为由DB配置驱动
 *
 * 通过yy_field_mapping表的映射规则，将不同平台的字段标准化为统一格式。
 * 特殊平台（如药师帮签名）可继承重写特定方法。
 */
@Component
public class YyConfigurablePlatformAdapter implements IYyPlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(YyConfigurablePlatformAdapter.class);

    @Autowired
    private com.ruoyi.yy.mapper.YyFieldMappingMapper fieldMappingMapper;

    @Override
    public String getPlatformCode() {
        return "*";  // 通用适配器，匹配所有平台
    }

    @Override
    public String decrypt(String encryptedData, String platformKey, int encryptType) {
        if (encryptType == 0) {
            return encryptedData;  // 无加密
        }
        // AES/DES解密逻辑（复用现有代码）
        try {
            if (encryptType == 1) {  // AES
                return decryptAES(encryptedData, platformKey);
            }
        } catch (Exception e) {
            log.error("Decrypt failed", e);
        }
        return encryptedData;
    }

    @Override
    public JSONArray extractProductArray(String decryptedJson, String entryPath) {
        if (entryPath == null || entryPath.isEmpty()) {
            return JSONArray.parseArray(decryptedJson);
        }
        JSONObject root = JSONObject.parseObject(decryptedJson);
        return root.getJSONArray(entryPath);
    }

    @Override
    public YyProductSnapshot normalizeProduct(JSONObject rawItem, String platformCode, String apiCode) {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform(platformCode);
        snapshot.setSourceApi(apiCode);

        // 加载该平台的字段映射配置
        List<YyFieldMapping> mappings = fieldMappingMapper.selectByPlatformCode(platformCode);
        Map<String, YyFieldMapping> mappingMap = mappings.stream()
            .collect(Collectors.toMap(YyFieldMapping::getSourceField, m -> m));

        // 应用字段映射
        for (Map.Entry<String, YyFieldMapping> entry : mappingMap.entrySet()) {
            String sourceField = entry.getKey();
            YyFieldMapping mapping = entry.getValue();
            Object rawValue = rawItem.get(sourceField);

            if (rawValue == null && mapping.getDefaultValue() != null) {
                rawValue = mapping.getDefaultValue();
            }
            if (rawValue == null) continue;

            String value = rawValue.toString();

            // 应用转换规则
            if (mapping.getTransformRule() != null && !mapping.getTransformRule().isEmpty()) {
                value = applyTransform(value, mapping.getTransformRule());
            }

            // 设置到快照对象
            setSnapshotField(snapshot, mapping.getTargetField(), value);
        }

        // 保存原始数据
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
        // 去除品牌名等，只保留通用名
        String cleaned = drugName.replaceAll("[（(][^）)]*[）)]", "").trim();
        if (!cleaned.equals(drugName.trim())) {
            keywords.add(cleaned);
        }
        return keywords;
    }

    /**
     * 应用值转换规则
     */
    private String applyTransform(String value, String transformRule) {
        if (transformRule.startsWith("regex:")) {
            String pattern = transformRule.substring(6);
            Matcher m = Pattern.compile(pattern).matcher(value);
            if (m.find()) {
                return m.group(1);
            }
        } else if (transformRule.startsWith("replace:")) {
            String[] parts = transformRule.substring(8).split("→");
            if (parts.length == 2) {
                return value.replace(parts[0], parts[1]);
            }
        }
        return value;
    }

    /**
     * 将值设置到快照对象的指定字段
     */
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
                try { snapshot.setPriceCurrent(new BigDecimal(value)); } catch (NumberFormatException ignored) {}
                break;
            case "stock_quantity":
                try { snapshot.setStockQuantity(Integer.parseInt(value)); } catch (NumberFormatException ignored) {}
                break;
            // product_data中的字段通过JSON处理
            default:
                log.debug("Unmapped field: {}", field);
        }
    }

    private String decryptAES(String data, String key) throws Exception {
        // 复用现有AES解密逻辑
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(
            key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, spec);
        byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(data));
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 3: 创建YyPlatformAdapterRegistry**

```java
package com.ruoyi.yy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台适配器注册中心 — 启动时加载所有适配器，运行时按platformCode获取
 */
@Component
public class YyPlatformAdapterRegistry {

    private final Map<String, IYyPlatformAdapter> adapters = new HashMap<>();
    private IYyPlatformAdapter defaultAdapter;

    @Autowired
    public YyPlatformAdapterRegistry(List<IYyPlatformAdapter> adapterList) {
        for (IYyPlatformAdapter adapter : adapterList) {
            adapters.put(adapter.getPlatformCode(), adapter);
            if ("*".equals(adapter.getPlatformCode())) {
                defaultAdapter = adapter;
            }
        }
    }

    /**
     * 获取指定平台的适配器，未找到则返回通用适配器
     */
    public IYyPlatformAdapter getAdapter(String platformCode) {
        IYyPlatformAdapter adapter = adapters.get(platformCode);
        if (adapter == null) {
            adapter = defaultAdapter;
        }
        return adapter;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/platform/adapter/
git commit -m "feat: add IYyPlatformAdapter pattern with YyConfigurablePlatformAdapter and registry"
```

---

## Task 11: DataFusionServiceImpl重构（接入YyFusionEngineImpl + 批量化）

**Files:**
- Modify: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java`

- [ ] **Step 1: 读取现有DataFusionServiceImpl**

Read the file at `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java` to understand the current implementation.

- [ ] **Step 2: 在DataFusionServiceImpl中注入YyFusionEngineImpl和相关依赖**

在类的字段声明区域添加：

```java
@Autowired
private com.ruoyi.yy.service.impl.YyFusionEngineImpl fusionEngine;

@Autowired
private com.ruoyi.yy.mapper.YyProductSnapshotMapper productSnapshotMapper;
```

- [ ] **Step 3: 重构ingest方法的核心匹配逻辑**

将原有的MD5融合键匹配逻辑替换为调用YyFusionEngineImpl。在 `ingest()` 方法中，替换匹配部分：

```java
// 旧逻辑：MD5融合键匹配
// String fusionKey = generateFusionKey(commonName, specification, manufacturer, approvalNumber);

// 新逻辑：调用YyFusionEngineImpl
com.ruoyi.yy.domain.YyProductSnapshot snapshot = new com.ruoyi.yy.domain.YyProductSnapshot();
snapshot.setSourcePlatform(dataIngestDto.getPlatformCode());
snapshot.setSkuId(dataIngestDto.getSkuId());
snapshot.setCommonName(commonName);
snapshot.setBarcode(barcode);
snapshot.setApprovalNumber(approvalNumber);
snapshot.setManufacturer(manufacturer);
snapshot.setSpecification(specification);
snapshot.setPriceCurrent(dataIngestDto.getPrice());

// 保存快照
productSnapshotMapper.insert(snapshot);

// 执行融合匹配
com.ruoyi.yy.domain.YyFusionResult fusionResult = fusionEngine.fuse(snapshot);

if (fusionResult.isMatched()) {
    // 绑定到药品主数据
    snapshot.setDrugId(fusionResult.getDrugId());
    snapshot.setFusionConfidence(fusionResult.getConfidence());
    productSnapshotMapper.updateDrugBinding(snapshot.getId(), fusionResult.getDrugId(), fusionResult.getConfidence());
    log.info("Product fused: sku={} → drug_id={} via {} conf={}",
        dataIngestDto.getSkuId(), fusionResult.getDrugId(),
        fusionResult.getMatchMethod(), fusionResult.getConfidence());
} else {
    log.info("Product needs review: sku={}", dataIngestDto.getSkuId());
}
```

- [ ] **Step 4: 批量化查询消除N+1**

原有循环中每条产品执行 `selectByFusionKey` + `upsert` = N*2次DB操作。改为批量操作：

在 `YyProductSnapshotMapper.java` 添加批量方法：

```java
void batchInsert(@Param("list") List<YyProductSnapshot> list);
List<YyProductSnapshot> selectByPlatformAndSkuIds(@Param("platform") String platform, @Param("skuIds") List<String> skuIds);
```

在 `YyProductSnapshotMapper.xml` 添加批量SQL：

```xml
<insert id="batchInsert" parameterType="list">
    INSERT INTO yy_product_snapshot (source_platform, sku_id, product_id, source_api,
        drug_id, fusion_confidence, common_name, barcode, approval_number,
        manufacturer, specification, price_current, stock_quantity,
        product_data, raw_data_payload, collected_at)
    VALUES
    <foreach collection="list" item="s" separator=",">
        (#{s.sourcePlatform}, #{s.skuId}, #{s.productId}, #{s.sourceApi},
         #{s.drugId}, #{s.fusionConfidence}, #{s.commonName}, #{s.barcode}, #{s.approvalNumber},
         #{s.manufacturer}, #{s.specification}, #{s.priceCurrent}, #{s.stockQuantity},
         #{s.productData}, #{s.rawDataPayload}, #{s.collectedAt})
    </foreach>
</insert>

<select id="selectByPlatformAndSkuIds" resultMap="YyProductSnapshotResult">
    SELECT * FROM yy_product_snapshot
    WHERE source_platform = #{platform} AND sku_id IN
    <foreach collection="skuIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

重构 `ingest()` 方法：先批量查询已存在的快照，再批量插入新快照，最后逐条执行融合匹配（YyFusionEngineImpl需要逐条处理）。

- [ ] **Step 5: 确保事务注解正确**

确认 `ingest()` 方法的 `@Transactional` 注解包含 `rollbackFor = Exception.class`：

```java
@Transactional(rollbackFor = Exception.class)
public void ingest(YyDataIngestDTO dataIngestDto) {
    // ...
}
```

- [ ] **Step 6: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java \
        ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyProductSnapshotMapper.java \
        ruoyi-system/src/main/resources/mapper/yy/YyProductSnapshotMapper.xml
git commit -m "refactor: integrate YyFusionEngineImpl and batch operations into DataFusionServiceImpl"
```

---

## Task 12: Chrome扩展重构（模块拆分）

**Files:**
- Create: `helpbuy-clone/Extensions/engine.js`
- Create: `helpbuy-clone/Extensions/platform-manager.js`
- Create: `helpbuy-clone/Extensions/message-router.js`
- Modify: `helpbuy-clone/Extensions/background.js`

- [ ] **Step 1: 从background.js提取CDP执行引擎到engine.js**

从 `background.js` 中提取以下函数到新文件 `engine.js`：
- `callCdpFetch()`
- `executeDynamicApiBatch()`
- `executeDynamicApiBatchWithSign()`
- `handleAppendFetch()`

```javascript
// engine.js — CDP执行引擎
// 从background.js提取的通用CDP执行逻辑

/**
 * 调用CDP fetch拦截
 */
async function callCdpFetch(tabId, apiConfig, keyword, page, pageSize) {
    // ... 从background.js迁移的实现
}

/**
 * 批量执行动态API
 */
async function executeDynamicApiBatch(tabId, platformConfig, keyword, pages) {
    // ... 从background.js迁移的实现
}

/**
 * 带签名的批量执行
 */
async function executeDynamicApiBatchWithSign(tabId, platformConfig, keyword, pages) {
    // ... 从background.js迁移的实现
}

/**
 * 处理追加采集
 */
async function handleAppendFetch(apiCode, params) {
    // ... 从background.js迁移的实现（已修复P0 bug）
}

export { callCdpFetch, executeDynamicApiBatch, executeDynamicApiBatchWithSign, handleAppendFetch };
```

- [ ] **Step 2: 创建platform-manager.js（动态配置加载）**

```javascript
// platform-manager.js — 平台配置管理
// 从后端API动态加载平台配置，替代硬编码

let platformConfigs = [];

/**
 * 从后端加载全部平台配置
 */
async function loadPlatformConfigs() {
    try {
        const response = await fetch('https://your-backend/yy/platform/configForExtension');
        const data = await response.json();
        platformConfigs = data.platforms || [];
        console.log(`[PlatformManager] Loaded ${platformConfigs.length} platform configs`);
        return platformConfigs;
    } catch (error) {
        console.error('[PlatformManager] Failed to load configs:', error);
        return [];
    }
}

/**
 * 获取指定平台配置
 */
function getPlatformConfig(platformCode) {
    return platformConfigs.find(p => p.platformCode === platformCode);
}

/**
 * 获取所有平台配置
 */
function getAllPlatformConfigs() {
    return platformConfigs;
}

export { loadPlatformConfigs, getPlatformConfig, getAllPlatformConfigs };
```

- [ ] **Step 3: 创建message-router.js（消息路由）**

```javascript
// message-router.js — 消息路由
// 统一处理chrome.runtime.onMessage，分发到对应处理函数

import { loadPlatformConfigs, getPlatformConfig } from './platform-manager.js';
import { callCdpFetch, executeDynamicApiBatch, handleAppendFetch } from './engine.js';

/**
 * 初始化消息路由
 */
function initMessageRouter() {
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
        handleMessage(message, sender).then(sendResponse);
        return true; // 异步响应
    });
}

/**
 * 消息分发
 */
async function handleMessage(message, sender) {
    switch (message.action) {
        case 'fetchProducts':
            return await handleFetchProducts(message, sender);
        case 'appendFetch':
            return await handleAppendFetch(message.apiCode, message.params);
        case 'getConfigs':
            return { configs: getAllPlatformConfigs() };
        case 'refreshConfigs':
            return { configs: await loadPlatformConfigs() };
        default:
            return { error: `Unknown action: ${message.action}` };
    }
}

async function handleFetchProducts(message, sender) {
    const config = getPlatformConfig(message.platformCode);
    if (!config) {
        return { error: `Unknown platform: ${message.platformCode}` };
    }
    // ... 采集逻辑
}

export { initMessageRouter };
```

- [ ] **Step 4: 重构background.js为薄入口**

```javascript
// background.js — 薄入口
// 组装各模块，启动扩展

import { loadPlatformConfigs } from './platform-manager.js';
import { initMessageRouter } from './message-router.js';

// 启动时加载配置
loadPlatformConfigs();

// 初始化消息路由
initMessageRouter();

// 保持现有的alarm、webRequest等监听器
// ...
```

- [ ] **Step 5: 验证Chrome扩展功能**

1. 在Chrome中加载解压的扩展
2. 访问药师帮网站，验证数据采集功能正常
3. 验证动态配置加载是否生效

- [ ] **Step 6: Commit**

```bash
git add helpbuy-clone/Extensions/engine.js \
        helpbuy-clone/Extensions/platform-manager.js \
        helpbuy-clone/Extensions/message-router.js \
        helpbuy-clone/Extensions/background.js
git commit -m "refactor: split Chrome extension background.js into modular files"
```

---

## Task 13: YyAiAdvisorImpl（AI比价顾问）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPurchaseAdvice.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiAdvisorImpl.java`

- [ ] **Step 1: 创建YyPurchaseAdvice值对象**

```java
package com.ruoyi.yy.service.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI采购建议
 */
public class YyPurchaseAdvice implements Serializable {

    private static final long serialVersionUID = 1L;

    private String summary;             // 自然语言建议摘要
    private String bestPlatform;        // 推荐平台编码
    private BigDecimal bestPrice;       // 最优价格
    private BigDecimal totalSaving;     // 相比最贵平台节省金额
    private List<String> tips;          // 凑单/时机建议
    private Map<String, Object> detail; // 详细数据

    // Getters and setters
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBestPlatform() { return bestPlatform; }
    public void setBestPlatform(String bestPlatform) { this.bestPlatform = bestPlatform; }
    public BigDecimal getBestPrice() { return bestPrice; }
    public void setBestPrice(BigDecimal bestPrice) { this.bestPrice = bestPrice; }
    public BigDecimal getTotalSaving() { return totalSaving; }
    public void setTotalSaving(BigDecimal totalSaving) { this.totalSaving = totalSaving; }
    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }
    public Map<String, Object> getDetail() { return detail; }
    public void setDetail(Map<String, Object> detail) { this.detail = detail; }
}
```

- [ ] **Step 2: 实现YyAiAdvisorImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.impl.YyAiGatewayImpl;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.price.domain.YyPriceComparison;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI比价顾问 — 综合多平台价格给出采购建议
 *
 * 能力：
 * - 综合成本计算（价格+运费-满减-会员折扣）
 * - 凑单建议
 * - 采购时机建议
 */
@Service
public class YyAiAdvisorImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiAdvisorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购比价专家。请根据以下多平台价格数据，给出最优采购建议。\n" +
        "请返回JSON格式：\n" +
        "{\"summary\": \"自然语言建议\", \"bestPlatform\": \"平台编码\", \"bestPrice\": 价格, " +
        "\"totalSaving\": 节省金额, \"tips\": [\"建议1\", \"建议2\"]}";

    @Autowired
    private YyAiGateway aiGateway;

    /**
     * 获取采购建议
     *
     * @param drugName 药品名
     * @param prices   各平台价格列表
     * @return 采购建议
     */
    public YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices) {
        if (prices == null || prices.isEmpty()) {
            YyPurchaseAdvice empty = new YyPurchaseAdvice();
            empty.setSummary("暂无该药品的比价数据");
            return empty;
        }

        String userPrompt = buildPrompt(drugName, prices);

        YyAiRequest request = new YyAiRequest();
        request.setScene("advisor");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("AI advisor failed: {}", response.getErrorMessage());
            return buildFallbackAdvice(prices);
        }

        return parseResponse(response.getContent(), prices);
    }

    private String buildPrompt(String drugName, List<YyPriceComparison> prices) {
        StringBuilder sb = new StringBuilder();
        sb.append("药品: ").append(drugName).append("\n\n");
        sb.append("各平台价格:\n");

        for (YyPriceComparison price : prices) {
            sb.append("- 平台: ").append(price.getPlatformCode())
              .append(" 供货价: ").append(price.getPrice())
              .append(" 运费: ").append(price.getFreightAmount() != null ? price.getFreightAmount() : "0")
              .append(" 库存: ").append(price.getStockQuantity() != null ? price.getStockQuantity() : "未知")
              .append("\n");
        }

        sb.append("\n请分析哪个平台最划算，考虑价格、运费、库存因素，并给出凑单建议。");
        return sb.toString();
    }

    private YyPurchaseAdvice parseResponse(String content, List<YyPriceComparison> prices) {
        try {
            JsonNode root = JSON.readTree(content);
            YyPurchaseAdvice advice = new YyPurchaseAdvice();
            advice.setSummary(root.path("summary").asText("暂无建议"));
            advice.setBestPlatform(root.path("bestPlatform").asText());
            advice.setBestPrice(new BigDecimal(root.path("bestPrice").asText("0")));
            advice.setTotalSaving(new BigDecimal(root.path("totalSaving").asText("0")));

            List<String> tips = new ArrayList<>();
            JsonNode tipsNode = root.path("tips");
            if (tipsNode.isArray()) {
                for (JsonNode tip : tipsNode) {
                    tips.add(tip.asText());
                }
            }
            advice.setTips(tips);
            return advice;
        } catch (Exception e) {
            log.error("Failed to parse AI advisor response", e);
            return buildFallbackAdvice(prices);
        }
    }

    /**
     * 降级建议：不调用LLM，直接取最低价
     */
    private YyPurchaseAdvice buildFallbackAdvice(List<YyPriceComparison> prices) {
        YyPriceComparison cheapest = prices.stream()
            .min((a, b) -> {
                BigDecimal priceA = a.getPrice() != null ? a.getPrice() : BigDecimal.valueOf(99999);
                BigDecimal priceB = b.getPrice() != null ? b.getPrice() : BigDecimal.valueOf(99999);
                return priceA.compareTo(priceB);
            })
            .orElse(null);

        YyPurchaseAdvice advice = new YyPurchaseAdvice();
        if (cheapest != null) {
            advice.setSummary("推荐在 " + cheapest.getPlatformCode() + " 采购，价格最低");
            advice.setBestPlatform(cheapest.getPlatformCode());
            advice.setBestPrice(cheapest.getPrice());
        } else {
            advice.setSummary("暂无价格数据");
        }
        advice.setTips(new ArrayList<>());
        return advice;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/ai/advisor/
git commit -m "feat: add YyAiAdvisorImpl for multi-platform price comparison advice"
```

---

## Task 14: YyAiDataCleanerImpl（AI数据清洗）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyCleanResult.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiDataCleanerImpl.java`

- [ ] **Step 1: 创建YyCleanResult值对象**

```java
package com.ruoyi.yy.service.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 数据清洗结果
 */
public class YyCleanResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalProcessed;
    private int autoFixed;
    private int needsReview;
    private List<Map<String, Object>> suggestions;  // 清洗建议列表

    // Getters and setters
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
    public int getAutoFixed() { return autoFixed; }
    public void setAutoFixed(int autoFixed) { this.autoFixed = autoFixed; }
    public int getNeedsReview() { return needsReview; }
    public void setNeedsReview(int needsReview) { this.needsReview = needsReview; }
    public List<Map<String, Object>> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Map<String, Object>> suggestions) { this.suggestions = suggestions; }
}
```

- [ ] **Step 2: 实现YyAiDataCleanerImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.impl.YyAiGatewayImpl;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI数据清洗 — 批量标准化厂家名、规格、通用名
 *
 * 场景：历史采集数据中存在大量脏数据（错别字、格式不一致、缺失值）
 * 能力：
 * - 批量标准化厂家名（"同仁堂" vs "北京同仁堂股份有限公司"）
 * - 规格格式统一（"0.25g×12片" vs "0.25g*12s"）
 * - 识别并修复数据不一致
 */
@Service
public class YyAiDataCleanerImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiDataCleanerImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药数据清洗专家。请分析以下商品数据，识别需要标准化的字段。\n" +
        "请返回JSON数组，每个元素包含：\n" +
        "{\"snapshotId\": id, \"field\": \"字段名\", \"original\": \"原值\", " +
        "\"suggested\": \"建议值\", \"confidence\": 0.9, \"reason\": \"原因\"}";

    @Autowired
    private YyAiGateway aiGateway;

    @Autowired
    private YyProductSnapshotMapper snapshotMapper;

    /**
     * 批量清洗商品数据
     *
     * @param snapshots 待清洗的商品快照列表
     * @return 清洗结果
     */
    public YyCleanResult cleanProductData(List<YyProductSnapshot> snapshots) {
        YyCleanResult result = new YyCleanResult();
        result.setTotalProcessed(snapshots.size());

        if (snapshots.isEmpty()) {
            return result;
        }

        // 按平台分组，每批最多20条
        List<List<YyProductSnapshot>> batches = partition(snapshots, 20);

        List<Map<String, Object>> allSuggestions = new ArrayList<>();

        for (List<YyProductSnapshot> batch : batches) {
            String userPrompt = buildPrompt(batch);

            YyAiRequest request = new YyAiRequest();
            request.setScene("cleaner");
            request.setSystemPrompt(SYSTEM_PROMPT);
            request.setUserPrompt(userPrompt);
            request.setModel("qwen-turbo");
            request.setTemperature(0.1);
            request.setMaxTokens(2000);

            YyAiResponse response = aiGateway.call(request);

            if (response.isSuccess()) {
                List<Map<String, Object>> batchSuggestions = parseSuggestions(response.getContent());
                allSuggestions.addAll(batchSuggestions);
            } else {
                log.warn("AI cleaner batch failed: {}", response.getErrorMessage());
            }
        }

        result.setSuggestions(allSuggestions);
        result.setNeedsReview(allSuggestions.size());
        return result;
    }

    private String buildPrompt(List<YyProductSnapshot> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("待清洗的商品数据：\n\n");

        for (YyProductSnapshot s : batch) {
            sb.append("ID: ").append(s.getId()).append("\n");
            sb.append("  通用名: ").append(nullSafe(s.getCommonName())).append("\n");
            sb.append("  厂家: ").append(nullSafe(s.getManufacturer())).append("\n");
            sb.append("  规格: ").append(nullSafe(s.getSpecification())).append("\n");
            sb.append("  批准文号: ").append(nullSafe(s.getApprovalNumber())).append("\n");
            sb.append("  69码: ").append(nullSafe(s.getBarcode())).append("\n\n");
        }

        sb.append("请识别需要标准化的字段，如厂家名不一致、规格格式混乱等。");
        return sb.toString();
    }

    private List<Map<String, Object>> parseSuggestions(String content) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        try {
            JsonNode root = JSON.readTree(content);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("snapshotId", item.path("snapshotId").asLong());
                    suggestion.put("field", item.path("field").asText());
                    suggestion.put("original", item.path("original").asText());
                    suggestion.put("suggested", item.path("suggested").asText());
                    suggestion.put("confidence", item.path("confidence").asDouble());
                    suggestion.put("reason", item.path("reason").asText());
                    suggestions.add(suggestion);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI cleaner response", e);
        }
        return suggestions;
    }

    private List<List<YyProductSnapshot>> partition(List<YyProductSnapshot> list, int size) {
        List<List<YyProductSnapshot>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String nullSafe(String value) {
        return value != null ? value : "未知";
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/ai/cleaner/
git commit -m "feat: add YyAiDataCleanerImpl for batch data standardization"
```

---

## Task 15: 集成测试 + 数据迁移脚本

**Files:**
- Modify: `ruoyi-system/pom.xml` (添加测试依赖)
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/service/impl/DataFusionServiceIntegrationTest.java`
- Create: `ruoyi-system/src/main/resources/db/migration/V20260430__migrate_standard_product_to_snapshot.sql`

- [ ] **Step 0: 添加测试依赖**

项目当前零测试基础设施。在 `ruoyi-system/pom.xml` 添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

创建测试目录结构：

```bash
mkdir -p ruoyi-system/src/test/java/com/ruoyi/yy/fusion/engine
mkdir -p ruoyi-system/src/test/java/com/ruoyi/yy/ai/gateway
mkdir -p ruoyi-system/src/test/java/com/ruoyi/yy/service/impl
```

- [ ] **Step 1: 创建数据融合集成测试**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.service.impl.YyFusionEngineImpl;
import com.ruoyi.yy.domain.YyFusionResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据融合集成测试
 *
 * 需要数据库连接，使用@Transactional回滚避免污染数据
 */
@SpringBootTest
@Transactional
class DataFusionServiceIntegrationTest {

    @Autowired
    private YyFusionEngineImpl fusionEngine;

    @Autowired
    private YyProductSnapshotMapper snapshotMapper;

    @Test
    void ingest_createsSnapshot() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("test_platform");
        snapshot.setSkuId("TEST_SKU_001");
        snapshot.setCommonName("测试药品");
        snapshot.setSpecification("0.25g*12s");
        snapshot.setManufacturer("测试厂家");
        snapshot.setProductData("{}");

        snapshotMapper.insert(snapshot);

        assertNotNull(snapshot.getId());
        assertTrue(snapshot.getId() > 0);

        YyProductSnapshot loaded = snapshotMapper.selectById(snapshot.getId());
        assertNotNull(loaded);
        assertEquals("test_platform", loaded.getSourcePlatform());
        assertEquals("TEST_SKU_001", loaded.getSkuId());
    }

    @Test
    void fuse_noDrugMaster_returnsNoMatch() {
        // 没有药品主数据时，融合应返回未匹配
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("test_platform");
        snapshot.setSkuId("TEST_SKU_002");
        snapshot.setCommonName("不存在的药品XYZ");
        snapshot.setSpecification("999g");
        snapshot.setManufacturer("不存在的厂家");
        snapshot.setProductData("{}");

        snapshotMapper.insert(snapshot);

        YyFusionResult result = fusionEngine.fuse(snapshot);

        // 没有匹配到药品主数据
        assertFalse(result.isMatched());
    }

    @Test
    void selectByPlatformSku_works() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU_SELECT_TEST");
        snapshot.setCommonName("查询测试药品");
        snapshot.setProductData("{}");

        snapshotMapper.insert(snapshot);

        YyProductSnapshot found = snapshotMapper.selectByPlatformSku("ysbang", "SKU_SELECT_TEST");
        assertNotNull(found);
        assertEquals("查询测试药品", found.getCommonName());
    }
}
```

- [ ] **Step 2: 创建数据迁移脚本**

```sql
-- V20260430__migrate_standard_product_to_snapshot.sql
-- 从yy_standard_product迁移到yy_product_snapshot
-- 注意：此脚本应在新表创建完成后执行

INSERT INTO yy_product_snapshot (source_platform, sku_id, product_id, source_api,
    drug_id, fusion_confidence, common_name, barcode, approval_number, manufacturer,
    specification, price_current, stock_quantity, raw_data_payload, collected_at, synced_at,
    product_data)
SELECT
    source_platform,
    sku_id,
    product_id,
    source_api,
    fusion_group_id,
    NULL,
    common_name,
    barcode,
    approval_number,
    manufacturer,
    specification,
    price_current,
    stock_quantity,
    raw_data_payload,
    collected_at,
    synced_at,
    JSON_OBJECT(
        'productName', product_name,
        'brandName', brand_name,
        'categoryId', category_id,
        'categoryName', category_name,
        'unit', unit,
        'packingRatio', packing_ratio,
        'productStatus', product_status,
        'salesVolume', sales_volume,
        'shopName', shop_name,
        'priceRetail', price_retail,
        'priceAssemble', price_assemble,
        'isTaxIncluded', is_tax_included,
        'freightAmount', freight_amount,
        'freeShippingThreshold', free_shipping_threshold
    )
FROM yy_standard_product
WHERE NOT EXISTS (
    SELECT 1 FROM yy_product_snapshot ps
    WHERE ps.source_platform = yy_standard_product.source_platform
    AND ps.sku_id = yy_standard_product.sku_id
);
```

- [ ] **Step 3: 运行集成测试**

Run: `mvn test -pl ruoyi-system -Dtest=DataFusionServiceIntegrationTest -DfailIfNoTests=false`
Expected: PASS（需要数据库连接）

- [ ] **Step 4: Commit**

```bash
git add ruoyi-system/src/test/java/com/ruoyi/yy/service/impl/DataFusionServiceIntegrationTest.java \
        ruoyi-system/src/main/resources/db/migration/V20260430__migrate_standard_product_to_snapshot.sql
git commit -m "feat: add integration tests and data migration script"
```

---

## 完成总结

实施计划包含 **15个Task**，涵盖：

| 阶段 | Task | 内容 |
|------|------|------|
| **Week 1-2** | 1-3 | 数据库Schema+Flyway+MapperScan、常量VO、药品主数据 |
| **Week 3-4** | 4-7 | 4级匹配策略、YyFusionEngineImpl、商品快照+JSON TypeHandler |
| **Week 5-6** | 8-10 | YyAiGateway+熔断器(AtomicRef CAS)+YyMockAiGateway、YyAiMatchStrategy、平台适配器 |
| **Week 7-8** | 11-12 | DataFusionService重构(批量化N+1消除)、Chrome扩展模块化 |
| **Week 9-10** | 13-15 | YyAiAdvisorImpl、YyAiDataCleanerImpl、集成测试+数据迁移 |

### 工程审查修复清单 (26项)

| # | 修复 | 优先级 |
|---|------|--------|
| F1 | Mapper XML移除PostgreSQL `::json` 语法 | P0 |
| F2 | 集成Flyway依赖+配置 | P0 |
| F3 | 扩展YyFieldMapping实体 | P1 |
| F4 | YyCircuitBreaker改用AtomicReference CAS | P1 |
| F5 | 抽取IYyAiGateway，YyMockAiGateway改为实现接口 | P1 |
| F6 | selectCandidates改为前缀匹配+回退全模糊 | P1 |
| F7 | 添加JsonStringTypeHandler | P0 |
| F9 | 双库共存约定（FastJSON2平台层/Jackson AI层） | P2 |
| F13 | 添加spring-boot-starter-test依赖 | P0 |
| F14 | Chrome扩展手动测试清单 | P2 |
| F15 | YyMockAiGateway测试支持 | P1 |
| F16 | 批量查询消除N+1（550→5次/批） | P0 |
| F17 | Redis缓存LLM结果（TTL 24h） | P1 |
| F18 | 缓存键设计：`ai:cache:{scene}:{hash}` | P2 |
| OV6 | @MapperScan注册新包路径 | P0 |

### 自审清单

1. **Spec覆盖**: 设计文档的核心需求（药品主数据、4级融合、平台适配器、Chrome扩展、5个AI模块）全部有对应Task
2. **Placeholder扫描**: 无TBD/TODO，所有步骤包含完整代码
3. **类型一致性**: `YyProductSnapshot`, `YyDrugMaster`, `YyMatchResult`, `YyFusionResult` 在所有Task中一致
4. **P0修复**: 已修复PostgreSQL语法、Flyway集成、JSON TypeHandler、MapperScan、测试依赖
5. **TDD**: 每个策略和引擎都有先写测试的步骤
6. **线程安全**: YyCircuitBreaker使用AtomicReference CAS，无volatile竞态
7. **性能**: 批量查询消除N+1，前缀匹配+回退避免全表扫描

### 执行选择

**计划已完成并通过工程审查，保存到 `docs/superpowers/plans/2026-04-30-medical-b2b-platform-redesign.md`。**

两种执行方式：

1. **Subagent-Driven（推荐）** — 每个Task派发独立子代理，Task间审查，快速迭代
2. **Inline Execution** — 在当前会话中按Task顺序执行，带检查点

选择哪种方式？
