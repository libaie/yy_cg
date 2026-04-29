/**
 * 融合分组字段配置
 * 配置驱动，统一管理所有字段定义
 */

// ========== 搜索字段配置 ==========
export const FUSION_SEARCH_FIELDS = [
  {
    key: 'commonName',
    label: '通用名',
    placeholder: '请输入药品通用名'
  },
  {
    key: 'manufacturer',
    label: '生产厂家',
    placeholder: '请输入生产厂家'
  }
]

// ========== 融合分组表格列配置 ==========
export const FUSION_TABLE_COLUMNS = [
  { prop: 'commonName', label: '药品通用名', minWidth: 120 },
  { prop: 'specification', label: '规格', width: 120 },
  { prop: 'manufacturer', label: '生产厂家', minWidth: 150 },
  { prop: 'approvalNumber', label: '批准文号', width: 130 },
  { 
    prop: 'platformCount', 
    label: '平台数', 
    width: 80, 
    align: 'center',
    slot: true,
    type: 'count'
  },
  { 
    prop: 'minPrice', 
    label: '最低价', 
    width: 100, 
    align: 'right',
    slot: true,
    type: 'price'
  },
  { prop: 'totalStock', label: '总库存', width: 90, align: 'center' },
  { prop: 'lastUpdated', label: '更新时间', width: 160, align: 'center' }
]

// ========== 平台对比列配置 ==========
export const PLATFORM_COMPARE_COLUMNS = [
  { prop: 'sourcePlatform', label: '平台', width: 100 },
  { prop: 'skuId', label: 'SKU ID', width: 120 },
  { prop: 'productName', label: '商品名称', minWidth: 140 },
  { 
    prop: 'priceCurrent', 
    label: '供货价', 
    width: 90, 
    align: 'right',
    slot: true,
    type: 'price'
  },
  { prop: 'stockQuantity', label: '库存', width: 70, align: 'center' },
  { prop: 'unit', label: '单位', width: 60, align: 'center' },
  { prop: 'shopName', label: '供应商', minWidth: 120 },
  { prop: 'expirationDate', label: '有效期', width: 110 },
  { prop: 'syncedAt', label: '同步时间', width: 160 }
]
