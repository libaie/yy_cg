# min_order_qty / max_order_qty 全链路同步完成报告

## 同步时间
2026-04-14

## 新增字段
| 字段名 | 类型 | 说明 |
|--------|------|------|
| min_order_qty | INT | 最小起订量 |
| max_order_qty | INT | 最大订购量 |

## 修改文件清单

### 1. 数据库
| 文件 | 修改内容 |
|------|----------|
| `sql/yy_standard_product_v2.sql` | 添加 min_order_qty, max_order_qty 字段定义 |

### 2. Java Domain
| 文件 | 修改内容 |
|------|----------|
| `YyStandardProduct.java` | 添加 minOrderQty, maxOrderQty 属性 |

### 3. Mapper XML
| 文件 | 修改内容 |
|------|----------|
| `YyStandardProductMapper.xml` | resultMap, SQL查询, insert, update, upsert 全部同步 |

### 4. Service
| 文件 | 修改内容 |
|------|----------|
| `DataFusionServiceImpl.java` | 添加常量, INTEGER_FIELDS, setStandardField 映射 |

### 5. 前端配置
| 文件 | 修改内容 |
|------|----------|
| `fieldMapping/standardFields.js` | 添加限购分组, 包含两个新字段 |

## 字段映射页面效果

标准字段下拉新增"限购"分组：
```
限购
├── 最小起订量 (min_order_qty)
└── 最大订购量 (max_order_qty)
```

## 数据流验证

```
平台数据 → 字段映射配置(min_order_qty/max_order_qty)
    ↓
DataFusionServiceImpl.setStandardField()
    ↓
YyStandardProduct.minOrderQty/maxOrderQty
    ↓
YyStandardProductMapper.upsertYyStandardProduct()
    ↓
yy_standard_product.min_order_qty/max_order_qty
```

## 测试建议

1. 在字段映射页面配置 min_order_qty 和 max_order_qty 映射
2. 执行数据融合测试
3. 验证数据库中字段值正确存储
4. 验证前端列表和详情页显示
