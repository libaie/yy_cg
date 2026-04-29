# YY 模块前端接口文档

> **基础路径**：`http://localhost:8080`（生产环境替换为实际域名）
> **认证方式**：除标注 `@Anonymous` 的接口外，其余接口均需在请求 Header 中携带 `Authorization: Bearer {token}`
> **通用返回格式**：`{ "code": 200, "msg": "操作成功", "data": { ... } }`
> **分页返回格式**：`{ "code": 200, "msg": "操作成功", "rows": [...], "total": 100 }`
> **生成时间**：2026-03-30 23:50

---

## 一、登录认证模块（无需 Token）

### 1. 发送短信验证码

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/captcha` |
| **权限** | 无需 Token（@Anonymous） |
| **描述** | 发送短信验证码到指定手机号（当前为模拟发送，验证码输出到后端日志） |

**请求参数（JSON Body）：**

```json
{
  "phone": "13800138000"
}
```

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "验证码已发送"
}
```

**响应示例（失败）：**

```json
{
  "code": 500,
  "msg": "手机号不能为空"
}
```

---

### 2. 用户注册

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/register` |
| **权限** | 无需 Token（@Anonymous） |
| **描述** | 使用手机号+密码+验证码注册，同时支持填写推荐人邀请码 |

**请求参数（JSON Body）：**

```json
{
  "phone": "13800138000",
  "password": "abc123",
  "code": "123456",
  "nickName": "张三",
  "referrerCode": "A1B2C3D4"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号（唯一） |
| password | String | 是 | 密码（BCrypt 加密存储） |
| code | String | 是 | 短信验证码 |
| nickName | String | 否 | 昵称 |
| referrerCode | String | 否 | 推荐人邀请码（8位大写字母+数字） |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "注册成功"
}
```

**可能的错误：**
- `"手机号和密码不能为空"`
- `"验证码已过期"` / `"验证码错误"`
- `"手机号已注册"`
- `"填写的推荐码无效"`
- `"邀请码生成失败，请重试"`

---

### 3. 用户登录

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/login` |
| **权限** | 无需 Token（@Anonymous） |
| **描述** | 支持密码登录和短信验证码登录两种方式 |

**请求参数（JSON Body）：**

```json
{
  "phone": "13800138000",
  "password": "abc123",
  "code": "123456",
  "loginType": "password"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号 |
| password | String | 密码登录时必填 | 用户密码 |
| code | String | 短信登录时必填 | 短信验证码 |
| loginType | String | 否 | "password"（默认）或 "sms" |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**可能的错误：**
- `"用户不存在"`
- `"用户密码不正确"`
- `"验证码已过期"` / `"验证码错误"`

---

## 二、用户模块（需 Token + `yy:client:access` 权限）

### 4. 获取个人资料

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/user/profile` |
| **权限** | `yy:client:access` |
| **描述** | 获取当前登录用户的个人信息及会员等级名称 |

**请求参数：** 无（用户ID从 Token 中自动获取）

**响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "user": {
      "userId": 1,
      "phone": "13800138000",
      "nickName": "张三",
      "avatar": "https://xxx.com/avatar.png",
      "memberLevel": 1,
      "memberLevelId": 5,
      "memberExpireTime": "2026-12-31",
      "firstMemberTime": "2026-01-15",
      "balance": 100.00,
      "chatbotUrl": "https://xxx.com/chatbot",
      "inviteCode": "A1B2C3D4",
      "referrerCode": null,
      "regTime": "2026-01-01",
      "createTime": "2026-01-01 10:00:00"
    },
    "memberLevelName": "黄金会员"
  }
}
```

> **注意**：`password` 字段在查询时已从 SQL 中排除，不会返回。

---

### 5. 绑定推荐人

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/user/bindReferrer` |
| **权限** | `yy:client:access` |
| **描述** | 绑定推荐人的邀请码，每个用户只能绑定一次 |

**请求参数（JSON Body）：**

```json
{
  "referrerCode": "A1B2C3D4"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| referrerCode | String | 是 | 推荐人的邀请码 |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "操作成功"
}
```

**可能的错误：**
- `"推荐码不能为空"`
- `"您已经绑定过推荐人，无法再次绑定"`
- `"不能绑定自己的推荐码"`
- `"无效的推荐码"`

---

### 6. 获取我的邀请码

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/user/myInviteCode` |
| **权限** | `yy:client:access` |
| **描述** | 获取当前用户的邀请码，老用户首次调用时自动生成 |

**请求参数：** 无

**响应示例：**

```json
{
  "code": 200,
  "msg": "获取成功",
  "data": "A1B2C3D4"
}
```

---

### 7. 会员预下单

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/user/preorder` |
| **权限** | `yy:client:access` |
| **描述** | 用户点击购买套餐时创建预付款订单。检查是否存在同类型支付中订单，有则提示，无则创建。订单支付超时30分钟，超时后自动取消 |

**请求参数（JSON Body）：**

```json
{
  "tierId": 5
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tierId | Long | 是 | 套餐ID |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "订单生成成功",
  "data": {
    "orderNo": "YY20260330231100a1b2c3d4",
    "payExpireTime": "2026-03-31 00:11:00"
  }
}
```

**可能的错误：**
- `"预下单失败：参数缺失 (tierId)"`
- `"参数格式错误：tierId"`
- `"会员套餐不存在"`
- `"该会员套餐暂未上架"`
- `"已存在该套餐的待支付订单，请先完成支付或等待超时后重试"`
- `"该套餐为限购套餐，您已达到购买上限（N次）"`

---

### 8. 支付宝异步回调

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/user/notify/alipay` |
| **权限** | 无需 Token（@Anonymous） |
| **描述** | 支付宝支付成功后的异步通知回调。当前验签逻辑已注释（未引入 Alipay SDK），仅处理业务逻辑 |

**请求参数（Form-Data）：**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| out_trade_no | String | 系统订单号（YY格式） |
| trade_no | String | 支付宝交易流水号 |
| trade_status | String | 交易状态，`TRADE_SUCCESS` 或 `TRADE_FINISHED` 视为成功 |

**响应示例：**
- 成功：`"success"`
- 失败：`"fail"`

**业务逻辑：** 当 `trade_status` 为 `TRADE_SUCCESS` 或 `TRADE_FINISHED` 时，调用 `upgradeUserMember` 完成会员升级：
- 更新订单状态为已支付（payStatus=1）
- 记录支付宝流水号（transactionId）
- 记录支付时间（payTime）
- 更新用户会员等级和到期时间

---

### 9. 微信支付异步回调

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/user/notify/wechat` |
| **权限** | 无需 Token（@Anonymous） |
| **描述** | 微信支付成功后的异步通知回调。当前为模拟实现（未引入 WeChat SDK） |

**请求参数：** 微信 V3 回调通过 HTTP Header 传输签名，Body 为加密的 JSON 数据流

**响应示例：**

```json
{
  "code": "SUCCESS",
  "message": "模拟回调成功"
}
```

---

## 三、会员套餐模块

### 10. 获取可用套餐列表

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/tier/listAll` |
| **权限** | `yy:client:access` |
| **描述** | 获取所有已上架的会员套餐列表，同时返回等级字典数据，方便前端渲染 |

**请求参数：** 无

**响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "data": [
      {
        "tierId": 1,
        "tierName": "黄金会员",
        "memberLevel": 1,
        "cardTitle": "黄金季卡",
        "cardType": "seasonal",
        "durationDays": 90,
        "price": 99.00,
        "privileges": "专属客服,无限聊天",
        "cardTag": "热销",
        "limitCount": 3,
        "isActive": 1,
        "createTime": "2026-03-01 10:00:00"
      }
    ],
    "tierDict": [
      { "dictLabel": "普通", "dictValue": "0" },
      { "dictLabel": "黄金", "dictValue": "1" },
      { "dictLabel": "铂金", "dictValue": "2" },
      { "dictLabel": "钻石", "dictValue": "3" }
    ]
  }
}
```

---

## 四、平台信息模块

### 11. 获取所有平台信息

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/platform/listAll` |
| **权限** | `yy:client:access` |
| **描述** | 获取所有平台信息列表（不分页） |

**请求参数：** 无

**响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "pId": 1,
      "platformName": "某某平台",
      "platformLogoUrl": "https://xxx.com/logo.png",
      "platformLoginUrl": "https://xxx.com/login",
      "createTime": "2026-03-01 10:00:00"
    }
  ]
}
```

---

## 五、订阅订单模块

### 12. 创建预付款订单

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/subscription/preorder` |
| **权限** | `yy:client:access` |
| **描述** | 与第7条功能相同，通过订阅模块创建预付款订单 |

**请求参数（JSON Body）：**

```json
{
  "tierId": 5
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tierId | Long | 是 | 套餐ID |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "orderNo": "YY20260330231100a1b2c3d4",
    "payExpireTime": "2026-03-31 00:11:00"
  }
}
```

**错误信息同第7条。**

---

### 13. 查询我的订阅订单列表

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/subscription/myList` |
| **权限** | `yy:client:access` |
| **描述** | 查询当前登录用户的订阅订单列表（分页），自动限定只查自己的数据 |

**请求参数（Query String）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| startTime | Date | 否 | 按会员开始时间筛选（yyyy-MM-dd） |
| endTime | Date | 否 | 按会员到期时间筛选（yyyy-MM-dd） |
| payStatus | Integer | 否 | 按支付状态筛选（0/1/2/3） |
| orderNo | String | 否 | 按订单号精确查询 |

**响应示例：**

```json
{
  "code": 200,
  "msg": "查询成功",
  "rows": [
    {
      "subId": 1,
      "userId": 10,
      "tierId": 5,
      "startTime": "2026-03-30",
      "endTime": "2026-06-28",
      "payStatus": 1,
      "orderNo": "YY20260330231100a1b2c3d4",
      "transactionId": "2026033022001412345678901234",
      "payExpireTime": "2026-03-31 00:11:00",
      "payTime": "2026-03-30 23:15:00",
      "createTime": "2026-03-30 23:11:00",
      "user": { "phone": "13800138000" },
      "tier": {
        "tierId": 5,
        "cardTitle": "黄金季卡",
        "tierName": "黄金会员",
        "memberLevel": 1,
        "durationDays": 90,
        "price": 99.00,
        "cardTag": "热销"
      }
    }
  ],
  "total": 5
}
```

---

### 14. 查询订阅订单详细

| 项目 | 说明 |
|------|------|
| **URL** | `GET /yy/subscription/{subId}` |
| **权限** | `yy:client:access` 或 `yy:subscription:query` |
| **描述** | 根据订阅记录ID获取订单详细信息。普通用户只能查看自己的订单 |

**请求参数（路径参数）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| subId | Long | 是 | 订阅记录ID |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "subId": 1,
    "userId": 10,
    "tierId": 5,
    "startTime": "2026-03-30",
    "endTime": "2026-06-28",
    "payStatus": 1,
    "orderNo": "YY20260330231100a1b2c3d4",
    "transactionId": "2026033022001412345678901234",
    "payExpireTime": "2026-03-31 00:11:00",
    "payTime": "2026-03-30 23:15:00",
    "createTime": "2026-03-30 23:11:00",
    "user": { "phone": "13800138000" },
    "tier": {
      "tierId": 5,
      "cardTitle": "黄金季卡",
      "tierName": "黄金会员",
      "memberLevel": 1,
      "durationDays": 90,
      "price": 99.00,
      "cardTag": "热销"
    }
  }
}
```

**可能的错误：**
- `"订单不存在"`
- `"无权查看该订单"`（普通用户查看他人订单时）

---

## 六、管理员手动升级（调试用）

### 15. 管理员手动触发会员升级

| 项目 | 说明 |
|------|------|
| **URL** | `POST /yy/user/upgrade` |
| **权限** | `yy:admin:upgrade` |
| **描述** | 管理员手动触发会员升级，模拟支付回调的业务逻辑。用于内部调试或异常订单处理 |

**请求参数（JSON Body）：**

```json
{
  "orderNo": "YY20260330231100a1b2c3d4",
  "transactionId": "MANUAL_20260330"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | 是 | 系统订单号 |
| transactionId | String | 否 | 第三方流水号（手动操作可填备注） |

**响应示例（成功）：**

```json
{
  "code": 200,
  "msg": "升级成功：会员等级已更新"
}
```

**响应示例（失败）：**

```json
{
  "code": 500,
  "msg": "升级失败：可能订单已失效、用户不存在或不支持降级购买"
}
```

**可能的错误：**
- `"升级失败：请求参数缺失 (orderNo)"`
- `"不支持降级购买，当前会员等级高于所选套餐"`

---

## 附录

### A. 支付状态枚举

| 值 | 含义 | 说明 |
|----|------|------|
| 0 | 未支付 | 订单超时后从支付中变为未支付 |
| 1 | 已支付 | 支付成功，会员权益已生效 |
| 2 | 已取消 | 手动取消或因限购被拦截 |
| 3 | 支付中 | 预付款订单创建后进入此状态，等待支付回调 |

### B. 会员等级枚举

| 值 | 名称 | 字典类型 |
|----|------|----------|
| 0 | 普通 | yy_member_tier_name |
| 1 | 黄金 | yy_member_tier_name |
| 2 | 铂金 | yy_member_tier_name |
| 3 | 钻石 | yy_member_tier_name |

### C. 订单号格式

```
YY + yyyyMMddHHmmss + 8位UUID
示例：YY20260330231100a1b2c3d4
总长度：2 + 14 + 8 = 24位
```

### D. 会员升级业务规则

| 场景 | 规则 |
|------|------|
| 首次购买 | 从当前时间开始计算有效期 |
| 过期后续费 | 从当前时间开始计算有效期 |
| 同等级续费 | 在原到期时间基础上累加天数 |
| 升级购买 | 从当前时间重新计算有效期 |
| 降级购买 | 不支持，抛出异常 |

### E. 短信验证码规则

| 项目 | 说明 |
|------|------|
| 验证码长度 | 6位纯数字 |
| 有效期 | 5分钟 |
| 缓存Key | yy_sms_code:{phone} |
| 存储 | Redis |

### F. 支付超时机制

| 项目 | 说明 |
|------|------|
| 超时时长 | 30分钟 |
| 定时任务 | 每分钟执行一次（cron: 0 */1 * * * ?） |
| 超时处理 | 将 payStatus=3 且 pay_expire_time 小于当前时间的订单更新为 payStatus=0 |

### G. 权限一览

| 权限标识 | 说明 | 角色 |
|----------|------|------|
| yy:client:access | 医药系统普通用户专属权限 | 普通用户 |
| yy:admin:upgrade | 管理员手动触发会员升级 | 管理员 |
| yy:subscription:list | 管理员查看所有订阅订单列表 | 管理员 |
| yy:subscription:query | 管理员查看订阅订单详情 | 管理员 |
| yy:subscription:edit | 管理员编辑订阅订单 | 管理员 |
| yy:subscription:remove | 管理员删除订阅订单 | 管理员 |
| yy:subscription:export | 管理员导出订阅订单 | 管理员 |

### H. 已知问题与改进建议

1. **preorder 接口重复** - YyUserController 和 YyMemberSubscriptionController 均存在 /preorder，前端 user.js 调 /yy/user/preorder，subscription.js 调 /yy/subscription/preorder，功能完全一致，建议后续统一收敛到订阅Controller
2. **支付宝回调签名验证已注释** - 生产环境上线前必须启用签名验证，否则存在伪造支付回调的安全风险
3. **微信回调为模拟实现** - 生产环境需接入真实的微信支付 V3 SDK
4. **SMS 验证码通过 System.out.println 输出** - 生产环境需替换为真实的短信服务商接口
