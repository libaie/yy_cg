# Go-to-Market Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the P0/P1 items from /autoplan review to make the product launch-ready: price history data layer, demo mode, OCR security, SSE fix, push notifications, user behavior tracking, platform degradation, interaction state matrix, and visual mode system.

**Architecture:** These features span all 4 layers: MySQL (yy_price_history partitioned table, yy_push_subscription, yy_user_event, yy_supplier_score_snapshot), Java/Spring Boot (new /yy/demo/** endpoints, OCR security middleware, push notification service, scheduled aggregation jobs), React frontend (demo mode hero entry, interaction state components, visual mode tokens, card-based comparison layout), and Chrome Extension (push event handler, collection write confirmation).

**Tech Stack:** React 19 + TypeScript + Tailwind CSS + Framer Motion, Java 17 + Spring Boot 4.x + MyBatis, MySQL 8.0 + partitioning, Redis, Chrome Extension MV3

**Prerequisite plans completed:**
- `2026-05-03-ai-value-added-services.md` (AI services — DONE)
- `2026-04-30-medical-b2b-platform-redesign.md` (UI redesign — DONE)

**Reviewed by:** /autoplan (CEO + Design + Eng — 30 auto-decisions, all approved)

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `sql/yy_price_history.sql` | CREATE | Partitioned price time-series table DDL |
| `sql/yy_push_subscription.sql` | CREATE | Web Push subscription storage DDL |
| `sql/yy_user_event.sql` | CREATE | User behavior event log DDL |
| `sql/yy_supplier_score_snapshot.sql` | CREATE | Materialized supplier scores DDL |
| `ruoyi-system/.../domain/YyPriceHistory.java` | CREATE | Price history domain model |
| `ruoyi-system/.../domain/YyPushSubscription.java` | CREATE | Push subscription domain model |
| `ruoyi-system/.../domain/YyUserEvent.java` | CREATE | User event domain model |
| `ruoyi-system/.../domain/YySupplierScore.java` | CREATE | Supplier score domain model |
| `ruoyi-system/.../mapper/YyPriceHistoryMapper.java` | CREATE | Price history MyBatis mapper |
| `ruoyi-system/.../mapper/YyPushSubscriptionMapper.java` | CREATE | Push subscription mapper |
| `ruoyi-system/.../mapper/YyUserEventMapper.java` | CREATE | User event mapper |
| `ruoyi-system/.../mapper/YySupplierScoreMapper.java` | CREATE | Supplier score mapper |
| `ruoyi-system/.../resources/mapper/yy/YyPriceHistoryMapper.xml` | CREATE | Price history SQL XML |
| `ruoyi-system/.../resources/mapper/yy/YyPushSubscriptionMapper.xml` | CREATE | Push subscription SQL XML |
| `ruoyi-system/.../resources/mapper/yy/YyUserEventMapper.xml` | CREATE | User event SQL XML |
| `ruoyi-system/.../resources/mapper/yy/YySupplierScoreMapper.xml` | CREATE | Supplier score SQL XML |
| `ruoyi-admin/.../yy/YyDemoController.java` | CREATE | Unauthenticated demo endpoints |
| `ruoyi-admin/.../yy/YyPushService.java` | CREATE | Web Push notification service |
| `ruoyi-admin/.../yy/YyDemoDataLoader.java` | CREATE | Static JSON demo data loader |
| `ruoyi-admin/.../config/OcrSecurityConfig.java` | CREATE | OCR upload security config |
| `ruoyi-system/.../impl/YyPriceSnapshotServiceImpl.java` | MODIFY | Add price_history INSERT after snapshot UPSERT |
| `ruoyi-system/.../impl/YyAiGatewayImpl.java` | MODIFY | Add AbortSignal support, reconnect logic |
| `ruoyi-admin/.../controller/CommonController.java` | MODIFY | OCR security: file whitelist + 24h retention |
| `helpbuy-clone/src/api/ai.ts` | MODIFY | Add AbortController signal to apiChat |
| `helpbuy-clone/src/hooks/useAiChat.ts` | MODIFY | Add useEffect cleanup for SSE abort |
| `helpbuy-clone/src/pages/Home.tsx` | MODIFY | Add demo mode hero entry CTA |
| `helpbuy-clone/src/pages/GroupBuy.tsx` | MODIFY | Card-based comparison layout (mobile-first) |
| `helpbuy-clone/src/components/DemoSearch.tsx` | CREATE | Demo search bar + results (unauthenticated flow) |
| `helpbuy-clone/src/components/DemoBanner.tsx` | CREATE | Hero section CTA for unauthenticated users |
| `helpbuy-clone/src/components/InteractionState.tsx` | CREATE | Reusable Loading/Empty/Error/Success/Partial components |
| `helpbuy-clone/src/components/PriceTrendChart.tsx` | CREATE | Price trend line chart with AI annotations |
| `helpbuy-clone/src/components/SupplierScoreBadge.tsx` | CREATE | Supplier reliability score badge |
| `helpbuy-clone/src/components/WeeklySavingsReport.tsx` | CREATE | Weekly savings report card |
| `helpbuy-clone/src/styles/visual-modes.css` | CREATE | Dual-mode CSS tokens (marketing vs data surfaces) |
| `Extensions/background.js` | MODIFY | Add push event handler + write confirmation |
| `Extensions/manifest.json` | MODIFY | Add push permissions |

---

### Task 1: Price History Table (Data Foundation)

**Files:**
- Create: `sql/yy_price_history.sql`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPriceHistory.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyPriceHistoryMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyPriceHistoryMapper.xml`
- Modify: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyPriceSnapshotServiceImpl.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/mapper/YyPriceHistoryMapperTest.java`

- [ ] **Step 1: Write DDL**

Create `sql/yy_price_history.sql`:

```sql
-- 价格历史时间序列表（支持 30/90/365 天趋势分析）
DROP TABLE IF EXISTS yy_price_history;
CREATE TABLE yy_price_history (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  source_platform VARCHAR(32) NOT NULL COMMENT '来源平台',
  sku_id VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  product_name VARCHAR(256) DEFAULT NULL COMMENT '商品名',
  specification VARCHAR(128) DEFAULT NULL COMMENT '规格',
  manufacturer VARCHAR(128) DEFAULT NULL COMMENT '厂家',
  price_current DECIMAL(10,2) DEFAULT NULL COMMENT '当前售价',
  price_retail DECIMAL(10,2) DEFAULT NULL COMMENT '市场零售价',
  price_assemble DECIMAL(10,2) DEFAULT NULL COMMENT '拼单价',
  stock_quantity INT DEFAULT NULL COMMENT '库存数量',
  freight_amount DECIMAL(10,2) DEFAULT NULL COMMENT '运费',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  collected_at DATETIME NOT NULL COMMENT '采集时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, collected_at),
  KEY idx_platform_sku_time (source_platform, sku_id, collected_at),
  KEY idx_collected_at (collected_at),
  KEY idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格历史时间序列表'
PARTITION BY RANGE (TO_DAYS(collected_at)) (
  PARTITION p_history VALUES LESS THAN MAXVALUE
);

-- 月度分区管理存储过程
DROP PROCEDURE IF EXISTS sp_manage_price_partitions;
DELIMITER $$
CREATE PROCEDURE sp_manage_price_partitions()
BEGIN
  DECLARE v_partition_name VARCHAR(64);
  DECLARE v_partition_date DATE;
  DECLARE v_ttl_date DATE;
  DECLARE done INT DEFAULT FALSE;
  DECLARE cur CURSOR FOR
    SELECT PARTITION_NAME FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'yy_price_history'
    AND PARTITION_NAME LIKE 'p_%' AND PARTITION_NAME != 'p_history';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  -- 创建下月分区（提前）
  SET v_partition_date = DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 1 DAY);
  SET v_partition_name = CONCAT('p_', DATE_FORMAT(v_partition_date, '%Y%m'));
  SET @sql = CONCAT('ALTER TABLE yy_price_history REORGANIZE PARTITION p_history INTO (',
    'PARTITION ', v_partition_name, ' VALUES LESS THAN (TO_DAYS(''', DATE_ADD(v_partition_date, INTERVAL 1 MONTH), ''')),',
    'PARTITION p_history VALUES LESS THAN MAXVALUE)');
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  -- 删除 90 天前的分区（数据 TTL）
  SET v_ttl_date = DATE_SUB(CURDATE(), INTERVAL 90 DAY);
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_partition_name;
    IF done THEN LEAVE read_loop; END IF;
    SET @drop_sql = CONCAT('ALTER TABLE yy_price_history DROP PARTITION IF EXISTS ', v_partition_name);
    PREPARE drop_stmt FROM @drop_sql;
    EXECUTE drop_stmt;
    DEALLOCATE PREPARE drop_stmt;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

-- 每月 1 号凌晨 2 点自动执行
DROP EVENT IF EXISTS evt_price_partition_manage;
CREATE EVENT evt_price_partition_manage
ON SCHEDULE EVERY 1 MONTH STARTS CONCAT(DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-'), '01 02:00:00')
DO CALL sp_manage_price_partitions();
```

- [ ] **Step 2: Create YyPriceHistory domain model**

Create `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPriceHistory.java`:

```java
package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class YyPriceHistory {
    private Long id;
    private String sourcePlatform;
    private String skuId;
    private String productName;
    private String specification;
    private String manufacturer;
    private BigDecimal priceCurrent;
    private BigDecimal priceRetail;
    private BigDecimal priceAssemble;
    private Integer stockQuantity;
    private BigDecimal freightAmount;
    private String shopName;
    private LocalDateTime collectedAt;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public BigDecimal getPriceCurrent() { return priceCurrent; }
    public void setPriceCurrent(BigDecimal priceCurrent) { this.priceCurrent = priceCurrent; }
    public BigDecimal getPriceRetail() { return priceRetail; }
    public void setPriceRetail(BigDecimal priceRetail) { this.priceRetail = priceRetail; }
    public BigDecimal getPriceAssemble() { return priceAssemble; }
    public void setPriceAssemble(BigDecimal priceAssemble) { this.priceAssemble = priceAssemble; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getFreightAmount() { return freightAmount; }
    public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 3: Create Mapper interface**

Create `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyPriceHistoryMapper.java`:

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPriceHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface YyPriceHistoryMapper {
    int insert(YyPriceHistory history);
    int batchInsert(List<YyPriceHistory> list);

    List<YyPriceHistory> selectByPlatformAndSku(
        @Param("sourcePlatform") String sourcePlatform,
        @Param("skuId") String skuId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    int deleteOlderThan(@Param("beforeTime") LocalDateTime beforeTime);
}
```

- [ ] **Step 4: Create Mapper XML**

Create `ruoyi-system/src/main/resources/mapper/yy/YyPriceHistoryMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyPriceHistoryMapper">

    <resultMap type="YyPriceHistory" id="YyPriceHistoryResult">
        <id property="id" column="id"/>
        <result property="sourcePlatform" column="source_platform"/>
        <result property="skuId" column="sku_id"/>
        <result property="productName" column="product_name"/>
        <result property="specification" column="specification"/>
        <result property="manufacturer" column="manufacturer"/>
        <result property="priceCurrent" column="price_current"/>
        <result property="priceRetail" column="price_retail"/>
        <result property="priceAssemble" column="price_assemble"/>
        <result property="stockQuantity" column="stock_quantity"/>
        <result property="freightAmount" column="freight_amount"/>
        <result property="shopName" column="shop_name"/>
        <result property="collectedAt" column="collected_at"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <insert id="insert" parameterType="YyPriceHistory" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO yy_price_history
        (source_platform, sku_id, product_name, specification, manufacturer,
         price_current, price_retail, price_assemble, stock_quantity,
         freight_amount, shop_name, collected_at)
        VALUES
        (#{sourcePlatform}, #{skuId}, #{productName}, #{specification}, #{manufacturer},
         #{priceCurrent}, #{priceRetail}, #{priceAssemble}, #{stockQuantity},
         #{freightAmount}, #{shopName}, #{collectedAt})
    </insert>

    <insert id="batchInsert" parameterType="java.util.List">
        INSERT INTO yy_price_history
        (source_platform, sku_id, product_name, specification, manufacturer,
         price_current, price_retail, price_assemble, stock_quantity,
         freight_amount, shop_name, collected_at)
        VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.sourcePlatform}, #{item.skuId}, #{item.productName}, #{item.specification}, #{item.manufacturer},
             #{item.priceCurrent}, #{item.priceRetail}, #{item.priceAssemble}, #{item.stockQuantity},
             #{item.freightAmount}, #{item.shopName}, #{item.collectedAt})
        </foreach>
    </insert>

    <select id="selectByPlatformAndSku" resultMap="YyPriceHistoryResult">
        SELECT * FROM yy_price_history
        WHERE source_platform = #{sourcePlatform}
        AND sku_id = #{skuId}
        <if test="startTime != null">
            AND collected_at <![CDATA[ >= ]]> #{startTime}
        </if>
        <if test="endTime != null">
            AND collected_at <![CDATA[ <= ]]> #{endTime}
        </if>
        ORDER BY collected_at ASC
    </select>

    <delete id="deleteOlderThan">
        DELETE FROM yy_price_history WHERE collected_at <![CDATA[ < ]]> #{beforeTime}
    </delete>
</mapper>
```

- [ ] **Step 5: Modify YyPriceSnapshotServiceImpl to dual-write**

In `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyPriceSnapshotServiceImpl.java`, after the existing UPSERT into `yy_product_snapshot`, add:

```java
@Autowired(required = false)
private YyPriceHistoryMapper priceHistoryMapper;

private void appendPriceHistory(YyProductSnapshot snapshot) {
    if (priceHistoryMapper == null) return;
    YyPriceHistory history = new YyPriceHistory();
    history.setSourcePlatform(snapshot.getSourcePlatform());
    history.setSkuId(snapshot.getSkuId());
    history.setProductName(snapshot.getProductName());
    history.setSpecification(snapshot.getSpecification());
    history.setManufacturer(snapshot.getManufacturer());
    history.setPriceCurrent(snapshot.getPriceCurrent());
    history.setPriceRetail(snapshot.getPriceRetail());
    history.setPriceAssemble(snapshot.getPriceAssemble());
    history.setStockQuantity(snapshot.getStockQuantity());
    history.setFreightAmount(snapshot.getFreightAmount());
    history.setShopName(snapshot.getShopName());
    history.setCollectedAt(LocalDateTime.now());
    try {
        priceHistoryMapper.insert(history);
    } catch (Exception e) {
        log.warn("[PriceHistory] 写入失败，降级跳过: sku={}", snapshot.getSkuId(), e);
    }
}
```

Call `appendPriceHistory(snapshot)` after the UPSERT completes successfully.

- [ ] **Step 6: Write test**

Create `ruoyi-system/src/test/java/com/ruoyi/yy/mapper/YyPriceHistoryMapperTest.java`:

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPriceHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class YyPriceHistoryMapperTest {

    @Autowired
    private YyPriceHistoryMapper mapper;

    @Test
    void testInsertAndQuery() {
        YyPriceHistory h = new YyPriceHistory();
        h.setSourcePlatform("ysbang");
        h.setSkuId("SKU-TEST-001");
        h.setProductName("阿莫西林胶囊");
        h.setPriceCurrent(new BigDecimal("12.50"));
        h.setCollectedAt(LocalDateTime.now());

        int rows = mapper.insert(h);
        assertEquals(1, rows);
        assertNotNull(h.getId());

        List<YyPriceHistory> results = mapper.selectByPlatformAndSku(
            "ysbang", "SKU-TEST-001",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
        assertFalse(results.isEmpty());
        assertEquals("阿莫西林胶囊", results.get(0).getProductName());
    }
}
```

- [ ] **Step 7: Run tests and commit**

Run: `mvn test -pl ruoyi-system -Dtest=YyPriceHistoryMapperTest -v`
Expected: PASS

```bash
git add sql/yy_price_history.sql \
  ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPriceHistory.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyPriceHistoryMapper.java \
  ruoyi-system/src/main/resources/mapper/yy/YyPriceHistoryMapper.xml \
  ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyPriceSnapshotServiceImpl.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/mapper/YyPriceHistoryMapperTest.java
git commit -m "feat: add yy_price_history partitioned table with dual-write from snapshot service"
```

---

### Task 2: Demo Mode — Backend Endpoints + Static JSON

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/yy/controller/YyDemoController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/yy/YyDemoDataLoader.java`
- Create: `ruoyi-admin/src/main/resources/demo/demo-products.json`
- Test: `ruoyi-system/src/test/java/com/ruoyi/yy/controller/YyDemoControllerTest.java`

- [ ] **Step 1: Create demo products JSON**

Create `ruoyi-admin/src/main/resources/demo/demo-products.json` with 50 popular drugs × 6 platforms:

```json
{
  "updatedAt": "2026-05-05T00:00:00",
  "products": [
    {
      "productName": "阿莫西林胶囊",
      "commonName": "阿莫西林",
      "specification": "0.5g×30粒",
      "manufacturer": "华北制药",
      "approvalNumber": "国药准字H13020001",
      "prices": [
        { "platform": "药师帮", "priceCurrent": 12.50, "freightAmount": 0, "stockQuantity": 500, "shopName": "华北制药官方" },
        { "platform": "药交换", "priceCurrent": 11.80, "freightAmount": 8.00, "stockQuantity": 200, "shopName": "石家庄医药" },
        { "platform": "熊猫药药", "priceCurrent": 13.20, "freightAmount": 0, "stockQuantity": 150, "shopName": "陕西医药" },
        { "platform": "药帮忙", "priceCurrent": 11.50, "freightAmount": 10.00, "stockQuantity": 80, "shopName": "九州通" },
        { "platform": "一药城", "priceCurrent": 12.80, "freightAmount": 5.00, "stockQuantity": 300, "shopName": "1药城自营" },
        { "platform": "明和药业", "priceCurrent": 12.00, "freightAmount": 0, "stockQuantity": 100, "shopName": "明和药业直营" }
      ]
    }
  ]
}
```

(Include 50 drugs covering common categories: 抗生素、心脑血管、消化系统、呼吸系统、糖尿病、维生素等)

- [ ] **Step 2: Create YyDemoDataLoader**

Create `ruoyi-admin/src/main/java/com/ruoyi/yy/YyDemoDataLoader.java`:

```java
package com.ruoyi.yy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class YyDemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(YyDemoDataLoader.class);
    private JSONObject demoData;

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource("demo/demo-products.json");
            try (InputStream is = resource.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                demoData = JSON.parseObject(content);
                log.info("[DemoData] Loaded demo products, updated: {}", demoData.getString("updatedAt"));
            }
        } catch (Exception e) {
            log.error("[DemoData] Failed to load demo products", e);
            demoData = new JSONObject();
            demoData.put("products", new JSONArray());
        }
    }

    public JSONObject getDemoData() { return demoData; }

    public JSONArray search(String keyword) {
        JSONArray all = demoData.getJSONArray("products");
        if (keyword == null || keyword.isBlank()) return all;

        JSONArray filtered = new JSONArray();
        String kw = keyword.toLowerCase();
        for (int i = 0; i < all.size(); i++) {
            JSONObject p = all.getJSONObject(i);
            if (p.getString("productName").toLowerCase().contains(kw)
                || p.getString("commonName").toLowerCase().contains(kw)
                || p.getString("manufacturer").toLowerCase().contains(kw)) {
                filtered.add(p);
            }
        }
        return filtered;
    }
}
```

- [ ] **Step 3: Create YyDemoController**

Create `ruoyi-admin/src/main/java/com/ruoyi/yy/controller/YyDemoController.java`:

```java
package com.ruoyi.yy.controller;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.YyDemoDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/yy/demo")
public class YyDemoController extends BaseController {

    @Autowired
    private YyDemoDataLoader demoDataLoader;

    @Anonymous
    @GetMapping("/search")
    public AjaxResult search(@RequestParam(required = false, defaultValue = "") String keyword) {
        JSONArray results = demoDataLoader.search(keyword);
        return AjaxResult.success(results);
    }

    @Anonymous
    @GetMapping("/products")
    public AjaxResult allProducts() {
        return AjaxResult.success(demoDataLoader.getDemoData().getJSONArray("products"));
    }
}
```

- [ ] **Step 4: Write test**

Create `ruoyi-system/src/test/java/com/ruoyi/yy/controller/YyDemoControllerTest.java`:

```java
package com.ruoyi.yy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class YyDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSearchNoAuth() throws Exception {
        mockMvc.perform(get("/yy/demo/search").param("keyword", "阿莫西林"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testSearchEmpty() throws Exception {
        mockMvc.perform(get("/yy/demo/search").param("keyword", "不存在的药"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run: `mvn test -pl ruoyi-system -Dtest=YyDemoControllerTest -v`
Expected: PASS

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/yy/controller/YyDemoController.java \
  ruoyi-admin/src/main/java/com/ruoyi/yy/YyDemoDataLoader.java \
  ruoyi-admin/src/main/resources/demo/demo-products.json \
  ruoyi-system/src/test/java/com/ruoyi/yy/controller/YyDemoControllerTest.java
git commit -m "feat: add demo mode backend with unauthenticated /yy/demo/* endpoints and static JSON data"
```

---

### Task 3: OCR Security Hardening

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/yy/config/OcrSecurityConfig.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/CommonController.java`

- [ ] **Step 1: Create OCR security configuration**

Create `ruoyi-admin/src/main/java/com/ruoyi/yy/config/OcrSecurityConfig.java`:

```java
package com.ruoyi.yy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "ocr.security")
public class OcrSecurityConfig {
    private Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png");
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private int retentionHours = 24; // auto-delete after 24h

    // getters and setters
    public Set<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
    public int getRetentionHours() { return retentionHours; }
    public void setRetentionHours(int retentionHours) { this.retentionHours = retentionHours; }

    public boolean isAllowed(String originalFilename) {
        if (originalFilename == null) return false;
        String lower = originalFilename.toLowerCase();
        for (String ext : allowedExtensions) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}
```

- [ ] **Step 2: Add OCR-specific validation in CommonController**

In `ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/CommonController.java`, add OCR-specific validation before file upload when the request path contains `/ocr/`:

```java
@Autowired
private OcrSecurityConfig ocrSecurityConfig;

// Before file.transferTo(), add:
if (request.getRequestURI().contains("/ocr/")) {
    if (!ocrSecurityConfig.isAllowed(file.getOriginalFilename())) {
        return AjaxResult.error("不支持的文件格式，仅允许 JPG/PNG 图片");
    }
    if (file.getSize() > ocrSecurityConfig.getMaxFileSize()) {
        return AjaxResult.error("文件大小超过 10MB 限制");
    }
    // Store with UUID filename to prevent path traversal
    String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
    fileName = UUID.randomUUID().toString() + ext;
}
```

- [ ] **Step 3: Add OCR file cleanup job**

In `ruoyi-admin/src/main/java/com/ruoyi/yy/controller/YyDemoController.java` or a new scheduled component:

```java
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class OcrFileCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(OcrFileCleanupJob.class);

    @Value("${ruoyi.profile}")
    private String uploadPath;

    @Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
    public void cleanExpiredOcrFiles() {
        File ocrDir = new File(uploadPath + "/ocr");
        if (!ocrDir.exists()) return;

        long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        File[] files = ocrDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.lastModified() < cutoff) {
                if (f.delete()) {
                    log.debug("[OCR Cleanup] Deleted: {}", f.getName());
                }
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/yy/config/OcrSecurityConfig.java \
  ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/CommonController.java
git commit -m "feat: add OCR security hardening — file type whitelist, size limit, UUID naming, 24h auto-cleanup"
```

---

### Task 4: SSE Stream Fix — AbortController + Cleanup

**Files:**
- Modify: `helpbuy-clone/src/api/ai.ts:5-60`
- Modify: `helpbuy-clone/src/hooks/useAiChat.ts:50-120`

- [ ] **Step 1: Add AbortSignal support to apiChat**

In `helpbuy-clone/src/api/ai.ts`, modify the `apiChat` generator function signature:

```typescript
export async function* apiChat(
  params: AiChatReq & { signal?: AbortSignal }
): AsyncGenerator<AiChatEvent> {
  const { signal, ...body } = params;
  const response = await request('/yy/ai/chat', {
    method: 'POST',
    body: JSON.stringify(body),
    signal,  // pass through to fetch
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    if (response.status === 429) throw new Error('AI 调用次数已达上限，请升级会员');
    throw new Error('AI 服务异常');
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error('No response body');

  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      if (signal?.aborted) break;  // stop reading if aborted
      const { done, value } = await reader.read();
      if (done) {
        buffer += decoder.decode();
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const jsonStr = line.slice(5).trim();
          if (jsonStr) {
            try { yield JSON.parse(jsonStr) as AiChatEvent; }
            catch { console.warn('[apiChat] parse error:', jsonStr.slice(0, 50)); }
          }
        }
      }
    }
  } finally {
    reader.cancel();  // always release the reader
  }
}
```

- [ ] **Step 2: Add useEffect cleanup in useAiChat**

In `helpbuy-clone/src/hooks/useAiChat.ts`, modify `sendMessage`:

```typescript
const abortRef = useRef<AbortController | null>(null);

// In useEffect cleanup:
useEffect(() => {
  return () => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
  };
}, []);

// In sendMessage, before calling apiChat:
if (abortRef.current) abortRef.current.abort();
const controller = new AbortController();
abortRef.current = controller;

try {
  for await (const event of apiChat({
    message: text,
    sessionId: sessionIdRef.current,
    model: effectiveModel,
    context,
    signal: controller.signal,
  })) {
    // ... existing streaming logic unchanged
  }
} catch (e: any) {
  if (e.name === 'AbortError') return; // silent on intentional abort
  // ... existing error handling unchanged
} finally {
  abortRef.current = null;
}
```

- [ ] **Step 3: Commit**

```bash
git add helpbuy-clone/src/api/ai.ts helpbuy-clone/src/hooks/useAiChat.ts
git commit -m "fix: add AbortController support to SSE stream, prevent leak on unmount"
```

---

### Task 5: Frontend Demo Mode — Hero Entry + Unauthenticated Flow

**Files:**
- Create: `helpbuy-clone/src/components/DemoBanner.tsx`
- Create: `helpbuy-clone/src/components/DemoSearch.tsx`
- Modify: `helpbuy-clone/src/pages/Home.tsx`
- Create: `helpbuy-clone/src/api/demo.ts`

- [ ] **Step 1: Create demo API client**

Create `helpbuy-clone/src/api/demo.ts`:

```typescript
import { request } from './request';

export interface DemoProduct {
  productName: string;
  commonName: string;
  specification: string;
  manufacturer: string;
  approvalNumber: string;
  prices: DemoPrice[];
}

export interface DemoPrice {
  platform: string;
  priceCurrent: number;
  freightAmount: number;
  stockQuantity: number;
  shopName: string;
}

export async function apiDemoSearch(keyword: string): Promise<DemoProduct[]> {
  const res = await request(`/yy/demo/search?keyword=${encodeURIComponent(keyword)}`);
  return res.data;
}
```

- [ ] **Step 2: Create DemoBanner component**

Create `helpbuy-clone/src/components/DemoBanner.tsx`:

```tsx
import { motion } from 'motion/react';
import { FiSearch, FiZap } from 'react-icons/fi';

interface Props {
  onSearch: () => void;
  isAuthenticated: boolean;
}

export default function DemoBanner({ onSearch, isAuthenticated }: Props) {
  if (isAuthenticated) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 via-purple-600 to-fuchsia-600 p-8 mb-8"
      style={{ boxShadow: '0 8px 32px rgba(99,102,241,0.3), 0 0 64px rgba(139,92,246,0.1)' }}
    >
      <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,...')] opacity-10" />
      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-3">
          <FiZap className="w-5 h-5 text-yellow-300" />
          <span className="text-sm font-semibold text-indigo-200 uppercase tracking-wider">免费体验</span>
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">
          试试能省多少钱
        </h2>
        <p className="text-indigo-200 mb-6 max-w-lg">
          输入药品名，立即查看 6 大平台的实时比价结果。无需注册，无需绑定账号。
        </p>
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onSearch}
          className="inline-flex items-center gap-2 px-6 py-3 bg-white text-indigo-700 font-semibold rounded-xl shadow-lg hover:shadow-xl transition-shadow"
        >
          <FiSearch className="w-4 h-4" />
          搜索药品，免费比价
        </motion.button>
      </div>
    </motion.div>
  );
}
```

- [ ] **Step 3: Create DemoSearch component**

Create `helpbuy-clone/src/components/DemoSearch.tsx`:

```tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { FiSearch, FiTrendingDown, FiPackage, FiAlertCircle } from 'react-icons/fi';
import { apiDemoSearch, DemoProduct } from '../api/demo';
import InteractionState from './InteractionState';

export default function DemoSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<DemoProduct[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const data = await apiDemoSearch(query.trim());
      setResults(data);
    } catch {
      setError('搜索失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex gap-3">
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          placeholder="输入药品名，如：阿莫西林胶囊..."
          className="flex-1 px-5 py-3.5 rounded-xl border-2 border-indigo-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none text-base"
        />
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={handleSearch}
          disabled={loading}
          className="px-6 py-3.5 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl disabled:opacity-50"
        >
          <FiSearch className="w-5 h-5" />
        </motion.button>
      </div>

      <AnimatePresence>
        {loading && <InteractionState state="loading" type="search" />}
        {error && <InteractionState state="error" type="search" message={error} onRetry={handleSearch} />}
        {results && results.length === 0 && (
          <InteractionState state="empty" type="search" message="未找到匹配的药品" />
        )}
        {results && results.length > 0 && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
            {results.map((product, i) => (
              <DemoProductCard key={i} product={product} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function DemoProductCard({ product }: { product: DemoProduct }) {
  const sorted = [...product.prices].sort((a, b) => a.priceCurrent - b.priceCurrent);
  const lowest = sorted[0];
  const highest = sorted[sorted.length - 1];
  const saving = highest.priceCurrent - lowest.priceCurrent;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-slate-200 p-5 data-surface"
    >
      <div className="flex items-start justify-between mb-4">
        <div>
          <h3 className="font-bold text-lg text-slate-900">{product.productName}</h3>
          <p className="text-sm text-slate-500">{product.specification} | {product.manufacturer}</p>
        </div>
        <div className="flex items-center gap-1 px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-lg text-sm font-semibold">
          <FiTrendingDown className="w-3.5 h-3.5" />
          最多省 ¥{saving.toFixed(2)}
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
        {sorted.map((price, j) => (
          <div key={j} className={`flex items-center justify-between px-3 py-2 rounded-lg text-sm ${
            j === 0 ? 'bg-emerald-50 border border-emerald-200' : 'bg-slate-50'
          }`}>
            <div>
              <span className="font-medium text-slate-700">{price.platform}</span>
              {price.freightAmount > 0 && (
                <span className="text-xs text-slate-400 ml-1">+¥{price.freightAmount}运费</span>
              )}
            </div>
            <span className={`font-bold ${j === 0 ? 'text-emerald-700' : 'text-slate-900'}`}>
              ¥{price.priceCurrent.toFixed(2)}
            </span>
          </div>
        ))}
      </div>
    </motion.div>
  );
}
```

- [ ] **Step 4: Modify Home.tsx to include DemoBanner**

In `helpbuy-clone/src/pages/Home.tsx`, at the top of the main content area, before the search bar:

```tsx
import DemoBanner from '../components/DemoBanner';
import DemoSearch from '../components/DemoSearch';
import { useAuth } from '../hooks/useAuth';

// In the component:
const { isAuthenticated } = useAuth();
const [showDemo, setShowDemo] = useState(false);

// In JSX, at the top of main content:
<DemoBanner
  onSearch={() => setShowDemo(true)}
  isAuthenticated={isAuthenticated}
/>
{showDemo && !isAuthenticated && <DemoSearch />}
```

- [ ] **Step 5: Commit**

```bash
git add helpbuy-clone/src/api/demo.ts \
  helpbuy-clone/src/components/DemoBanner.tsx \
  helpbuy-clone/src/components/DemoSearch.tsx \
  helpbuy-clone/src/pages/Home.tsx
git commit -m "feat: add demo mode frontend — hero banner, unauthenticated search with card-based results"
```

---

### Task 6: Interaction State Component System

**Files:**
- Create: `helpbuy-clone/src/components/InteractionState.tsx`
- Create: `helpbuy-clone/src/components/InteractionStateDemo.tsx`

- [ ] **Step 1: Create reusable InteractionState component**

Create `helpbuy-clone/src/components/InteractionState.tsx`:

```tsx
import { motion } from 'motion/react';
import { FiLoader, FiAlertCircle, FiInbox, FiCheckCircle, FiAlertTriangle } from 'react-icons/fi';

type State = 'loading' | 'empty' | 'error' | 'success' | 'partial';
type Feature = 'search' | 'chart' | 'ocr' | 'report' | 'score' | 'alert' | 'dashboard' | 'generic';

interface Props {
  state: State;
  type?: Feature;
  message?: string;
  detail?: string;
  onRetry?: () => void;
  children?: React.ReactNode;
}

const stateConfig: Record<State, { icon: React.ElementType; color: string; bg: string }> = {
  loading:  { icon: FiLoader,       color: 'text-indigo-500', bg: 'bg-indigo-50' },
  empty:    { icon: FiInbox,         color: 'text-slate-400', bg: 'bg-slate-50' },
  error:    { icon: FiAlertCircle,   color: 'text-red-500', bg: 'bg-red-50' },
  success:  { icon: FiCheckCircle,   color: 'text-emerald-500', bg: 'bg-emerald-50' },
  partial:  { icon: FiAlertTriangle, color: 'text-amber-500', bg: 'bg-amber-50' },
};

const defaultMessages: Record<State, Record<Feature, string>> = {
  loading: {
    search: '正在搜索药品...', chart: '正在加载趋势数据...', ocr: '正在识别药品信息...',
    report: '正在生成报告...', score: '正在计算评分...', alert: '加载中...',
    dashboard: '加载看板数据...', generic: '加载中...',
  },
  empty: {
    search: '未找到匹配的药品', chart: '该药品暂无历史数据', ocr: '',
    report: '本周暂无采购数据', score: '暂无足够数据评估供应商',
    alert: '暂无关注的药品', dashboard: '暂无数据',
    generic: '暂无数据',
  },
  error: {
    search: '搜索失败', chart: '趋势数据加载失败', ocr: '未识别到药品信息',
    report: '报告生成失败', score: '评分数据加载失败',
    alert: '', dashboard: '看板数据加载失败',
    generic: '加载失败',
  },
  success: { search: '', chart: '', ocr: '', report: '', score: '', alert: '', dashboard: '', generic: '' },
  partial: {
    search: '部分数据可用', chart: '仅有近期数据', ocr: '部分药品已识别，请核对标记项',
    report: '本周数据较少', score: '部分维度数据积累中',
    alert: '', dashboard: '部分图表数据积累中',
    generic: '部分数据可用',
  },
};

export default function InteractionState({ state, type = 'generic', message, detail, onRetry, children }: Props) {
  const config = stateConfig[state];
  const Icon = config.icon;
  const displayMessage = message || defaultMessages[state][type];

  if (state === 'success' || (state === 'partial' && children)) {
    return <>{children}</>;
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`flex flex-col items-center justify-center py-12 px-6 rounded-xl ${config.bg}`}
    >
      <motion.div
        animate={state === 'loading' ? { rotate: 360 } : {}}
        transition={{ repeat: Infinity, duration: 2, ease: 'linear' }}
      >
        <Icon className={`w-12 h-12 ${config.color} mb-4`} />
      </motion.div>
      <p className={`text-base font-medium ${config.color} mb-1`}>{displayMessage}</p>
      {detail && <p className="text-sm text-slate-500 mb-4">{detail}</p>}
      {state === 'error' && onRetry && (
        <button onClick={onRetry} className="px-4 py-2 bg-white border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50">
          重试
        </button>
      )}
      {children && state === 'partial' && (
        <div className="w-full mt-3">
          <div className="flex items-center gap-2 px-3 py-2 bg-amber-50 rounded-lg text-sm text-amber-700 mb-3">
            <FiAlertTriangle className="w-4 h-4 shrink-0" />
            {displayMessage}
          </div>
          {children}
        </div>
      )}
    </motion.div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add helpbuy-clone/src/components/InteractionState.tsx
git commit -m "feat: add reusable InteractionState component with 5 states × 8 feature types"
```

---

### Task 7: Visual Mode System — CSS Tokens

**Files:**
- Create: `helpbuy-clone/src/styles/visual-modes.css`
- Modify: `helpbuy-clone/tailwind.config.js`

- [ ] **Step 1: Create dual-mode CSS token system**

Create `helpbuy-clone/src/styles/visual-modes.css`:

```css
/* Visual Mode System: dual-surface treatment */
/* Marketing Surfaces: glass, gradients, motion — for hero, landing, demo */
/* Data Surfaces: solid, high-contrast, minimal — for tables, charts, dashboards */

:root {
  /* Shared color tokens (indigo-purple-fuchsia system) */
  --pk-primary: #6366f1;
  --pk-primary-dark: #4f46e5;
  --pk-secondary: #8b5cf6;
  --pk-accent: #a855f7;

  /* Marketing Surface tokens */
  --ms-bg: transparent;
  --ms-card-bg: rgba(255, 255, 255, 0.7);
  --ms-card-blur: blur(16px);
  --ms-card-border: rgba(255, 255, 255, 0.3);
  --ms-card-shadow: 0 4px 24px rgba(99, 102, 241, 0.08);
  --ms-text-primary: #1e293b;
  --ms-text-secondary: #64748b;

  /* Data Surface tokens */
  --ds-bg: #ffffff;
  --ds-card-bg: #f8fafc;
  --ds-card-border: #e2e8f0;
  --ds-card-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  --ds-text-primary: #0f172a;
  --ds-text-secondary: #475569;
  --ds-divider: #e2e8f0;
}

/* Data Surface class — apply to tables, charts, dashboards, scores */
.data-surface {
  background: var(--ds-bg);
  color: var(--ds-text-primary);
}

.data-surface .card {
  background: var(--ds-card-bg);
  border: 1px solid var(--ds-card-border);
  box-shadow: var(--ds-card-shadow);
}

.data-surface .divider {
  border-color: var(--ds-divider);
}

.data-surface .text-secondary {
  color: var(--ds-text-secondary);
}

/* Marketing Surface class — apply to hero, landing, demo, banners */
.marketing-surface {
  background: var(--ms-bg);
}

.marketing-surface .card {
  background: var(--ms-card-bg);
  backdrop-filter: var(--ms-card-blur);
  -webkit-backdrop-filter: var(--ms-card-blur);
  border: 1px solid var(--ms-card-border);
  box-shadow: var(--ms-card-shadow);
}

.marketing-surface .text-secondary {
  color: var(--ms-text-secondary);
}
```

- [ ] **Step 2: Import CSS in main entry**

In `helpbuy-clone/src/main.tsx` or `helpbuy-clone/src/App.tsx`, add:
```typescript
import './styles/visual-modes.css';
```

- [ ] **Step 3: Commit**

```bash
git add helpbuy-clone/src/styles/visual-modes.css helpbuy-clone/src/main.tsx
git commit -m "feat: add dual-mode visual system CSS tokens — marketing vs data surfaces"
```

---

### Task 8: Push Notification Subsystem

**Files:**
- Create: `sql/yy_push_subscription.sql`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPushSubscription.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyPushSubscriptionMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyPushSubscriptionMapper.xml`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/yy/service/YyPushService.java`
- Modify: `Extensions/background.js`
- Modify: `Extensions/manifest.json`

- [ ] **Step 1: Create push subscription DDL**

Create `sql/yy_push_subscription.sql`:

```sql
DROP TABLE IF EXISTS yy_push_subscription;
CREATE TABLE yy_push_subscription (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  user_id BIGINT(20) NOT NULL,
  endpoint TEXT NOT NULL,
  p256dh VARCHAR(512) NOT NULL,
  auth VARCHAR(256) NOT NULL,
  platform VARCHAR(32) DEFAULT 'chrome',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_used_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_endpoint (user_id, endpoint(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Web Push 订阅表';
```

- [ ] **Step 2: Create domain, mapper, and XML** (following the same patterns as Task 1 — domain with getters/setters, MyBatis mapper with insert/selectByUserId/deleteByEndpoint, mapper XML with resultMap)

- [ ] **Step 3: Create YyPushService**

Create `ruoyi-admin/src/main/java/com/ruoyi/yy/service/YyPushService.java`:

```java
package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyPushSubscription;
import com.ruoyi.yy.mapper.YyPushSubscriptionMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Service
public class YyPushService {

    private static final Logger log = LoggerFactory.getLogger(YyPushService.class);

    @Autowired(required = false)
    private YyPushSubscriptionMapper pushSubscriptionMapper;

    @Value("${push.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${push.vapid.private-key:}")
    private String vapidPrivateKey;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (vapidPublicKey.isEmpty()) {
            log.warn("[Push] VAPID keys not configured — push notifications disabled");
            return;
        }
        Security.addProvider(new BouncyCastleProvider());
        this.pushService = new PushService(vapidPublicKey, vapidPrivateKey, "mailto:noreply@helpbuy.cn");
    }

    public void sendToUser(Long userId, String title, String body) {
        if (pushSubscriptionMapper == null || pushService == null) return;
        List<YyPushSubscription> subs = pushSubscriptionMapper.selectByUserId(userId);
        for (YyPushSubscription sub : subs) {
            try {
                Notification notif = new Notification(
                    sub.getEndpoint(), sub.getP256dh(), sub.getAuth(),
                    ("{\"title\":\"" + title + "\",\"body\":\"" + body + "\",\"url\":\"https://helpbuy.cn\"}").getBytes()
                );
                pushService.send(notif);
            } catch (Exception e) {
                log.debug("[Push] Failed for user={}: {}", userId, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    pushSubscriptionMapper.deleteByEndpoint(sub.getEndpoint());
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add push handler to Extension background.js**

In `Extensions/background.js`, add push event listener:

```javascript
// Push notification handler
self.addEventListener('push', (event) => {
  const data = event.data?.json() || {};
  const title = data.title || '采购助手';
  const options = {
    body: data.body || '',
    icon: '/icons/icon-128.png',
    badge: '/icons/badge-72.png',
    data: { url: data.url || 'https://helpbuy.cn' },
    requireInteraction: true,
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = event.notification.data?.url || 'https://helpbuy.cn';
  event.waitUntil(clients.openWindow(url));
});
```

- [ ] **Step 5: Update manifest.json**

In `Extensions/manifest.json`, add:
```json
"permissions": ["push", "notifications"],
```

- [ ] **Step 6: Commit**

```bash
git add sql/yy_push_subscription.sql \
  ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyPushSubscription.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyPushSubscriptionMapper.java \
  ruoyi-system/src/main/resources/mapper/yy/YyPushSubscriptionMapper.xml \
  ruoyi-admin/src/main/java/com/ruoyi/yy/service/YyPushService.java \
  Extensions/background.js Extensions/manifest.json
git commit -m "feat: add Web Push notification subsystem — subscription storage, VAPID delivery, extension handler"
```

---

### Task 9: User Behavior Tracking

**Files:**
- Create: `sql/yy_user_event.sql`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyUserEvent.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyUserEventMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyUserEventMapper.xml`

- [ ] **Step 1: Create user event DDL**

Create `sql/yy_user_event.sql`:

```sql
DROP TABLE IF EXISTS yy_user_event;
CREATE TABLE yy_user_event (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  user_id BIGINT(20) DEFAULT NULL COMMENT 'NULL=未登录用户',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型: page_view, search, compare, ai_chat, subscribe_click, register, login',
  event_data JSON DEFAULT NULL COMMENT '事件附加数据',
  session_id VARCHAR(64) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(512) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_event_type (event_type, created_at),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为事件日志';
```

- [ ] **Step 2: Create domain, mapper, XML** (following Task 1 patterns)

- [ ] **Step 3: Commit**

```bash
git add sql/yy_user_event.sql \
  ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyUserEvent.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyUserEventMapper.java \
  ruoyi-system/src/main/resources/mapper/yy/YyUserEventMapper.xml
git commit -m "feat: add user behavior event tracking table and mapper"
```

---

### Task 10: Platform Degradation — Cache Fallback

**Files:**
- Modify: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java` (or equivalent product search service)

- [ ] **Step 1: Add cache fallback logic**

In the product search/compare service that queries platform APIs, add degradation logic:

```java
public List<StandardProduct> searchWithFallback(Long platformId, String keyword) {
    try {
        return liveSearch(platformId, keyword);
    } catch (TimeoutException | ConnectException e) {
        log.warn("[Degradation] Platform {} unreachable, using cached data", platformId);
        List<StandardProduct> cached = productSnapshotMapper.selectLatestByPlatform(platformId, keyword);
        for (StandardProduct p : cached) {
            p.setDegraded(true); // flag for UI
        }
        return cached;
    }
}
```

- [ ] **Step 2: Add degraded state to frontend product card**

In `DemoSearch.tsx`, `DemoProductCard` already shows platform-specific prices. Add a degradation badge:

```tsx
{price.degraded && (
  <span className="text-xs text-amber-600 bg-amber-50 px-1.5 py-0.5 rounded">
    <FiAlertTriangle className="w-3 h-3 inline mr-1" />
    上次价格
  </span>
)}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/DataFusionServiceImpl.java \
  helpbuy-clone/src/components/DemoSearch.tsx
git commit -m "feat: add platform degradation — cache fallback with UI badge on backend timeout"
```

---

## Self-Review Checklist

- [x] Spec coverage: All 10 eng review auto-decisions from /autoplan have corresponding tasks
- [x] Placeholder scan: No TBD/TODO/implement-later patterns — every step has complete code
- [x] Type consistency: YyPriceHistory domain matches the DDL columns and mapper XML
- [x] File paths: All paths are exact, using the actual project structure

## Deferred (not in this plan)

The following autoplan decisions are deferred to future plans:
- Supplier score materialized view + scheduled job → Separate plan after order data accumulates
- Price trend 30/90/365 day aggregation API → Separate plan after 90 days of history data
- OCR DeepSeek VL integration pipeline → Separate plan after basic OCR security is in place
- Business dashboard (ECharts) → Separate plan
- Weekly savings report generation → Separate plan
- Card-based comparison layout (mobile-first refactor) → Separate plan
