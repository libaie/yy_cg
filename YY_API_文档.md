# YY 模块前端接口文档

> **基础路径**: `http://你的域名:端口`  
> **认证方式**: 登录后在请求 Header 中携带 `Authorization: Bearer {token}`  
> **公共响应格式**: `{ "code": 200, "msg": "操作成功", "data": ... }`

---

## 一、登录注册模块（`/yy`）— 无需 Token

### 1. 发送短信验证码

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/captcha` |
| **描述** | 发送短信验证码（目前为模拟，验证码会打印在服务端日志） |
| **认证** | ❌ 无需 Token |

**请求体 (JSON)**：
```json
{
  "phone": "13800138000"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | ✅ | 手机号 |

**响应**：
```json
{
  "code": 200,
  "msg": "验证码已发送"
}
```

---

### 2. 注册

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/register` |
| **描述** | 用户注册，需要先获取短信验证码 |
| **认证** | ❌ 无需 Token |

**请求体 (JSON)**：
```json
{
  "phone": "13800138000",
  "password": "123456",
  "code": "123456",
  "nickName": "用户昵称",
  "referrerCode": "A1B2C3D4"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | ✅ | 手机号（唯一） |
| password | String | ✅ | 密码 |
| code | String | ✅ | 短信验证码 |
| nickName | String | ❌ | 用户昵称 |
| referrerCode | String | ❌ | 推荐人的邀请码（8位） |

**响应**：
```json
{
  "code": 200,
  "msg": "注册成功"
}
```

---

### 3. 登录

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/login` |
| **描述** | 支持密码登录和验证码登录两种方式 |
| **认证** | ❌ 无需 Token |

**请求体 (JSON)**：
```json
{
  "phone": "13800138000",
  "password": "123456",
  "code": "123456",
  "loginType": "password"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | ✅ | 手机号 |
| password | String | 密码登录时✅ | 密码 |
| code | String | 验证码登录时✅ | 短信验证码 |
| loginType | String | ❌ | 登录类型：`password`（密码登录，默认）或 `sms`（验证码登录） |

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| token | String | JWT 令牌，后续请求需在 Header 中携带 |

---

## 二、用户模块（`/yy/user`）— 需 Token

### 4. 获取个人用户信息

| 项目 | 内容 |
|------|------|
| **接口** | `GET /yy/user/profile` |
| **描述** | 获取当前登录用户的个人信息及会员等级 |
| **认证** | ✅ 需要 Token |

**请求参数**：无

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "user": {
    "userId": 1,
    "phone": "13800138000",
    "nickName": "用户昵称",
    "avatar": "https://xxx.com/avatar.png",
    "memberLevel": 1,
    "memberLevelId": 1,
    "memberExpireTime": "2026-06-30",
    "firstMemberTime": "2026-03-30",
    "balance": "0.00",
    "chatbotUrl": "",
    "inviteCode": "A1B2C3D4",
    "referrerCode": "E5F6G7H8",
    "regTime": "2026-03-30"
  },
  "memberLevelName": "黄金会员"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| user | Object | 用户信息对象 |
| user.userId | Long | 用户ID |
| user.phone | String | 手机号 |
| user.nickName | String | 昵称 |
| user.avatar | String | 头像 URL |
| user.memberLevel | Long | 会员等级（0普通, 1黄金, 2铂金, 3钻石） |
| user.memberLevelId | Long | 当前生效的套餐ID |
| user.memberExpireTime | String | 会员到期时间（yyyy-MM-dd） |
| user.firstMemberTime | String | 首次开通会员时间 |
| user.balance | String | 余额 |
| user.chatbotUrl | String | 聊天机器人 URL |
| user.inviteCode | String | 自己的邀请码（8位） |
| user.referrerCode | String | 绑定的推荐人邀请码 |
| user.regTime | String | 注册时间 |
| memberLevelName | String | 会员等级名称（中文） |

---

### 5. 绑定推荐人

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/user/bindReferrer` |
| **描述** | 绑定推荐人的邀请码（只能绑定一次） |
| **认证** | ✅ 需要 Token |

**请求体 (JSON)**：
```json
{
  "referrerCode": "A1B2C3D4"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| referrerCode | String | ✅ | 推荐人的邀请码（8位） |

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 6. 获取我的邀请码

| 项目 | 内容 |
|------|------|
| **接口** | `GET /yy/user/myInviteCode` |
| **描述** | 获取或生成当前登录用户的邀请码 |
| **认证** | ✅ 需要 Token |

**请求参数**：无

**响应**：
```json
{
  "code": 200,
  "msg": "获取成功",
  "data": "A1B2C3D4"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | String | 8位邀请码 |

---

### 7. 创建预付款订单

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/user/preorder` |
| **描述** | 用户点击购买会员套餐时创建预付款订单。系统会检查是否存在同类型支付中订单，如有则返回提示 |
| **认证** | ✅ 需要 Token |

**请求体 (JSON)**：
```json
{
  "tierId": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tierId | Long | ✅ | 会员套餐ID |

**成功响应**：
```json
{
  "code": 200,
  "msg": "订单生成成功",
  "data": {
    "orderNo": "YY20260330212000A1B2C3D4",
    "payExpireTime": "2026-03-30 21:50:00"
  }
}
```

**失败响应（已有支付中订单）**：
```json
{
  "code": 500,
  "msg": "已存在该套餐的待支付订单，请先完成支付或等待超时后重试"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data.orderNo | String | 系统生成的订单号（格式：YY + yyyyMMddHHmmss + 8位UUID） |
| data.payExpireTime | String | 支付超时时间（当前时间 + 30分钟） |

---

## 三、会员套餐模块（`/yy/tier`）— 需 Token

### 8. 获取所有可用套餐列表

| 项目 | 内容 |
|------|------|
| **接口** | `GET /yy/tier/listAll` |
| **描述** | 获取所有已上架的会员套餐列表，附带等级字典数据 |
| **认证** | ✅ 需要 Token |

**请求参数**：无

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "tierId": 1,
      "tierName": "黄金会员",
      "memberLevel": 1,
      "cardTitle": "黄金年卡",
      "cardType": null,
      "durationDays": 365,
      "price": "299.00",
      "privileges": "专属客服,优先响应",
      "cardTag": "热门",
      "limitCount": 1,
      "isActive": 1,
      "createTime": "2026-03-30 10:00:00"
    }
  ],
  "tierDict": [
    {
      "dictValue": "0",
      "dictLabel": "普通会员"
    },
    {
      "dictValue": "1",
      "dictLabel": "黄金会员"
    },
    {
      "dictValue": "2",
      "dictLabel": "铂金会员"
    },
    {
      "dictValue": "3",
      "dictLabel": "钻石会员"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data[] | Array | 套餐列表 |
| data[].tierId | Long | 套餐ID |
| data[].tierName | String | 套餐名称 |
| data[].memberLevel | Integer | 对应等级（0普通, 1黄金, 2铂金, 3钻石） |
| data[].cardTitle | String | 卡片标题 |
| data[].cardType | String | 卡片类型 |
| data[].durationDays | Long | 有效期天数 |
| data[].price | String | 价格 |
| data[].privileges | String | 权益描述（逗号分隔） |
| data[].cardTag | String | 标签（如"热门""推荐"） |
| data[].limitCount | Long | 限购次数（0或null表示不限） |
| data[].isActive | Integer | 是否上架（1上架, 0下架） |
| tierDict[] | Array | 等级字典数据 |
| tierDict[].dictValue | String | 字典值 |
| tierDict[].dictLabel | String | 字典标签 |

---

## 四、订阅订单模块（`/yy/subscription`）— 需 Token

### 9. 创建预付款订单（备用入口）

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/subscription/preorder` |
| **描述** | 创建预付款订单（功能与 `/yy/user/preorder` 一致，备用入口） |
| **认证** | ✅ 需要 Token |

**请求体 (JSON)**：
```json
{
  "tierId": 1
}
```

**响应**：同接口 7

---

### 10. 查询我的订阅订单列表

| 项目 | 内容 |
|------|------|
| **接口** | `GET /yy/subscription/myList` |
| **描述** | 查询当前登录用户的订阅订单列表（分页） |
| **认证** | ✅ 需要 Token |

**请求参数 (Query)**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | ❌ | 页码（默认 1） |
| pageSize | Integer | ❌ | 每页条数（默认 10） |
| payStatus | Integer | ❌ | 按支付状态筛选（0未支付, 1已支付, 2已取消, 3支付中） |
| orderNo | String | ❌ | 按订单号搜索 |

**响应**：
```json
{
  "code": 200,
  "msg": "查询成功",
  "rows": [
    {
      "subId": 1,
      "userId": 1,
      "tierId": 1,
      "startTime": "2026-03-30",
      "endTime": "2027-03-30",
      "payStatus": 1,
      "orderNo": "YY20260330212000A1B2C3D4",
      "transactionId": "20260330220014100012000012345",
      "payExpireTime": "2026-03-30 21:50:00",
      "createTime": "2026-03-30 21:20:00",
      "user": {
        "phone": "13800138000"
      },
      "tier": {
        "tierId": 1,
        "cardTitle": "黄金年卡",
        "tierName": "黄金会员",
        "memberLevel": 1,
        "durationDays": 365,
        "price": "299.00",
        "cardTag": "热门"
      }
    }
  ],
  "total": 1
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Long | 总记录数 |
| rows[] | Array | 订单列表 |
| rows[].subId | Long | 订阅记录ID |
| rows[].userId | Long | 用户ID |
| rows[].tierId | Long | 套餐ID |
| rows[].startTime | String | 会员开始时间 |
| rows[].endTime | String | 会员到期时间 |
| rows[].payStatus | Integer | 支付状态：0未支付, 1已支付, 2已取消, 3支付中 |
| rows[].orderNo | String | 系统订单号 |
| rows[].transactionId | String | 第三方支付流水号 |
| rows[].payExpireTime | String | 支付超时时间 |
| rows[].createTime | String | 创建时间 |
| rows[].user.phone | String | 用户手机号 |
| rows[].tier.* | Object | 套餐信息（同接口8字段） |

---

## 五、平台模块（`/yy/platform`）— 需 Token

### 11. 获取所有平台信息

| 项目 | 内容 |
|------|------|
| **接口** | `GET /yy/platform/listAll` |
| **描述** | 获取所有平台信息列表（不分页） |
| **认证** | ✅ 需要 Token |

**请求参数**：无

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "pId": 1,
      "platformName": "平台名称",
      "platformUrl": "https://example.com",
      "platformDesc": "平台描述",
      "createTime": "2026-03-30 10:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data[] | Array | 平台列表 |
| data[].pId | Long | 平台ID |
| data[].platformName | String | 平台名称 |
| data[].platformUrl | String | 平台链接 |
| data[].platformDesc | String | 平台描述 |

---

## 六、支付回调模块（`/yy/user`）— 无需 Token

### 12. 支付宝异步回调

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/user/notify/alipay` |
| **描述** | 接收支付宝支付结果的异步通知，完成会员升级 |
| **认证** | ❌ 无需 Token（@Anonymous） |
| **Content-Type** | `application/x-www-form-urlencoded` |

**请求参数 (Form)**：

| 字段 | 类型 | 说明 |
|------|------|------|
| out_trade_no | String | 我们生成的订单号（YY...） |
| trade_no | String | 支付宝交易流水号 |
| trade_status | String | 交易状态：TRADE_SUCCESS 或 TRADE_FINISHED |
| ... | String | 支付宝其他标准回调参数 |

**响应**：
- 成功返回字符串：`success`
- 失败返回字符串：`fail`

---

### 13. 微信支付异步回调

| 项目 | 内容 |
|------|------|
| **接口** | `POST /yy/user/notify/wechat` |
| **描述** | 接收微信支付结果的异步通知，完成会员升级（当前为模拟状态） |
| **认证** | ❌ 无需 Token（@Anonymous） |

**请求体 (JSON)**：微信支付 V3 标准回调格式

**响应**：
```json
{
  "code": "SUCCESS",
  "message": "模拟回调成功"
}
```

---

## 附录

### 状态码说明

| payStatus | 含义 |
|-----------|------|
| 0 | 未支付（超时终态） |
| 1 | 已支付 |
| 2 | 已取消 |
| 3 | 支付中（待用户付款） |

| memberLevel | 含义 |
|-------------|------|
| 0 | 普通会员 |
| 1 | 黄金会员 |
| 2 | 铂金会员 |
| 3 | 钻石会员 |

### 定时任务

- **任务名称**：超时支付中订单自动取消
- **执行频率**：每分钟执行一次（`@Scheduled(cron = "0 */1 * * * ?")`）
- **逻辑**：扫描 `pay_status = 3` 且 `pay_expire_time < 当前时间` 的订单，批量更新为 `pay_status = 0`

### 错误响应示例

| 场景 | 返回 |
|------|------|
| 手机号已注册 | `{ "code": 500, "msg": "手机号已注册" }` |
| 验证码过期 | `{ "code": 500, "msg": "验证码已过期" }` |
| 验证码错误 | `{ "code": 500, "msg": "验证码错误" }` |
| 用户不存在 | `{ "code": 500, "msg": "用户不存在" }` |
| 密码不正确 | `{ "code": 500, "msg": "用户密码不正确" }` |
| 套餐不存在 | `{ "code": 500, "msg": "套餐不存在" }` |
| 限购已达上限 | `{ "code": 500, "msg": "该套餐为限购套餐，您已达到购买上限（N次）" }` |
| 已存在支付中订单 | `{ "code": 500, "msg": "已存在该套餐的待支付订单，请先完成支付或等待超时后重试" }` |
| 不支持降级购买 | `{ "code": 500, "msg": "不支持降级购买，当前会员等级高于所选套餐" }` |
| 已绑定过推荐人 | `{ "code": 500, "msg": "您已经绑定过推荐人，无法再次绑定" }` |
| 不能绑定自己 | `{ "code": 500, "msg": "不能绑定自己的推荐码" }` |
| 推荐码无效 | `{ "code": 500, "msg": "无效的推荐码" }` |
| 未登录/Token过期 | `{ "code": 401, "msg": "令牌已过期或无效" }` |
| 无权限 | `{ "code": 403, "msg": "没有访问权限" }` |
