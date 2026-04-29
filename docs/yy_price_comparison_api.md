# 多平台比价系统 - API接口文档

## 基础信息
- **基础路径**: `/yy/priceComparison`
- **认证方式**: Bearer Token
- **响应格式**: `{ "code": 200, "msg": "操作成功", "data": ... }`

---

## 一、商品比价接口

### 1. 同品多平台比价

**接口**: `GET /yy/priceComparison/compare/{skuId}`

**描述**: 查询同一商品在各平台的价格对比

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuId | String | ✅ | SKU ID |
| customerType | String | ❌ | 客户业态：single/chain/clinic/wholesale，默认single |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "skuId": "SP001",
      "platformCode": "ysbang",
      "platformName": "药师帮",
      "basePrice": 100.00,
      "currentPrice": 95.00,
      "activityType": "拼团",
      "activityPrice": 85.00,
      "activityName": "春季药品拼团节",
      "minOrderQty": 1,
      "freightAmount": 5.00,
      "deliveryDays": 2,
      "totalCost": 90.00,
      "isFreeShipping": false,
      "savedAmount": 15.00,
      "savedPercent": 14.29,
      "isBestPrice": true,
      "rank": 1
    }
  ]
}
```

---

### 2. 批量商品比价

**接口**: `POST /yy/priceComparison/batchCompare`

**描述**: 批量查询多个商品的比价结果

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuIds | List<String> | ✅ | SKU ID列表 |
| customerType | String | ❌ | 客户业态 |

**请求体**:
```json
["SP001", "SP002", "SP003"]
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "SP001": [
      { "platformCode": "ysbang", "totalCost": 90.00 },
      { "platformCode": "ykd", "totalCost": 95.00 }
    ],
    "SP002": [
      { "platformCode": "ykd", "totalCost": 88.00 }
    ]
  }
}
```

---

### 3. 按条件比价

**接口**: `POST /yy/priceComparison/compareByCondition`

**描述**: 按复杂条件进行比价查询

**请求体**:
```json
{
  "skuId": "SP001",
  "productName": "感冒灵",
  "commonName": "感冒灵颗粒",
  "customerType": "single",
  "platformCodes": ["ysbang", "ykd"],
  "hasActivity": true,
  "minPrice": 50.00,
  "maxPrice": 200.00,
  "sortBy": "totalCost",
  "sortOrder": "asc",
  "pageNum": 1,
  "pageSize": 20
}
```

---

## 二、推荐与优化接口

### 4. 最优平台推荐

**接口**: `GET /yy/priceComparison/recommend/{skuId}`

**描述**: 获取商品的最优采购平台推荐

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuId | String | ✅ | SKU ID |
| customerType | String | ❌ | 客户业态 |
| regionCode | String | ❌ | 区域编码（影响运费） |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "skuId": "SP001",
    "productName": "感冒灵颗粒",
    "recommendedPlatform": "ysbang",
    "recommendedPlatformName": "药师帮",
    "recommendedPrice": 85.00,
    "totalCost": 90.00,
    "reason": "价格最低，参与拼团活动",
    "savedAmount": 15.00,
    "suggestedOrderQty": 3
  }
}
```

---

### 5. 生成采购组合方案

**接口**: `POST /yy/priceComparison/purchasePlan`

**描述**: 为多个商品生成最优采购组合方案

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuIds | List<String> | ✅ | 采购商品列表 |
| customerType | String | ❌ | 客户业态 |

**请求体**:
```json
["SP001", "SP002", "SP003"]
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "planId": "abc123",
    "items": [
      {
        "skuId": "SP001",
        "productName": "感冒灵颗粒",
        "platformCode": "ysbang",
        "platformName": "药师帮",
        "unitPrice": 85.00,
        "quantity": 1,
        "subtotal": 85.00,
        "activityType": "拼团",
        "freight": 5.00,
        "deliveryDays": 2
      }
    ],
    "totalProductAmount": 255.00,
    "totalFreightAmount": 15.00,
    "totalDiscountAmount": 50.00,
    "finalAmount": 220.00,
    "totalSavedAmount": 35.00,
    "savedPercent": 13.73,
    "optimizationTips": [
      "采购涉及2个平台，可尝试凑单减少运费"
    ]
  }
}
```

---

## 三、数据查询接口

### 6. 获取商品历史价格趋势

**接口**: `GET /yy/priceComparison/trend/{skuId}`

**描述**: 查询商品在指定平台的价格历史趋势

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuId | String | ✅ | SKU ID |
| platformCode | String | ✅ | 平台编码 |
| days | Int | ❌ | 查询天数，默认30 |

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "date": "2026-04-01",
      "platformCode": "ysbang",
      "basePrice": 100.00,
      "activityPrice": 85.00,
      "totalCost": 90.00,
      "activityType": "拼团"
    }
  ]
}
```

---

### 7. 获取平台活动列表

**接口**: `GET /yy/priceComparison/activities`

**描述**: 查询平台的活动配置

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platformCode | String | ❌ | 平台编码 |
| activeOnly | Boolean | ❌ | 是否只查有效活动，默认true |

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "platformCode": "ysbang",
      "platformName": "药师帮",
      "activityCode": "ys_2026_group_buy",
      "activityName": "春季药品拼团节",
      "activityType": "拼团",
      "activityDesc": "3人成团，享85折优惠",
      "startTime": "2026-04-01 00:00:00",
      "endTime": "2026-04-30 23:59:59",
      "isActive": 1,
      "isValid": true,
      "remainingDays": 9
    }
  ]
}
```

---

### 8. 获取比价统计

**接口**: `GET /yy/priceComparison/stats`

**描述**: 获取比价系统的统计信息

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerType | String | ❌ | 客户业态 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "customerType": "single",
    "totalProducts": 1000,
    "platformCount": 5,
    "avgSavedAmount": 12.50,
    "activityPercent": 35.5
  }
}
```

---

## 四、数据管理接口

### 9. 导入比价数据

**接口**: `POST /yy/priceComparison/import`

**描述**: 批量导入比价数据

**权限**: `yy:priceComparison:add`

**请求体**:
```json
[
  {
    "skuId": "SP001",
    "platformCode": "ysbang",
    "basePrice": 100.00,
    "currentPrice": 95.00,
    "activityType": "拼团",
    "activityPrice": 85.00,
    "freightAmount": 5.00,
    "deliveryDays": 2
  }
]
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "successCount": 10,
    "failCount": 0,
    "totalCount": 10
  }
}
```

---

### 10. 采集平台价格数据

**接口**: `POST /yy/priceComparison/collect`

**描述**: 触发平台价格数据采集

**权限**: `yy:priceComparison:add`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platformCode | String | ✅ | 平台编码 |
| skuIds | List<String> | ❌ | SKU ID列表（为空则采集全部） |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "platformCode": "ysbang",
    "collectedCount": 100,
    "successCount": 95,
    "failCount": 5
  }
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 500 | 服务器内部错误 |
| 1001 | 商品不存在 |
| 1002 | 平台不存在 |
| 1003 | 采集失败 |

---

## 注意事项

1. **价格计算规则**:
   - 到手价 = 活动价（或原价）+ 运费
   - 节省金额 = 最高价 - 当前到手价
   - 包邮判断：运费=0 或 到手价≥免邮门槛

2. **活动优先级**:
   - 拼团 > 限时折扣 > 会员价 > 凑单 > 专享价

3. **缓存策略**:
   - 比价结果缓存30分钟
   - 活动信息实时查询

4. **数据更新频率**:
   - B2B平台：每小时更新
   - C端平台：每12小时更新

---

**文档版本**: v1.0
**最后更新**: 2026-04-21
