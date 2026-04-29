/**
 * 标准商品字段配置
 * 配置驱动，统一管理所有字段定义
 */

// ========== 搜索字段配置 ==========
export const SEARCH_FIELDS = [
  {
    key: 'sourcePlatform',
    label: '平台',
    type: 'select',
    placeholder: '全部',
    options: [] // 动态加载
  },
  {
    key: 'commonName',
    label: '通用名',
    type: 'input',
    placeholder: '请输入'
  },
  {
    key: 'productName',
    label: '商品名',
    type: 'input',
    placeholder: '请输入'
  },
  {
    key: 'approvalNumber',
    label: '批准文号',
    type: 'input',
    placeholder: '请输入'
  },
  {
    key: 'barcode',
    label: '条码',
    type: 'input',
    placeholder: '请输入'
  },
  {
    key: 'productStatus',
    label: '状态',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '上架', value: 1 },
      { label: '下架', value: 0 }
    ]
  }
]

// ========== 表格列配置 ==========
export const TABLE_COLUMNS = [
  { prop: 'sourcePlatform', label: '平台', width: 100, fixed: true },
  { prop: 'skuId', label: 'SKU ID', width: 130 },
  { prop: 'productName', label: '商品名称', minWidth: 140 },
  { prop: 'commonName', label: '通用名', minWidth: 140 },
  { prop: 'specification', label: '规格', width: 100 },
  { prop: 'manufacturer', label: '生产厂家', minWidth: 150 },
  { prop: 'approvalNumber', label: '批准文号', width: 140 },
  { 
    prop: 'priceCurrent', 
    label: '供货价', 
    width: 90, 
    align: 'right',
    slot: true,
    type: 'price',
    style: 'color: #f56c6c; font-weight: bold;'
  },
  { 
    prop: 'priceRetail', 
    label: '零售价', 
    width: 90, 
    align: 'right',
    slot: true,
    type: 'price'
  },
  { prop: 'stockQuantity', label: '库存', width: 70, align: 'center' },
  { prop: 'unit', label: '单位', width: 60, align: 'center' },
  {
    prop: 'productStatus',
    label: '状态',
    width: 70,
    align: 'center',
    slot: true,
    type: 'status',
    options: [
      { label: '上架', value: 1, tagType: 'success' },
      { label: '下架', value: 0, tagType: 'danger' }
    ]
  },
  { prop: 'expirationDate', label: '有效期', width: 100, align: 'center' },
  { prop: 'shopName', label: '供应商', width: 120 },
  { prop: 'medicareType', label: '医保类型', width: 100 },
  { prop: 'collectedAt', label: '采集时间', width: 160, align: 'center' }
]

// ========== 详情字段配置 ==========
export const DETAIL_FIELDS = [
  // 基础标识
  { key: 'productId', label: '商品ID', group: '基础信息' },
  { key: 'skuId', label: 'SKU ID' },
  { key: 'sourcePlatform', label: '平台' },
  { key: 'barcode', label: '条码' },
  
  // 商品信息
  { key: 'productName', label: '商品名称', group: '商品信息' },
  { key: 'commonName', label: '通用名' },
  { key: 'brandName', label: '品牌' },
  { key: 'specification', label: '规格' },
  { key: 'manufacturer', label: '生产厂家' },
  { key: 'approvalNumber', label: '批准文号' },
  { key: 'categoryName', label: '分类' },
  { key: 'unit', label: '单位' },
  { key: 'packingRatio', label: '中包装' },
  
  // 价格信息
  { key: 'priceCurrent', label: '供货价', type: 'price', group: '价格信息' },
  { key: 'priceRetail', label: '零售价', type: 'price' },
  { key: 'priceAssemble', label: '拼团价', type: 'price' },
  { key: 'isTaxIncluded', label: '含税', type: 'boolean' },
  { key: 'freightAmount', label: '运费', type: 'price' },
  { key: 'freeShippingThreshold', label: '免邮门槛', type: 'price' },
  
  // 库存销售
  { key: 'stockQuantity', label: '库存', group: '库存销售' },
  { key: 'salesVolume', label: '销量' },
  { key: 'shopName', label: '供应商' },
  {
    key: 'productStatus',
    label: '状态',
    type: 'status',
    options: [
      { label: '上架', value: 1, tagType: 'success' },
      { label: '下架', value: 0, tagType: 'danger' }
    ]
  },
  
  // 日期信息
  { key: 'productionDate', label: '生产日期', group: '日期信息' },
  { key: 'expirationDate', label: '有效期至' },
  { key: 'shelfLife', label: '保质期' },
  
  // 医药专属
  { key: 'isPrescriptionDrug', label: '处方药', type: 'boolean', trueLabel: '是', falseLabel: '否', group: '医药专属' },
  { key: 'medicareType', label: '医保类型' },
  { key: 'traceabilityCodeStatus', label: '追溯码', type: 'boolean', trueLabel: '有', falseLabel: '无' },
  
  // JSON数据
  { key: 'mainImages', label: '主图', type: 'json', group: '扩展数据' },
  { key: 'warehouseStock', label: '分仓库存', type: 'json' },
  { key: 'priceStepRules', label: '阶梯价', type: 'json' },
  { key: 'tags', label: '标签', type: 'json' },
  { key: 'marketingTags', label: '营销标签', type: 'json' },
  { key: 'activityDetails', label: '活动明细', type: 'json' },
  { key: 'purchaseLimits', label: '限购规则', type: 'json' },
  
  // 时间戳
  { key: 'collectedAt', label: '采集时间', group: '时间戳' },
  { key: 'syncedAt', label: '同步时间' }
]

// ========== 字段中文名映射 ==========
export const FIELD_LABELS = {}
DETAIL_FIELDS.forEach(f => {
  FIELD_LABELS[f.key] = f.label
})
