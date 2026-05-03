# AI 增值服务实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为医药 B2B 采购平台 C 端实现自建 AI 增值服务（采购顾问、比价解读、药品问答、智能推荐），替换第三方 chatbot iframe。

**Architecture:** 后端新增 YyAiController 统一入口 + 4 个 AI 服务 + 意图路由 + 配额系统。前端重写 AIAssistant 为原生聊天 UI + 快捷卡片。管理端新增配额配置页面。

**Tech Stack:** Spring Boot + MyBatis (后端) / React 19 + TypeScript + Tailwind + Framer Motion (C端) / Vue + Element UI (管理端) / DashScope qwen-turbo (AI) / SSE (流式通信)

---

## File Structure

### 后端新增 (ruoyi-system)
- `src/main/java/com/ruoyi/yy/domain/YyAiQuotaConfig.java` — 配额配置实体
- `src/main/java/com/ruoyi/yy/domain/YyAiUsageLog.java` — 用量日志实体
- `src/main/java/com/ruoyi/yy/mapper/YyAiQuotaConfigMapper.java` — 配额配置 Mapper
- `src/main/java/com/ruoyi/yy/mapper/YyAiUsageLogMapper.java` — 用量日志 Mapper
- `src/main/resources/mapper/yy/YyAiQuotaConfigMapper.xml` — 配额 Mapper XML
- `src/main/resources/mapper/yy/YyAiUsageLogMapper.xml` — 用量 Mapper XML
- `src/main/resources/db/migration/V20260503__create_ai_quota_tables.sql` — Flyway 迁移
- `src/main/java/com/ruoyi/yy/service/impl/YyAiUsageService.java` — 配额检查 + 用量记录
- `src/main/java/com/ruoyi/yy/service/impl/YyAiIntentRouter.java` — 意图识别路由
- `src/main/java/com/ruoyi/yy/service/impl/YyAiDrugQaImpl.java` — 药品问答
- `src/main/java/com/ruoyi/yy/service/impl/YyAiInsightImpl.java` — 比价解读
- `src/main/java/com/ruoyi/yy/service/impl/YyAiRecommendImpl.java` — 智能推荐
- `src/main/java/com/ruoyi/yy/controller/YyAiController.java` — 统一 API 入口

### 后端测试
- `src/test/java/com/ruoyi/yy/YyAiUsageServiceTest.java`
- `src/test/java/com/ruoyi/yy/YyAiIntentRouterTest.java`
- `src/test/java/com/ruoyi/yy/YyAiDrugQaImplTest.java`
- `src/test/java/com/ruoyi/yy/YyAiInsightImplTest.java`
- `src/test/java/com/ruoyi/yy/YyAiRecommendImplTest.java`

### 管理端 (ruoyi-ui)
- `src/api/yy/aiQuota.js` — 配额配置 API
- `src/views/yy/aiQuota/index.vue` — 配额配置页面
- `src/router/index.js` — 路由配置（修改）

### C 端 (helpbuy-clone)
- `src/api/ai.ts` — AI 相关 API 调用
- `src/hooks/useAiChat.ts` — 聊天状态管理 hook
- `src/components/AIAssistant.tsx` — 重写：原生聊天 UI
- `src/components/ai/ChatMessage.tsx` — 消息气泡
- `src/components/ai/QuickActions.tsx` — 快捷功能卡片
- `src/components/ai/ChatInput.tsx` — 输入框组件
- `src/api/types.ts` — 修改：新增 AI 类型

---

## Task 1: 配额表 + Flyway 迁移 + 实体 + Mapper

**Files:**
- Create: `ruoyi-system/src/main/resources/db/migration/V20260503__create_ai_quota_tables.sql`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiQuotaConfig.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiUsageLog.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyAiQuotaConfigMapper.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyAiUsageLogMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyAiQuotaConfigMapper.xml`
- Create: `ruoyi-system/src/main/resources/mapper/yy/YyAiUsageLogMapper.xml`

- [ ] **Step 1: Create Flyway migration script**

```sql
-- V20260503__create_ai_quota_tables.sql

CREATE TABLE IF NOT EXISTS yy_ai_quota_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tier_level INT NOT NULL COMMENT '会员等级 (0=regular, 1=gold, 2=platinum, 3=diamond)',
    daily_chat_limit INT DEFAULT 10 COMMENT '每日对话次数 (-1=无限)',
    daily_tool_limit INT DEFAULT 3 COMMENT '每日快捷功能次数 (-1=无限)',
    max_tokens_per_req INT DEFAULT 800 COMMENT '单次请求最大 token 数',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 AI 功能',
    create_by VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tier_level (tier_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 功能配额配置';

CREATE TABLE IF NOT EXISTS yy_ai_usage_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    usage_type VARCHAR(20) NOT NULL COMMENT 'chat/tool',
    tool_name VARCHAR(50) DEFAULT NULL COMMENT 'advisor/insight/drug_qa/recommend',
    tokens_used INT DEFAULT 0 COMMENT '消耗 token 数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_date (user_id, create_time),
    KEY idx_usage_type (usage_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 功能用量日志';

INSERT INTO yy_ai_quota_config (tier_level, daily_chat_limit, daily_tool_limit, max_tokens_per_req, enabled) VALUES
(0, 10, 3, 800, 1),
(1, 50, -1, 800, 1),
(2, -1, -1, 1200, 1),
(3, -1, -1, 1600, 1);
```

- [ ] **Step 2: Create YyAiQuotaConfig entity**

```java
package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class YyAiQuotaConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer tierLevel;
    private Integer dailyChatLimit;
    private Integer dailyToolLimit;
    private Integer maxTokensPerReq;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTierLevel() { return tierLevel; }
    public void setTierLevel(Integer tierLevel) { this.tierLevel = tierLevel; }
    public Integer getDailyChatLimit() { return dailyChatLimit; }
    public void setDailyChatLimit(Integer dailyChatLimit) { this.dailyChatLimit = dailyChatLimit; }
    public Integer getDailyToolLimit() { return dailyToolLimit; }
    public void setDailyToolLimit(Integer dailyToolLimit) { this.dailyToolLimit = dailyToolLimit; }
    public Integer getMaxTokensPerReq() { return maxTokensPerReq; }
    public void setMaxTokensPerReq(Integer maxTokensPerReq) { this.maxTokensPerReq = maxTokensPerReq; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
```

- [ ] **Step 3: Create YyAiUsageLog entity**

```java
package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

public class YyAiUsageLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String usageType;
    private String toolName;
    private Integer tokensUsed;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 4: Create YyAiQuotaConfigMapper**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import java.util.List;

public interface YyAiQuotaConfigMapper {
    YyAiQuotaConfig selectByTierLevel(Integer tierLevel);
    List<YyAiQuotaConfig> selectAll();
    int updateById(YyAiQuotaConfig config);
}
```

- [ ] **Step 5: Create YyAiUsageLogMapper**

```java
package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyAiUsageLog;
import org.apache.ibatis.annotations.Param;

public interface YyAiUsageLogMapper {
    int insert(YyAiUsageLog log);
    int countTodayByUserAndType(@Param("userId") Long userId, @Param("usageType") String usageType);
}
```

- [ ] **Step 6: Create Mapper XMLs**

`YyAiQuotaConfigMapper.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyAiQuotaConfigMapper">
    <resultMap type="com.ruoyi.yy.domain.YyAiQuotaConfig" id="YyAiQuotaConfigResult">
        <id property="id" column="id"/>
        <result property="tierLevel" column="tier_level"/>
        <result property="dailyChatLimit" column="daily_chat_limit"/>
        <result property="dailyToolLimit" column="daily_tool_limit"/>
        <result property="maxTokensPerReq" column="max_tokens_per_req"/>
        <result property="enabled" column="enabled"/>
        <result property="createBy" column="create_by"/>
        <result property="createTime" column="create_time"/>
        <result property="updateBy" column="update_by"/>
        <result property="updateTime" column="update_time"/>
    </resultMap>

    <select id="selectByTierLevel" parameterType="Integer" resultMap="YyAiQuotaConfigResult">
        select * from yy_ai_quota_config where tier_level = #{tierLevel}
    </select>

    <select id="selectAll" resultMap="YyAiQuotaConfigResult">
        select * from yy_ai_quota_config order by tier_level
    </select>

    <update id="updateById" parameterType="com.ruoyi.yy.domain.YyAiQuotaConfig">
        update yy_ai_quota_config
        <set>
            <if test="dailyChatLimit != null">daily_chat_limit = #{dailyChatLimit},</if>
            <if test="dailyToolLimit != null">daily_tool_limit = #{dailyToolLimit},</if>
            <if test="maxTokensPerReq != null">max_tokens_per_req = #{maxTokensPerReq},</if>
            <if test="enabled != null">enabled = #{enabled},</if>
            <if test="updateBy != null">update_by = #{updateBy},</if>
            update_time = sysdate()
        </set>
        where id = #{id}
    </update>
</mapper>
```

`YyAiUsageLogMapper.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.yy.mapper.YyAiUsageLogMapper">
    <insert id="insert" parameterType="com.ruoyi.yy.domain.YyAiUsageLog" useGeneratedKeys="true" keyProperty="id">
        insert into yy_ai_usage_log (user_id, usage_type, tool_name, tokens_used)
        values (#{userId}, #{usageType}, #{toolName}, #{tokensUsed})
    </insert>

    <select id="countTodayByUserAndType" resultType="int">
        select count(*) from yy_ai_usage_log
        where user_id = #{userId}
          and usage_type = #{usageType}
          and DATE(create_time) = CURDATE()
    </select>
</mapper>
```

- [ ] **Step 7: Verify compilation**

Run: `cd ruoyi-system && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add ruoyi-system/src/main/resources/db/migration/V20260503__create_ai_quota_tables.sql \
  ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiQuotaConfig.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/domain/YyAiUsageLog.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyAiQuotaConfigMapper.java \
  ruoyi-system/src/main/java/com/ruoyi/yy/mapper/YyAiUsageLogMapper.java \
  ruoyi-system/src/main/resources/mapper/yy/YyAiQuotaConfigMapper.xml \
  ruoyi-system/src/main/resources/mapper/yy/YyAiUsageLogMapper.xml
git commit -m "feat: add AI quota config and usage log tables, entities, and mappers"
```

---

## Task 2: YyAiUsageService 用量统计 + 配额检查

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiUsageService.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiUsageServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import com.ruoyi.yy.mapper.YyAiQuotaConfigMapper;
import com.ruoyi.yy.mapper.YyAiUsageLogMapper;
import com.ruoyi.yy.service.impl.YyAiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiUsageServiceTest {

    private YyAiQuotaConfigMapper quotaMapper;
    private YyAiUsageLogMapper usageLogMapper;
    private YyAiUsageService service;

    @BeforeEach
    void setUp() {
        quotaMapper = mock(YyAiQuotaConfigMapper.class);
        usageLogMapper = mock(YyAiUsageLogMapper.class);
        service = new YyAiUsageService(quotaMapper, usageLogMapper);
    }

    @Test
    void checkQuota_unlimited_returnsTrue() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(-1);
        when(quotaMapper.selectByTierLevel(2)).thenReturn(config);

        assertTrue(service.checkQuota(1L, "chat", 2));
    }

    @Test
    void checkQuota_underLimit_returnsTrue() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(5);

        assertTrue(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_atLimit_returnsFalse() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(10);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_noConfig_returnsFalse() {
        when(quotaMapper.selectByTierLevel(0)).thenReturn(null);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void checkQuota_disabled_returnsFalse() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        config.setEnabled(0);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);

        assertFalse(service.checkQuota(1L, "chat", 0));
    }

    @Test
    void recordUsage_insertsLog() {
        service.recordUsage(1L, "chat", null, 100);

        verify(usageLogMapper).insert(argThat(log ->
            log.getUserId().equals(1L) &&
            log.getUsageType().equals("chat") &&
            log.getTokensUsed().equals(100)
        ));
    }

    @Test
    void getTodayUsage_returnsCorrectInfo() {
        YyAiQuotaConfig config = new YyAiQuotaConfig();
        config.setDailyChatLimit(10);
        config.setDailyToolLimit(5);
        when(quotaMapper.selectByTierLevel(0)).thenReturn(config);
        when(usageLogMapper.countTodayByUserAndType(1L, "chat")).thenReturn(3);
        when(usageLogMapper.countTodayByUserAndType(1L, "tool")).thenReturn(1);

        var info = service.getTodayUsage(1L, 0);

        assertEquals(3, info.getChatUsed());
        assertEquals(10, info.getChatLimit());
        assertEquals(1, info.getToolUsed());
        assertEquals(5, info.getToolLimit());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiUsageServiceTest -q 2>&1 | tail -5`
Expected: FAIL (class not found)

- [ ] **Step 3: Write YyAiUsageService**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiQuotaConfig;
import com.ruoyi.yy.domain.YyAiUsageLog;
import com.ruoyi.yy.mapper.YyAiQuotaConfigMapper;
import com.ruoyi.yy.mapper.YyAiUsageLogMapper;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class YyAiUsageService {

    private final YyAiQuotaConfigMapper quotaMapper;
    private final YyAiUsageLogMapper usageLogMapper;

    public YyAiUsageService(YyAiQuotaConfigMapper quotaMapper, YyAiUsageLogMapper usageLogMapper) {
        this.quotaMapper = quotaMapper;
        this.usageLogMapper = usageLogMapper;
    }

    public boolean checkQuota(Long userId, String usageType, int tierLevel) {
        YyAiQuotaConfig config = quotaMapper.selectByTierLevel(tierLevel);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0) {
            return false;
        }

        int limit = "chat".equals(usageType) ? config.getDailyChatLimit() : config.getDailyToolLimit();
        if (limit == -1) {
            return true;
        }

        int used = usageLogMapper.countTodayByUserAndType(userId, usageType);
        return used < limit;
    }

    public void recordUsage(Long userId, String usageType, String toolName, int tokens) {
        YyAiUsageLog log = new YyAiUsageLog();
        log.setUserId(userId);
        log.setUsageType(usageType);
        log.setToolName(toolName);
        log.setTokensUsed(tokens);
        usageLogMapper.insert(log);
    }

    public Map<String, Object> getTodayUsage(Long userId, int tierLevel) {
        YyAiQuotaConfig config = quotaMapper.selectByTierLevel(tierLevel);
        int chatLimit = config != null ? config.getDailyChatLimit() : 0;
        int toolLimit = config != null ? config.getDailyToolLimit() : 0;
        int chatUsed = usageLogMapper.countTodayByUserAndType(userId, "chat");
        int toolUsed = usageLogMapper.countTodayByUserAndType(userId, "tool");

        Map<String, Object> info = new HashMap<>();
        info.put("chatUsed", chatUsed);
        info.put("chatLimit", chatLimit);
        info.put("toolUsed", toolUsed);
        info.put("toolLimit", toolLimit);
        return info;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiUsageServiceTest -q 2>&1 | tail -5`
Expected: Tests run: 7, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiUsageService.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/YyAiUsageServiceTest.java
git commit -m "feat: add YyAiUsageService for quota checking and usage recording"
```

---

## Task 3: YyAiIntentRouter 意图识别

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiIntentRouter.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiIntentRouterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiIntentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiIntentRouterTest {

    private IYyAiGateway gateway;
    private YyAiIntentRouter router;

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        router = new YyAiIntentRouter();
        ReflectionTestUtils.setField(router, "aiGateway", gateway);
    }

    @Test
    void route_advisorIntent_returnsAdvisor() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("ADVISOR", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("阿莫西林哪个平台便宜");
        assertEquals("ADVISOR", intent);
    }

    @Test
    void route_insightIntent_returnsInsight() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("INSIGHT", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("为什么这个药品价格差异这么大");
        assertEquals("INSIGHT", intent);
    }

    @Test
    void route_drugQaIntent_returnsDrugQa() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("DRUG_QA", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("阿莫西林的用法用量是什么");
        assertEquals("DRUG_QA", intent);
    }

    @Test
    void route_recommendIntent_returnsRecommend() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("RECOMMEND", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("推荐一些抗生素类药品");
        assertEquals("RECOMMEND", intent);
    }

    @Test
    void route_generalIntent_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("GENERAL", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("你好");
        assertEquals("GENERAL", intent);
    }

    @Test
    void route_gatewayFailure_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        String intent = router.route("测试消息");
        assertEquals("GENERAL", intent);
    }

    @Test
    void route_invalidIntent_returnsGeneral() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("UNKNOWN_INTENT", "qwen-turbo", 10, 5, 50)
        );

        String intent = router.route("测试消息");
        assertEquals("GENERAL", intent);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiIntentRouterTest -q 2>&1 | tail -5`
Expected: FAIL (class not found)

- [ ] **Step 3: Write YyAiIntentRouter**

```java
package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class YyAiIntentRouter {

    private static final Logger log = LoggerFactory.getLogger(YyAiIntentRouter.class);
    private static final Set<String> VALID_INTENTS = Set.of(
        "ADVISOR", "INSIGHT", "DRUG_QA", "RECOMMEND", "GENERAL"
    );

    private static final String SYSTEM_PROMPT =
        "你是意图分类器。根据用户消息，返回以下意图之一：\n" +
        "- ADVISOR: 采购建议、比价、哪个平台便宜、推荐采购\n" +
        "- INSIGHT: 价格分析、为什么价格不同、价格趋势\n" +
        "- DRUG_QA: 药品信息、用法用量、适应症、副作用、相互作用\n" +
        "- RECOMMEND: 推荐药品、同类药品、关联商品\n" +
        "- GENERAL: 其他所有问题\n\n" +
        "只返回意图代码，不要其他内容。";

    @Autowired
    private IYyAiGateway aiGateway;

    public String route(String userMessage) {
        YyAiRequest request = new YyAiRequest();
        request.setScene("intent_router");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userMessage);
        request.setModel("qwen-turbo");
        request.setTemperature(0.0);
        request.setMaxTokens(50);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Intent routing failed: {}", response.getErrorMessage());
            return "GENERAL";
        }

        String intent = response.getContent().trim().toUpperCase();
        if (!VALID_INTENTS.contains(intent)) {
            log.warn("Invalid intent from LLM: {}", intent);
            return "GENERAL";
        }

        return intent;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiIntentRouterTest -q 2>&1 | tail -5`
Expected: Tests run: 7, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiIntentRouter.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/YyAiIntentRouterTest.java
git commit -m "feat: add YyAiIntentRouter for LLM-based intent classification"
```

---

## Task 4: YyAiDrugQaImpl 药品问答

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiDrugQaImpl.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiDrugQaImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiDrugQaImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiDrugQaImplTest {

    private IYyAiGateway gateway;
    private YyAiDrugQaImpl drugQa;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        drugQa = new YyAiDrugQaImpl();
        ReflectionTestUtils.setField(drugQa, "aiGateway", gateway);
    }

    @Test
    void ask_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "answer", "阿莫西林适用于敏感菌引起的感染",
            "sources", List.of("中国药典"),
            "warnings", List.of("青霉素过敏者禁用")
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = drugQa.ask("阿莫西林的适应症是什么", "阿莫西林胶囊");

        assertNotNull(result);
        assertEquals("阿莫西林适用于敏感菌引起的感染", result.get("answer"));
        assertNotNull(result.get("sources"));
        assertNotNull(result.get("warnings"));
    }

    @Test
    void ask_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = drugQa.ask("测试问题", "测试药品");

        assertNotNull(result);
        assertNotNull(result.get("answer"));
        assertTrue(((String) result.get("answer")).contains("暂时无法"));
    }

    @Test
    void ask_invalidJson_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("not json", "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = drugQa.ask("测试问题", "测试药品");

        assertNotNull(result);
        assertNotNull(result.get("answer"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiDrugQaImplTest -q 2>&1 | tail -5`
Expected: FAIL (class not found)

- [ ] **Step 3: Write YyAiDrugQaImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class YyAiDrugQaImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiDrugQaImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药知识专家。根据用户问题提供准确的药品信息。\n" +
        "注意：\n" +
        "1. 必须基于可靠来源回答\n" +
        "2. 涉到处方药时提醒用户咨询医生\n" +
        "3. 不提供诊断建议\n" +
        "4. 返回JSON: {\"answer\": \"...\", \"sources\": [\"...\"], \"warnings\": [\"...\"]}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> ask(String question, String drugName) {
        String userPrompt = "药品: " + drugName + "\n问题: " + question;

        YyAiRequest request = new YyAiRequest();
        request.setScene("drug_qa");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Drug QA failed: {}", response.getErrorMessage());
            return fallbackResult();
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("answer", root.path("answer").asText("暂无答案"));
            result.put("sources", parseArray(root, "sources"));
            result.put("warnings", parseArray(root, "warnings"));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse drug QA response", e);
            return fallbackResult();
        }
    }

    private List<String> parseArray(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        JsonNode node = root.path(field);
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private Map<String, Object> fallbackResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("answer", "暂时无法回答该问题，请稍后再试或咨询专业药师。");
        result.put("sources", Collections.emptyList());
        result.put("warnings", Collections.emptyList());
        return result;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiDrugQaImplTest -q 2>&1 | tail -5`
Expected: Tests run: 3, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiDrugQaImpl.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/YyAiDrugQaImplTest.java
git commit -m "feat: add YyAiDrugQaImpl for drug Q&A service"
```

---

## Task 5: YyAiInsightImpl 比价解读

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiInsightImpl.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiInsightImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiInsightImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiInsightImplTest {

    private IYyAiGateway gateway;
    private YyAiInsightImpl insight;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        insight = new YyAiInsightImpl();
        ReflectionTestUtils.setField(insight, "aiGateway", gateway);
    }

    @Test
    void analyze_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "summary", "该药品在不同平台价差约20%",
            "insights", List.of("药京价格最低", "平台B含运费"),
            "recommendation", "推荐在药京采购"
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = insight.analyze("阿莫西林", "[{\"platform\":\"药京\",\"price\":12.5}]");

        assertNotNull(result);
        assertEquals("该药品在不同平台价差约20%", result.get("summary"));
        assertNotNull(result.get("insights"));
        assertNotNull(result.get("recommendation"));
    }

    @Test
    void analyze_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = insight.analyze("测试药品", "[]");

        assertNotNull(result);
        assertTrue(((String) result.get("summary")).contains("暂时无法"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiInsightImplTest -q 2>&1 | tail -5`
Expected: FAIL (class not found)

- [ ] **Step 3: Write YyAiInsightImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class YyAiInsightImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiInsightImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购数据分析专家。分析以下多平台价格数据，解读价格差异原因。\n" +
        "返回JSON: {\"summary\": \"...\", \"insights\": [\"...\"], \"recommendation\": \"...\"}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> analyze(String drugName, String pricesJson) {
        String userPrompt = "药品: " + drugName + "\n多平台价格数据:\n" + pricesJson;

        YyAiRequest request = new YyAiRequest();
        request.setScene("insight");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.3);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Insight analysis failed: {}", response.getErrorMessage());
            return fallbackResult();
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("summary", root.path("summary").asText("暂无分析"));
            result.put("insights", parseArray(root, "insights"));
            result.put("recommendation", root.path("recommendation").asText(""));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse insight response", e);
            return fallbackResult();
        }
    }

    private List<String> parseArray(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        JsonNode node = root.path(field);
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private Map<String, Object> fallbackResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", "暂时无法分析价格数据，请稍后再试。");
        result.put("insights", Collections.emptyList());
        result.put("recommendation", "");
        return result;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiInsightImplTest -q 2>&1 | tail -5`
Expected: Tests run: 2, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiInsightImpl.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/YyAiInsightImplTest.java
git commit -m "feat: add YyAiInsightImpl for price comparison insight service"
```

---

## Task 6: YyAiRecommendImpl 智能推荐

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiRecommendImpl.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/yy/YyAiRecommendImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ruoyi.yy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiRecommendImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiRecommendImplTest {

    private IYyAiGateway gateway;
    private YyAiRecommendImpl recommend;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        recommend = new YyAiRecommendImpl();
        ReflectionTestUtils.setField(recommend, "aiGateway", gateway);
    }

    @Test
    void recommend_validResponse_parsesResult() throws Exception {
        String aiContent = json.writeValueAsString(Map.of(
            "recommendations", List.of(
                Map.of("drugName", "头孢克洛", "reason", "同类抗生素", "estimatedPrice", 15.0)
            ),
            "basedOn", "基于阿莫西林采购历史"
        ));

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiContent, "qwen-turbo", 100, 50, 200)
        );

        Map<String, Object> result = recommend.recommend("抗生素", 5);

        assertNotNull(result);
        assertNotNull(result.get("recommendations"));
        assertNotNull(result.get("basedOn"));
    }

    @Test
    void recommend_gatewayFailure_returnsFallback() {
        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.fail("timeout")
        );

        Map<String, Object> result = recommend.recommend("抗生素", 5);

        assertNotNull(result);
        assertNotNull(result.get("recommendations"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiRecommendImplTest -q 2>&1 | tail -5`
Expected: FAIL (class not found)

- [ ] **Step 3: Write YyAiRecommendImpl**

```java
package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class YyAiRecommendImpl {

    private static final Logger log = LoggerFactory.getLogger(YyAiRecommendImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是医药采购推荐专家。根据用户采购历史和当前药品，推荐关联药品。\n" +
        "返回JSON: {\"recommendations\": [{\"drugName\": \"...\", \"reason\": \"...\", \"estimatedPrice\": ...}], \"basedOn\": \"...\"}";

    @Autowired
    private IYyAiGateway aiGateway;

    public Map<String, Object> recommend(String category, int limit) {
        String userPrompt = "药品类别: " + category + "\n推荐数量: " + limit;

        YyAiRequest request = new YyAiRequest();
        request.setScene("recommend");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel("qwen-turbo");
        request.setTemperature(0.5);
        request.setMaxTokens(800);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("Recommend failed: {}", response.getErrorMessage());
            return fallbackResult();
        }

        try {
            JsonNode root = JSON.readTree(response.getContent());
            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", parseRecommendations(root));
            result.put("basedOn", root.path("basedOn").asText("基于采购历史"));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse recommend response", e);
            return fallbackResult();
        }
    }

    private List<Map<String, Object>> parseRecommendations(JsonNode root) {
        List<Map<String, Object>> list = new ArrayList<>();
        JsonNode node = root.path("recommendations");
        if (node.isArray()) {
            for (JsonNode item : node) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("drugName", item.path("drugName").asText(""));
                rec.put("reason", item.path("reason").asText(""));
                rec.put("estimatedPrice", item.path("estimatedPrice").asDouble(0));
                list.add(rec);
            }
        }
        return list;
    }

    private Map<String, Object> fallbackResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("recommendations", Collections.emptyList());
        result.put("basedOn", "暂时无法生成推荐，请稍后再试。");
        return result;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ruoyi-system && mvn test -pl . -Dtest=YyAiRecommendImplTest -q 2>&1 | tail -5`
Expected: Tests run: 2, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/service/impl/YyAiRecommendImpl.java \
  ruoyi-system/src/test/java/com/ruoyi/yy/YyAiRecommendImplTest.java
git commit -m "feat: add YyAiRecommendImpl for smart recommendation service"
```

---

## Task 7: YyAiController 统一 API + SSE

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/yy/controller/YyAiController.java`

- [ ] **Step 1: Write YyAiController**

```java
package com.ruoyi.yy.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.service.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/yy/ai")
public class YyAiController extends BaseController {

    @Autowired private YyAiUsageService usageService;
    @Autowired private YyAiIntentRouter intentRouter;
    @Autowired private YyAiDrugQaImpl drugQa;
    @Autowired private YyAiInsightImpl insight;
    @Autowired private YyAiRecommendImpl recommend;
    @Autowired private IYyAiGateway aiGateway;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    private final ObjectMapper json = new ObjectMapper();

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();

        if (!usageService.checkQuota(userId, "chat", tierLevel)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"type\":\"quota_exceeded\",\"message\":\"今日对话次数已用完，升级会员解锁更多\"}"));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        String message = (String) body.getOrDefault("message", "");
        String intent = intentRouter.route(message);

        SseEmitter emitter = new SseEmitter(60000L);
        sseExecutor.submit(() -> {
            try {
                String systemPrompt = buildSystemPrompt(intent);
                int maxTokens = getMaxTokens(tierLevel);

                YyAiRequest request = new YyAiRequest();
                request.setScene("chat_" + intent.toLowerCase());
                request.setSystemPrompt(systemPrompt);
                request.setUserPrompt(message);
                request.setModel("qwen-turbo");
                request.setTemperature(0.7);
                request.setMaxTokens(maxTokens);

                YyAiResponse response = aiGateway.call(request);

                if (response.isSuccess()) {
                    String content = response.getContent();
                    // Simulate streaming by sending chunks
                    int chunkSize = 10;
                    for (int i = 0; i < content.length(); i += chunkSize) {
                        String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data("{\"type\":\"token\",\"data\":\"" + escapeJson(chunk) + "\",\"intent\":\"" + intent + "\"}"));
                    }
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data("{\"type\":\"done\",\"data\":\"\"}"));
                    usageService.recordUsage(userId, "chat", null, response.getTotalTokens());
                } else {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"type\":\"ai_error\",\"message\":\"AI 响应失败，请重试\"}"));
                }
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {}
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    @PostMapping("/advisor")
    public AjaxResult advisor(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return error("今日快捷功能次数已用完，升级会员解锁更多");
        }
        // Delegate to existing YyAiAdvisorImpl
        String drugName = (String) body.getOrDefault("drugName", "");
        usageService.recordUsage(userId, "tool", "advisor", 0);
        return success("采购顾问功能已调用");
    }

    @PostMapping("/insight")
    public AjaxResult insightApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return error("今日快捷功能次数已用完，升级会员解锁更多");
        }
        String drugName = (String) body.getOrDefault("drugName", "");
        String prices = body.getOrDefault("prices", "[]").toString();
        Map<String, Object> result = insight.analyze(drugName, prices);
        usageService.recordUsage(userId, "tool", "insight", 0);
        return success(result);
    }

    @PostMapping("/drug-qa")
    public AjaxResult drugQa(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return error("今日快捷功能次数已用完，升级会员解锁更多");
        }
        String question = (String) body.getOrDefault("question", "");
        String drugName = (String) body.getOrDefault("drugName", "");
        Map<String, Object> result = drugQa.ask(question, drugName);
        usageService.recordUsage(userId, "tool", "drug_qa", 0);
        return success(result);
    }

    @PostMapping("/recommend")
    public AjaxResult recommendApi(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();
        if (!usageService.checkQuota(userId, "tool", tierLevel)) {
            return error("今日快捷功能次数已用完，升级会员解锁更多");
        }
        String category = (String) body.getOrDefault("category", "");
        int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 5;
        Map<String, Object> result = recommend.recommend(category, limit);
        usageService.recordUsage(userId, "tool", "recommend", 0);
        return success(result);
    }

    @GetMapping("/usage")
    public AjaxResult usage() {
        Long userId = getUserId();
        int tierLevel = getUserTierLevel();
        return success(usageService.getTodayUsage(userId, tierLevel));
    }

    private String buildSystemPrompt(String intent) {
        return switch (intent) {
            case "ADVISOR" -> "你是医药采购顾问，帮助用户选择最优采购平台和方案。";
            case "INSIGHT" -> "你是医药采购数据分析专家，分析价格差异原因。";
            case "DRUG_QA" -> "你是医药知识专家，提供准确的药品信息。涉到处方药时提醒咨询医生。";
            case "RECOMMEND" -> "你是医药采购推荐专家，根据采购历史推荐关联药品。";
            default -> "你是医药采购AI助手，帮助用户解决采购相关问题。";
        };
    }

    private int getMaxTokens(int tierLevel) {
        return switch (tierLevel) {
            case 3 -> 1600;
            case 2 -> 1200;
            default -> 800;
        };
    }

    private int getUserTierLevel() {
        // TODO: 从用户信息中获取会员等级，暂返回0
        return 0;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd ruoyi-system && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/yy/controller/YyAiController.java
git commit -m "feat: add YyAiController with SSE streaming and quota checks"
```

---

## Task 8: 管理端配额配置页面 (ruoyi-ui)

**Files:**
- Create: `ruoyi-ui/src/api/yy/aiQuota.js`
- Create: `ruoyi-ui/src/views/yy/aiQuota/index.vue`
- Modify: `ruoyi-ui/src/router/index.js` — 添加路由

- [ ] **Step 1: Create API module**

```javascript
// ruoyi-ui/src/api/yy/aiQuota.js
import request from '@/utils/request'

export function listAiQuota() {
  return request({ url: '/yy/ai-quota/list', method: 'get' })
}

export function getAiQuota(id) {
  return request({ url: '/yy/ai-quota/' + id, method: 'get' })
}

export function updateAiQuota(data) {
  return request({ url: '/yy/ai-quota', method: 'put', data })
}
```

- [ ] **Step 2: Create quota config page**

```vue
<!-- ruoyi-ui/src/views/yy/aiQuota/index.vue -->
<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="quotaList" border>
      <el-table-column label="会员等级" prop="tierLevel" width="120">
        <template slot-scope="scope">
          {{ tierMap[scope.row.tierLevel] }}
        </template>
      </el-table-column>
      <el-table-column label="每日对话限额" width="150">
        <template slot-scope="scope">
          <el-input-number
            v-model="scope.row.dailyChatLimit"
            :min="-1"
            :max="9999"
            size="small"
            @change="handleChange(scope.row)"
          />
          <span v-if="scope.row.dailyChatLimit === -1" style="color: #67c23a; margin-left: 5px;">无限</span>
        </template>
      </el-table-column>
      <el-table-column label="每日快捷功能限额" width="170">
        <template slot-scope="scope">
          <el-input-number
            v-model="scope.row.dailyToolLimit"
            :min="-1"
            :max="9999"
            size="small"
            @change="handleChange(scope.row)"
          />
          <span v-if="scope.row.dailyToolLimit === -1" style="color: #67c23a; margin-left: 5px;">无限</span>
        </template>
      </el-table-column>
      <el-table-column label="单次请求最大Token" width="170">
        <template slot-scope="scope">
          <el-input-number
            v-model="scope.row.maxTokensPerReq"
            :min="100"
            :max="4000"
            :step="100"
            size="small"
            @change="handleChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="100">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.enabled"
            :active-value="1"
            :inactive-value="0"
            @change="handleChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" @click="handleSave(scope.row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 10px; color: #909399; font-size: 12px;">
      -1 表示不限制（无限次使用）
    </div>
  </div>
</template>

<script>
import { listAiQuota, updateAiQuota } from '@/api/yy/aiQuota'

export default {
  name: 'AiQuota',
  data() {
    return {
      loading: false,
      quotaList: [],
      tierMap: { 0: '普通用户', 1: '黄金会员', 2: '铂金会员', 3: '钻石会员' }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const res = await listAiQuota()
        this.quotaList = res.data || []
      } finally {
        this.loading = false
      }
    },
    handleChange(row) {
      // Mark as changed (visual feedback)
    },
    async handleSave(row) {
      await updateAiQuota(row)
      this.$message.success('保存成功')
      this.getList()
    }
  }
}
</script>
```

- [ ] **Step 3: Add route (modify router/index.js)**

在 `ruoyi-ui/src/router/index.js` 的 yy 路由 children 中添加：

```javascript
{
  path: 'aiQuota',
  component: () => import('@/views/yy/aiQuota/index'),
  name: 'AiQuota',
  meta: { title: 'AI配额配置', icon: 'config' }
}
```

- [ ] **Step 4: Verify frontend builds**

Run: `cd ruoyi-ui && npm run build 2>&1 | tail -5`
Expected: Build successful

- [ ] **Step 5: Commit**

```bash
git add ruoyi-ui/src/api/yy/aiQuota.js \
  ruoyi-ui/src/views/yy/aiQuota/index.vue \
  ruoyi-ui/src/router/index.js
git commit -m "feat: add admin AI quota configuration page"
```


---

## Task 9: 前端 useAiChat hook + api/ai.ts

**Files:**
- Create: `helpbuy-clone/src/api/ai.ts`
- Create: `helpbuy-clone/src/hooks/useAiChat.ts`
- Modify: `helpbuy-clone/src/api/types.ts` — 新增 AI 类型

- [ ] **Step 1: Add AI types to types.ts**

在 `helpbuy-clone/src/api/types.ts` 末尾追加：

```typescript
// AI 相关类型
export interface AiChatReq {
  message: string;
  sessionId?: string;
  context?: Record<string, unknown>;
}

export interface AiChatEvent {
  type: 'token' | 'action' | 'done' | 'error' | 'quota_exceeded';
  data: string;
  intent?: string;
}

export interface AdvisorReq {
  drugName: string;
  prices?: unknown[];
}

export interface InsightReq {
  drugName: string;
  prices?: unknown[];
}

export interface DrugQaReq {
  question: string;
  drugName?: string;
}

export interface RecommendReq {
  category: string;
  limit?: number;
}

export interface AiUsageInfo {
  chatUsed: number;
  chatLimit: number;
  toolUsed: number;
  toolLimit: number;
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  intent?: string;
  timestamp: number;
}
```

- [ ] **Step 2: Create api/ai.ts**

```typescript
// helpbuy-clone/src/api/ai.ts
import { getToken } from '@/utils/auth';
import request from '@/utils/request';
import type {
  AiChatReq, AiChatEvent, AiUsageInfo,
  InsightReq, DrugQaReq, RecommendReq,
  ApiResponse
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function* apiChat(req: AiChatReq): AsyncGenerator<AiChatEvent> {
  const token = getToken();
  const response = await fetch(`${BASE_URL}/yy/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'Accept': 'text/event-stream',
    },
    body: JSON.stringify(req),
  });

  if (!response.ok) {
    if (response.status === 429) {
      yield { type: 'quota_exceeded', data: '今日次数已用完' };
      return;
    }
    yield { type: 'error', data: `HTTP ${response.status}` };
    return;
  }

  const reader = response.body?.getReader();
  if (!reader) return;

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const jsonStr = line.slice(5).trim();
        if (jsonStr) {
          try {
            yield JSON.parse(jsonStr) as AiChatEvent;
          } catch { /* skip malformed */ }
        }
      }
    }
  }
}

export const apiAiInsight = (req: InsightReq): Promise<ApiResponse<Record<string, unknown>>> =>
  request.post('/yy/ai/insight', req);

export const apiAiDrugQa = (req: DrugQaReq): Promise<ApiResponse<Record<string, unknown>>> =>
  request.post('/yy/ai/drug-qa', req);

export const apiAiRecommend = (req: RecommendReq): Promise<ApiResponse<Record<string, unknown>>> =>
  request.post('/yy/ai/recommend', req);

export const apiGetAiUsage = (): Promise<ApiResponse<AiUsageInfo>> =>
  request.get('/yy/ai/usage');
```

- [ ] **Step 3: Create useAiChat hook**

```typescript
// helpbuy-clone/src/hooks/useAiChat.ts
import { useState, useCallback, useRef } from 'react';
import { apiChat, apiGetAiUsage, apiAiInsight, apiAiDrugQa, apiAiRecommend } from '@/api/ai';
import type { Message, AiUsageInfo } from '@/api/types';

interface UseAiChatReturn {
  messages: Message[];
  isLoading: boolean;
  usageInfo: AiUsageInfo | null;
  sendMessage: (text: string, context?: Record<string, unknown>) => Promise<void>;
  callTool: (tool: string, params: Record<string, unknown>) => Promise<void>;
  clearMessages: () => void;
  fetchUsage: () => Promise<void>;
}

export function useAiChat(): UseAiChatReturn {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [usageInfo, setUsageInfo] = useState<AiUsageInfo | null>(null);
  const sessionIdRef = useRef(crypto.randomUUID());

  const addMessage = useCallback((msg: Message) => {
    setMessages(prev => [...prev, msg]);
  }, []);

  const sendMessage = useCallback(async (text: string, context?: Record<string, unknown>) => {
    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: text,
      timestamp: Date.now(),
    };
    addMessage(userMsg);
    setIsLoading(true);

    const assistantMsg: Message = {
      id: crypto.randomUUID(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    };

    try {
      for await (const event of apiChat({
        message: text,
        sessionId: sessionIdRef.current,
        context,
      })) {
        if (event.type === 'token') {
          assistantMsg.content += event.data;
          assistantMsg.intent = event.intent;
          setMessages(prev => {
            const filtered = prev.filter(m => m.id !== assistantMsg.id);
            return [...filtered, { ...assistantMsg }];
          });
        } else if (event.type === 'quota_exceeded') {
          assistantMsg.content = '今日对话次数已用完，升级会员解锁更多。';
          setMessages(prev => [...prev.filter(m => m.id !== assistantMsg.id), { ...assistantMsg }]);
        } else if (event.type === 'error') {
          assistantMsg.content = 'AI 响应失败，请重试。';
          setMessages(prev => [...prev.filter(m => m.id !== assistantMsg.id), { ...assistantMsg }]);
        }
      }

      if (!assistantMsg.content) {
        assistantMsg.content = '未收到 AI 响应，请重试。';
        setMessages(prev => [...prev.filter(m => m.id !== assistantMsg.id), { ...assistantMsg }]);
      }
    } catch {
      assistantMsg.content = '网络错误，请检查连接后重试。';
      setMessages(prev => [...prev.filter(m => m.id !== assistantMsg.id), { ...assistantMsg }]);
    } finally {
      setIsLoading(false);
      fetchUsage();
    }
  }, [addMessage]);

  const callTool = useCallback(async (tool: string, params: Record<string, unknown>) => {
    setIsLoading(true);
    try {
      let result: Record<string, unknown>;
      switch (tool) {
        case 'insight':
          result = (await apiAiInsight(params as { drugName: string; prices?: unknown[] })).data;
          break;
        case 'drug_qa':
          result = (await apiAiDrugQa(params as { question: string; drugName?: string })).data;
          break;
        case 'recommend':
          result = (await apiAiRecommend(params as { category: string; limit?: number })).data;
          break;
        default:
          return;
      }
      const toolMsg: Message = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: JSON.stringify(result, null, 2),
        intent: tool,
        timestamp: Date.now(),
      };
      addMessage(toolMsg);
    } catch {
      const errMsg: Message = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: '功能调用失败，请重试。',
        timestamp: Date.now(),
      };
      addMessage(errMsg);
    } finally {
      setIsLoading(false);
      fetchUsage();
    }
  }, [addMessage]);

  const clearMessages = useCallback(() => {
    setMessages([]);
    sessionIdRef.current = crypto.randomUUID();
  }, []);

  const fetchUsage = useCallback(async () => {
    try {
      const res = await apiGetAiUsage();
      setUsageInfo(res.data);
    } catch { /* ignore */ }
  }, []);

  return { messages, isLoading, usageInfo, sendMessage, callTool, clearMessages, fetchUsage };
}
```

- [ ] **Step 4: Verify TypeScript compiles**

Run: `cd helpbuy-clone && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors (or only pre-existing errors)

- [ ] **Step 5: Commit**

```bash
git add helpbuy-clone/src/api/types.ts \
  helpbuy-clone/src/api/ai.ts \
  helpbuy-clone/src/hooks/useAiChat.ts
git commit -m "feat: add AI API layer and useAiChat hook for C-end"
```

---

## Task 10: 前端 AIAssistant 重写 + 子组件

**Files:**
- Rewrite: `helpbuy-clone/src/components/AIAssistant.tsx`
- Create: `helpbuy-clone/src/components/ai/ChatMessage.tsx`
- Create: `helpbuy-clone/src/components/ai/QuickActions.tsx`
- Create: `helpbuy-clone/src/components/ai/ChatInput.tsx`

- [ ] **Step 1: Create ChatMessage component**

```tsx
// helpbuy-clone/src/components/ai/ChatMessage.tsx
import type { Message } from '@/api/types';

interface Props {
  message: Message;
}

export default function ChatMessage({ message }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3`}>
      <div
        className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm leading-relaxed ${
          isUser
            ? 'bg-blue-500 text-white rounded-br-sm'
            : 'bg-gray-100 text-gray-800 rounded-bl-sm'
        }`}
      >
        {message.intent && message.intent !== 'GENERAL' && (
          <div className="text-xs opacity-60 mb-1">
            {message.intent === 'ADVISOR' && '采购顾问'}
            {message.intent === 'INSIGHT' && '比价解读'}
            {message.intent === 'DRUG_QA' && '药品问答'}
            {message.intent === 'RECOMMEND' && '智能推荐'}
          </div>
        )}
        <div className="whitespace-pre-wrap">{message.content}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Create QuickActions component**

```tsx
// helpbuy-clone/src/components/ai/QuickActions.tsx
interface Props {
  onAction: (action: string) => void;
  disabled?: boolean;
}

const actions = [
  { key: 'advisor', label: '采购顾问', icon: '🏷️' },
  { key: 'insight', label: '比价解读', icon: '📊' },
  { key: 'drug_qa', label: '药品问答', icon: '💊' },
  { key: 'recommend', label: '智能推荐', icon: '⭐' },
];

export default function QuickActions({ onAction, disabled }: Props) {
  return (
    <div className="grid grid-cols-2 gap-2 p-3">
      {actions.map(a => (
        <button
          key={a.key}
          onClick={() => onAction(a.key)}
          disabled={disabled}
          className="flex items-center gap-2 p-3 rounded-xl border border-gray-200
                     hover:bg-blue-50 hover:border-blue-300 transition-colors
                     disabled:opacity-50 disabled:cursor-not-allowed text-sm"
        >
          <span className="text-lg">{a.icon}</span>
          <span className="text-gray-700">{a.label}</span>
        </button>
      ))}
    </div>
  );
}
```

- [ ] **Step 3: Create ChatInput component**

```tsx
// helpbuy-clone/src/components/ai/ChatInput.tsx
import { useState, type KeyboardEvent } from 'react';

interface Props {
  onSend: (text: string) => void;
  disabled?: boolean;
}

export default function ChatInput({ onSend, disabled }: Props) {
  const [text, setText] = useState('');

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="flex items-end gap-2 p-3 border-t border-gray-200 bg-white">
      <textarea
        value={text}
        onChange={e => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="输入消息..."
        disabled={disabled}
        rows={1}
        className="flex-1 resize-none rounded-xl border border-gray-300 px-3 py-2 text-sm
                   focus:outline-none focus:border-blue-400 disabled:bg-gray-50
                   max-h-32"
      />
      <button
        onClick={handleSend}
        disabled={disabled || !text.trim()}
        className="shrink-0 w-9 h-9 rounded-full bg-blue-500 text-white flex items-center justify-center
                   hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        ➤
      </button>
    </div>
  );
}
```

- [ ] **Step 4: Rewrite AIAssistant.tsx**

```tsx
// helpbuy-clone/src/components/AIAssistant.tsx
import { useState, useEffect, useRef } from 'react';
import { useAiChat } from '@/hooks/useAiChat';
import ChatMessage from './ai/ChatMessage';
import QuickActions from './ai/QuickActions';
import ChatInput from './ai/ChatInput';

export default function AIAssistant() {
  const [open, setOpen] = useState(false);
  const { messages, isLoading, usageInfo, sendMessage, callTool, clearMessages, fetchUsage } = useAiChat();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (open) fetchUsage();
  }, [open, fetchUsage]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleQuickAction = (action: string) => {
    switch (action) {
      case 'advisor':
        sendMessage('请给我采购建议');
        break;
      case 'insight':
        callTool('insight', { drugName: '', prices: [] });
        break;
      case 'drug_qa':
        sendMessage('请介绍一下常用药品');
        break;
      case 'recommend':
        callTool('recommend', { category: '常用药品', limit: 5 });
        break;
    }
  };

  return (
    <>
      {/* Floating button */}
      <button
        onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-blue-500 text-white
                   shadow-lg hover:bg-blue-600 transition-colors z-50 flex items-center justify-center text-xl"
      >
        {open ? '✕' : '🤖'}
      </button>

      {/* Chat panel */}
      {open && (
        <div className="fixed bottom-24 right-6 w-96 max-h-[600px] bg-white rounded-2xl shadow-2xl
                        border border-gray-200 flex flex-col z-50 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-blue-500 text-white">
            <span className="font-medium">AI 采购专家</span>
            {usageInfo && (
              <span className="text-xs opacity-80">
                {usageInfo.chatLimit === -1 ? '无限' : `剩余 ${Math.max(0, usageInfo.chatLimit - usageInfo.chatUsed)} 次`}
              </span>
            )}
          </div>

          {/* Messages or Quick Actions */}
          <div className="flex-1 overflow-y-auto min-h-0">
            {messages.length === 0 ? (
              <QuickActions onAction={handleQuickAction} disabled={isLoading} />
            ) : (
              <div className="p-3">
                {messages.map(msg => (
                  <ChatMessage key={msg.id} message={msg} />
                ))}
                {isLoading && (
                  <div className="flex justify-start mb-3">
                    <div className="bg-gray-100 rounded-2xl rounded-bl-sm px-4 py-2 text-sm text-gray-500">
                      AI 思考中...
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input */}
          <ChatInput onSend={sendMessage} disabled={isLoading} />

          {/* Footer */}
          {messages.length > 0 && (
            <div className="px-3 py-1 border-t border-gray-100 flex justify-between">
              <button
                onClick={clearMessages}
                className="text-xs text-gray-400 hover:text-gray-600"
              >
                清空对话
              </button>
            </div>
          )}
        </div>
      )}
    </>
  );
}
```

- [ ] **Step 5: Verify TypeScript compiles**

Run: `cd helpbuy-clone && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add helpbuy-clone/src/components/AIAssistant.tsx \
  helpbuy-clone/src/components/ai/ChatMessage.tsx \
  helpbuy-clone/src/components/ai/QuickActions.tsx \
  helpbuy-clone/src/components/ai/ChatInput.tsx
git commit -m "feat: rewrite AIAssistant with native chat UI, quick actions, and SSE streaming"
```

---

## Task 11: 集成到 App.tsx + 类型定义完善

**Files:**
- Modify: `helpbuy-clone/src/App.tsx` — 确认 AIAssistant 已全局渲染

- [ ] **Step 1: Verify AIAssistant is rendered in App.tsx**

确认 `helpbuy-clone/src/App.tsx` 中已有：
```tsx
import AIAssistant from '@/components/AIAssistant';
// ...
{user && <AIAssistant />}
```

如果缺少，添加到 RequireAuth 内部。

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd helpbuy-clone && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add helpbuy-clone/src/App.tsx
git commit -m "feat: integrate AIAssistant into App.tsx global layout"
```

---

## Task 12: 端到端测试 + 修复

- [ ] **Step 1: Run all backend tests**

Run: `cd ruoyi-system && mvn test -pl . -Dtest="YyAiUsageServiceTest,YyAiIntentRouterTest,YyAiDrugQaImplTest,YyAiInsightImplTest,YyAiRecommendImplTest" -q 2>&1 | tail -10`
Expected: All tests pass

- [ ] **Step 2: Run full backend build**

Run: `cd ruoyi-system && mvn clean package -DskipTests -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run frontend build**

Run: `cd helpbuy-clone && npm run build 2>&1 | tail -10`
Expected: Build successful

- [ ] **Step 4: Manual E2E verification checklist**

启动后端和前端，手动验证：
- [ ] 打开 AI 助手浮标，显示快捷功能卡片
- [ ] 点击"采购顾问"卡片，发送消息
- [ ] 点击"药品问答"，输入问题
- [ ] 自由对话"阿莫西林哪个平台便宜"
- [ ] 配额显示正确
- [ ] 管理端 /yy/aiQuota 页面可编辑配额

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete AI value-added services implementation"
```
