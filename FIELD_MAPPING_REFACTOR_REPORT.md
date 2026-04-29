# 字段映射页面重构完成报告

## 重构时间
2026-04-14

## 问题
字段映射页面中的标准字段列表使用硬编码的旧字段名（17个），需要更新为新的38个标准字段。

## 修改文件

| 文件 | 说明 |
|------|------|
| `ruoyi-ui/src/views/yy/fieldMapping/standardFields.js` | 新建：标准字段配置文件 |
| `ruoyi-ui/src/views/yy/fieldMapping/index.vue` | 重构：使用配置驱动 |

## 配置结构 (standardFields.js)

```javascript
// 38个标准字段定义
STANDARD_FIELDS = [
  {
    value: 'field_key',      // 字段键名
    label: '字段中文名',     // 显示名称
    type: 'string|number|decimal|date|boolean|json',  // 数据类型
    group: '分组名',         // 所属分组
    desc: '字段说明'         // 描述
  }
]

// 按分组组织
FIELD_GROUPS = {
  '核心标识': [...],
  '基础信息': [...],
  '分类': [...],
  ...
}

// 供 el-select 使用的选项
FIELD_OPTIONS = [...]

// 测试结果表格列配置
TEST_RESULT_COLUMNS = [...]
```

## 字段分组

| 分组 | 字段数 | 字段 |
|------|--------|------|
| 核心标识 | 3 | product_id, sku_id, source_platform |
| 基础信息 | 6 | barcode, product_name, common_name, brand_name, manufacturer, approval_number |
| 分类 | 2 | category_id, category_name |
| 规格 | 3 | specification, unit, packing_ratio |
| 状态与库存 | 3 | product_status, stock_quantity, sales_volume |
| 日期 | 3 | production_date, expiration_date, shelf_life |
| 医药专属 | 3 | is_prescription_drug, medicare_type, traceability_code_status |
| 价格 | 3 | price_retail, price_current, price_assemble |
| 物流与税务 | 3 | is_tax_included, freight_amount, free_shipping_threshold |
| 供应商 | 1 | shop_name |

## 改进点

1. **配置驱动** - 标准字段定义从代码中抽离到配置文件
2. **分组显示** - 下拉选择器按分组组织，更易查找
3. **类型扩展** - 新增 boolean 和 json 类型支持
4. **字段说明** - 每个字段都有描述说明
5. **测试结果** - 表格列也使用配置驱动

## 后端同步

后端 `DataFusionServiceImpl.java` 已包含38个标准字段常量：
- F_PRODUCT_ID
- F_SKU_ID
- F_SOURCE_PLATFORM
- ... (共38个)

## 验证建议

1. 访问字段映射页面
2. 选择平台后点击"加载映射"
3. 点击"添加映射"，验证标准字段下拉是否显示38个字段
4. 验证字段按分组显示
5. 测试映射功能是否正常工作
