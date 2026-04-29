# 前端配置驱动重构完成报告

## 重构时间
2026-04-14

## 重构内容
将前端页面从硬编码改为配置驱动，统一管理字段定义

## 新增文件

| 文件 | 说明 |
|------|------|
| `ruoyi-ui/src/views/yy/standardProduct/config.js` | 标准商品字段配置 |
| `ruoyi-ui/src/views/yy/fusionGroup/config.js` | 融合分组字段配置 |

## 修改文件

| 文件 | 说明 |
|------|------|
| `ruoyi-ui/src/views/yy/standardProduct/index.vue` | 改为配置驱动 |
| `ruoyi-ui/src/views/yy/fusionGroup/index.vue` | 改为配置驱动 |

## 配置结构

### 1. 标准商品配置 (standardProduct/config.js)

```javascript
// 搜索字段
SEARCH_FIELDS = [
  { key, label, type, placeholder, options }
]

// 表格列
TABLE_COLUMNS = [
  { prop, label, width, minWidth, fixed, align, slot, type, style, options }
]

// 详情字段
DETAIL_FIELDS = [
  { key, label, group, type, options, trueLabel, falseLabel }
]

// 字段中文映射
FIELD_LABELS = { key: label }
```

### 2. 融合分组配置 (fusionGroup/config.js)

```javascript
// 搜索字段
FUSION_SEARCH_FIELDS = [
  { key, label, placeholder }
]

// 表格列
FUSION_TABLE_COLUMNS = [
  { prop, label, width, minWidth, align, slot, type }
]

// 平台对比列
PLATFORM_COMPARE_COLUMNS = [
  { prop, label, width, minWidth, align, slot, type }
]
```

## 支持的字段类型

| type | 说明 | 渲染方式 |
|------|------|----------|
| input | 输入框 | el-input |
| select | 下拉选择 | el-select |
| price | 价格 | ¥前缀 + 红色加粗 |
| status | 状态 | el-tag (支持多种状态) |
| boolean | 布尔值 | el-tag (是/否) |
| json | JSON数据 | 格式化显示 |
| count | 计数 | el-tag |

## 优势

1. **配置驱动** - 字段定义集中管理，修改一处即可全局生效
2. **类型统一** - 相同类型的字段渲染方式一致
3. **易于扩展** - 新增字段只需在配置中添加一行
4. **维护方便** - 字段定义与渲染逻辑分离
5. **代码复用** - 减少重复的模板代码

## 使用示例

### 新增表格列
```javascript
// 在 TABLE_COLUMNS 数组中添加
{ prop: 'newField', label: '新字段', width: 100 }
```

### 新增搜索字段
```javascript
// 在 SEARCH_FIELDS 数组中添加
{ key: 'newField', label: '新字段', type: 'input', placeholder: '请输入' }
```

### 新增详情字段
```javascript
// 在 DETAIL_FIELDS 数组中添加
{ key: 'newField', label: '新字段', group: '分组名' }
```

## 后续优化建议

1. 将配置抽取为独立的 npm 包，供多个项目复用
2. 支持从后端动态加载字段配置
3. 支持用户自定义表格列显示/隐藏
4. 支持用户自定义表格列顺序
