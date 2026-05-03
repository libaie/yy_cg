# AI 增值服务设计文档

> 为医药 B2B 采购平台的 C 端（helpbuy-clone，React+TypeScript）实现自建 AI 增值服务，替换现有第三方 chatbot iframe。

## 1. 背景与目标

### 现状
- C 端 `AIAssistant.tsx` 是 iframe 嵌入第三方 chatbot（`profile.user.chatbotUrl`）
- 后端已有 `YyAiGateway`（DashScope/通义千问）、`YyAiAdvisorImpl`（采购顾问）、`YyAiDataCleanerImpl`（数据清洗）
- 会员体系四级：regular / gold / platinum / diamond

### 目标
1. 替换第三方 chatbot 为自建 AI（通义千问 qwen-turbo）
2. 实现 4 个 AI 增值功能：采购顾问、比价解读、药品问答、智能推荐
3. 会员分层限制：普通用户限次，付费会员无限（管理端可配置）
4. 混合交互：自由对话 + 快捷功能卡片

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────┐
│  C 端 (helpbuy-clone / React+TS)                │
│  AIAssistant.tsx → 原生聊天 UI                   │
│    ├── 快捷卡片 → POST /yy/ai/advisor 等         │
│    └── 自由对话 → POST /yy/ai/chat (SSE)         │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│  YyAiController (统一 API 入口)                   │
│    ├── 配额检查 → YyAiUsageService               │
│    ├── 流式对话 → YyAiIntentRouter → 对应服务     │
│    └── 快捷功能 → 直接调对应服务                   │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│  AI 服务层                                        │
│    ├── YyAiAdvisorImpl    (采购顾问，已有)        │
│    ├── YyAiInsightImpl    (比价解读，新建)        │
│    ├── YyAiDrugQaImpl     (药品问答，新建)        │
│    ├── YyAiRecommendImpl  (智能推荐，新建)        │
│    └── YyAiIntentRouter   (意图识别，新建)        │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│  YyAiGateway (已有)                               │
│    ├── 熔断器 YyCircuitBreaker                    │
│    ├── Redis 缓存 (TTL 24h)                       │
│    └── DashScope API (qwen-turbo)                 │
└─────────────────────────────────────────────────┘
```

### 2.2 技术栈
- **后端**: Spring Boot + MyBatis (RuoYi 框架)
- **前端**: React 19 + TypeScript + Tailwind CSS + Framer Motion
- **AI 模型**: 通义千问 qwen-turbo (DashScope API)
- **通信**: SSE (Server-Sent Events) 流式响应 + REST 快捷 API

## 3. 后端设计

### 3.1 新增文件清单

| 文件 | 职责 |
|------|------|
| `YyAiController.java` | 统一 API 入口，SSE 流式 + 快捷功能 |
| `YyAiIntentRouter.java` | 意图识别：从用户消息判断调用哪个 AI 服务 |
| `YyAiDrugQaImpl.java` | 药品问答服务 |
| `YyAiInsightImpl.java` | 比价解读服务 |
| `YyAiRecommendImpl.java` | 智能推荐服务 |
| `YyAiUsageService.java` | 用量统计 + 配额检查 |
| `YyAiQuotaConfig.java` | 配额配置实体 |
| `YyAiQuotaConfigMapper.java` | 配额配置 Mapper |
| `YyAiUsageLog.java` | 用量日志实体 |
| `YyAiUsageLogMapper.java` | 用量日志 Mapper |
| `V20260503__create_ai_quota_tables.sql` | Flyway 迁移脚本 |

### 3.2 配额配置表

```sql
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
```

默认数据：
```sql
INSERT INTO yy_ai_quota_config (tier_level, daily_chat_limit, daily_tool_limit, max_tokens_per_req, enabled) VALUES
(0, 10, 3, 800, 1),   -- regular
(1, 50, -1, 800, 1),  -- gold
(2, -1, -1, 1200, 1), -- platinum
(3, -1, -1, 1600, 1); -- diamond
```

### 3.3 用量日志表

```sql
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
```

### 3.4 YyAiController API 设计

#### 3.4.1 流式对话

```
POST /yy/ai/chat
Content-Type: application/json
Accept: text/event-stream

请求体:
{
  "message": "阿莫西林哪个平台最便宜？",
  "sessionId": "uuid-xxx",
  "context": {
    "drugName": "阿莫西林胶囊",
    "platformPrices": [...]
  }
}

SSE 响应:
data: {"type":"token","data":"推荐","intent":"advisor"}
data: {"type":"token","data":"在药京多平台采购"}
data: {"type":"action","data":"{\"label\":\"查看详情\",\"action\":\"view_advisor\"}"}
data: {"type":"done","data":""}
```

#### 3.4.2 快捷功能 API

```
POST /yy/ai/advisor
请求: { "drugName": "阿莫西林", "prices": [...] }
响应: ApiResponse<YyPurchaseAdvice>

POST /yy/ai/insight
请求: { "drugName": "阿莫西林", "prices": [...] }
响应: ApiResponse<{summary, insights[], recommendation}>

POST /yy/ai/drug-qa
请求: { "question": "阿莫西林的用法用量？", "drugName": "阿莫西林胶囊" }
响应: ApiResponse<{answer, sources[], warnings[]}>

POST /yy/ai/recommend
请求: { "category": "抗生素", "limit": 5 }
响应: ApiResponse<{recommendations[], basedOn}>

GET /yy/ai/usage
响应: ApiResponse<{chatUsed, chatLimit, toolUsed, toolLimit}>
```

### 3.5 YyAiIntentRouter 意图识别

使用 LLM 做轻量意图分类（qwen-turbo, max_tokens=50）：

```
System: 你是意图分类器。根据用户消息，返回以下意图之一：
- ADVISOR: 采购建议、比价、哪个平台便宜、推荐采购
- INSIGHT: 价格分析、为什么价格不同、价格趋势
- DRUG_QA: 药品信息、用法用量、适应症、副作用、相互作用
- RECOMMEND: 推荐药品、同类药品、关联商品
- GENERAL: 其他所有问题

只返回意图代码，不要其他内容。

User: {message}
```

识别结果路由到对应服务。GENERAL 直接调 LLM 自由回答。

### 3.6 AI 服务设计

#### YyAiDrugQaImpl（药品问答）

```
System: 你是医药知识专家。根据用户问题提供准确的药品信息。
注意：
1. 必须基于可靠来源回答
2. 涉到处方药时提醒用户咨询医生
3. 不提供诊断建议
4. 返回 JSON: {"answer": "...", "sources": ["..."], "warnings": ["..."]}
```

#### YyAiInsightImpl（比价解读）

```
System: 你是医药采购数据分析专家。分析以下多平台价格数据，解读价格差异原因。
返回 JSON: {"summary": "...", "insights": ["..."], "recommendation": "..."}
```

#### YyAiRecommendImpl（智能推荐）

```
System: 你是医药采购推荐专家。根据用户采购历史和当前药品，推荐关联药品。
返回 JSON: {"recommendations": [{"drugName": "...", "reason": "...", "estimatedPrice": ...}], "basedOn": "..."}
```

### 3.7 YyAiUsageService 配额检查

```java
@Service
public class YyAiUsageService {
    // 检查用户是否还有配额
    public boolean checkQuota(Long userId, String usageType);

    // 记录用量
    public void recordUsage(Long userId, String usageType, String toolName, int tokens);

    // 获取用户今日用量
    public AiUsageInfo getTodayUsage(Long userId);

    // 获取用户配额配置（根据会员等级）
    public YyAiQuotaConfig getQuotaConfig(int tierLevel);
}
```

查询逻辑：
```sql
SELECT COUNT(*) FROM yy_ai_usage_log
WHERE user_id = #{userId}
  AND usage_type = #{usageType}
  AND DATE(create_time) = CURDATE()
```

## 4. 前端设计

### 4.1 新增/修改文件清单

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/components/AIAssistant.tsx` | 重写 | 原生聊天 UI（替换 iframe） |
| `src/components/ai/ChatMessage.tsx` | 新建 | 消息气泡组件 |
| `src/components/ai/QuickActions.tsx` | 新建 | 快捷功能卡片 |
| `src/components/ai/ChatInput.tsx` | 新建 | 输入框组件 |
| `src/api/ai.ts` | 新建 | AI 相关 API 调用 |
| `src/hooks/useAiChat.ts` | 新建 | 聊天状态管理 hook |
| `src/api/types.ts` | 修改 | 新增 AI 相关类型 |

### 4.2 UI 布局

```
┌─────────────────────────────────┐
│  AI 采购专家        ⚡ 剩余 8次  │  ← Header（显示配额）
├─────────────────────────────────┤
│                                 │
│  ┌─ 快捷功能卡片 ──────────┐   │  ← 功能入口区
│  │ 🏷️ 采购顾问  📊 比价解读 │   │
│  │ 💊 药品问答  ⭐ 智能推荐 │   │
│  └──────────────────────────┘   │
│                                 │
│  ┌─ 对话消息流 ────────────┐   │  ← 聊天区
│  │ 用户: 阿莫西林哪个平台便宜│   │
│  │ AI: 推荐在XX平台采购...   │   │
│  │     [查看详情] [加入比价] │   │  ← 操作按钮
│  └──────────────────────────┘   │
│                                 │
├─────────────────────────────────┤
│  [输入消息...]        [发送 ➤]  │  ← 输入区
└─────────────────────────────────┘
```

### 4.3 API 调用层 (`src/api/ai.ts`)

```typescript
// 流式对话（SSE）
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

  const reader = response.body?.getReader();
  const decoder = new TextDecoder();
  // ... 解析 SSE 流，yield AiChatEvent
}

// 快捷功能
export const apiAiAdvisor = (req: AdvisorReq): Promise<ApiResponse<YyPurchaseAdvice>> =>
  request.post('/yy/ai/advisor', req);

export const apiAiInsight = (req: InsightReq): Promise<ApiResponse<InsightResult>> =>
  request.post('/yy/ai/insight', req);

export const apiAiDrugQa = (req: DrugQaReq): Promise<ApiResponse<DrugQaResult>> =>
  request.post('/yy/ai/drug-qa', req);

export const apiAiRecommend = (req: RecommendReq): Promise<ApiResponse<RecommendResult>> =>
  request.post('/yy/ai/recommend', req);

export const apiGetAiUsage = (): Promise<ApiResponse<AiUsageInfo>> =>
  request.get('/yy/ai/usage');
```

### 4.4 useAiChat Hook

```typescript
interface UseAiChatReturn {
  messages: Message[];
  isLoading: boolean;
  usageInfo: AiUsageInfo | null;
  sendMessage: (text: string, context?: AiContext) => Promise<void>;
  callTool: (tool: ToolType, params: any) => Promise<void>;
  clearMessages: () => void;
}

export function useAiChat(): UseAiChatReturn {
  // - 管理消息列表 state
  // - 处理 SSE 流式响应
  // - 调用 /yy/ai/usage 获取配额
  // - 错误处理（配额耗尽、超时等）
}
```

### 4.5 快捷功能卡片交互

| 卡片 | 点击行为 |
|------|----------|
| 采购顾问 | 弹出输入框输入药品名 → 调 `/yy/ai/advisor` |
| 比价解读 | 自动获取当前页面比价数据 → 调 `/yy/ai/insight` |
| 药品问答 | 弹出输入框输入问题 → 调 `/yy/ai/drug-qa` |
| 智能推荐 | 基于用户历史自动调 `/yy/ai/recommend` |

### 4.6 错误处理

| 场景 | 前端表现 |
|------|----------|
| 配额耗尽 (429) | 显示"今日次数已用完，升级会员解锁" + 跳转会员页 |
| AI 超时 | 显示"AI 响应超时，请重试" + 重试按钮 |
| SSE 断连 | 自动重连 1 次，失败后提示重试 |
| 未登录 | 不显示 AI 按钮 |

## 5. 会员配额管理

### 5.1 后端配额流程

```
请求进入 → YyAiController
  → YyAiUsageService.checkQuota(userId, usageType)
    → 查询 yy_ai_quota_config (按 tier_level)
    → 查询 yy_ai_usage_log (今日已用次数)
    → 对比 limit → 返回 true/false
  → 如果超限，返回 429 + 提示信息
  → 通过 → 执行 AI 服务 → recordUsage()
```

### 5.2 管理端配置页面

在 `ruoyi-ui` 新增配额配置页面：
- 路由: `/yy/ai-quota`
- 功能: 按会员等级编辑每日对话限额、快捷功能限额、token 上限
- 表单: 四行（对应四个等级），inline 编辑

## 6. 测试策略

| 层级 | 内容 |
|------|------|
| 后端单元测试 | 每个 AI 服务（mock gateway）、意图路由、配额服务 |
| 后端集成测试 | Controller 配额检查 + 服务调用链 |
| 前端组件测试 | ChatMessage、QuickActions 渲染 |
| 手动 E2E | 完整对话流程、快捷功能、配额限制、降级 |

## 7. 实施计划

| Task | 内容 | 文件数 |
|------|------|--------|
| 1 | 配额表 + Flyway 迁移 + 实体 + Mapper | 4 |
| 2 | YyAiUsageService 用量统计 + 配额检查 | 2 |
| 3 | YyAiIntentRouter 意图识别 | 1 |
| 4 | YyAiDrugQaImpl 药品问答 | 1 |
| 5 | YyAiInsightImpl 比价解读 | 1 |
| 6 | YyAiRecommendImpl 智能推荐 | 1 |
| 7 | YyAiController 统一 API + SSE | 1 |
| 8 | 管理端配额配置页面 (ruoyi-ui) | 3 |
| 9 | 前端 useAiChat hook + api/ai.ts | 3 |
| 10 | 前端 AIAssistant 重写 + 子组件 | 4 |
| 11 | 集成到 App.tsx + 类型定义 | 2 |
| 12 | 端到端测试 + 修复 | - |
