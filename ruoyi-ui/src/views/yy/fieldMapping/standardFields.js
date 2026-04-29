/**
 * 标准商品字段配置 v2.0
 * 38个标准字段定义，供字段映射页面使用
 */

export const STANDARD_FIELDS = [
  // ========== 核心标识 ==========
  { value: 'product_id', label: '商品ID', type: 'string', group: '核心标识', desc: '平台内部统一的商品唯一标识符' },
  { value: 'sku_id', label: 'SKU ID', type: 'string', group: '核心标识', desc: '具体规格商品的唯一标识符' },
  { value: 'source_platform', label: '数据来源平台', type: 'string', group: '核心标识', desc: '系统底层必需：记录该条数据是从哪个源平台采集的' },
  
  // ========== 基础信息 ==========
  { value: 'barcode', label: '商品条码', type: 'string', group: '基础信息', desc: '商品的国际条形码（如 69 码）' },
  { value: 'product_name', label: '商品名称', type: 'string', group: '基础信息', desc: '商品的主标题、通用名称或展示名称' },
  { value: 'common_name', label: '通用名称', type: 'string', group: '基础信息', desc: '医药专属：药品的法定通用名称' },
  { value: 'brand_name', label: '品牌名称', type: 'string', group: '基础信息', desc: '商品所属的品牌名称' },
  { value: 'manufacturer', label: '生产厂家', type: 'string', group: '基础信息', desc: '商品的生产企业或上市许可持有人' },
  { value: 'approval_number', label: '批准文号', type: 'string', group: '基础信息', desc: '医药/特种商品专属：如国药准字号' },
  
  // ========== 分类 ==========
  { value: 'category_id', label: '分类ID', type: 'string', group: '分类', desc: '商品所属的平台分类ID' },
  { value: 'category_name', label: '分类名称', type: 'string', group: '分类', desc: '商品所属的分类或类目名称' },
  
  // ========== 规格 ==========
  { value: 'specification', label: '商品规格', type: 'string', group: '规格', desc: '商品的包装规格说明（如：0.25g10s2板）' },
  { value: 'unit', label: '销售单位', type: 'string', group: '规格', desc: '商品的最小销售计价单位（如：盒、瓶、支）' },
  { value: 'packing_ratio', label: '中包装/件装量', type: 'string', group: '规格', desc: 'B2B批发特有：商品的最小包装倍数' },
  
  // ========== 限购 ==========
  { value: 'min_order_qty', label: '最小起订量', type: 'number', group: '限购', desc: '商品的最小起订数量' },
  { value: 'max_order_qty', label: '最大订购量', type: 'number', group: '限购', desc: '商品的最大订购数量限制' },
  
  // ========== 状态与库存 ==========
  { value: 'product_status', label: '商品状态', type: 'string', group: '状态与库存', desc: '商品在源平台的实时状态（如：1上架 0下架 在售等）' },
  { value: 'stock_quantity', label: '总库存量', type: 'number', group: '状态与库存', desc: '商品当前可售的总可用库存数量' },
  { value: 'warehouse_stock', label: '分仓库存列表', type: 'json', group: '状态与库存', desc: 'B2B/多仓特有：各个仓库的库存分布详细信息' },
  { value: 'sales_volume', label: '销量', type: 'number', group: '状态与库存', desc: '商品的历史总销量或近期销量' },
  
  // ========== 图片 ==========
  { value: 'main_images', label: '商品主图', type: 'string', group: '图片', desc: '商品轮播图或主图的 URL（单个URL或逗号分隔多个URL）' },
  
  // ========== 日期 ==========
  { value: 'production_date', label: '生产日期', type: 'date', group: '日期', desc: '商品的生产日期（格式通常为 YYYY-MM-DD）' },
  { value: 'expiration_date', label: '有效期至', type: 'date', group: '日期', desc: '商品的失效日期' },
  { value: 'shelf_life', label: '保质期', type: 'string', group: '日期', desc: '商品的保质时长（如：36月）' },
  
  // ========== 医药专属 ==========
  { value: 'is_prescription_drug', label: '是否处方药', type: 'boolean', group: '医药专属', desc: '标识是否为处方药或受控药品' },
  { value: 'medicare_type', label: '医保类型', type: 'string', group: '医药专属', desc: '医保标签（如：国家医保甲类）' },
  { value: 'traceability_code_status', label: '追溯码状态', type: 'boolean', group: '医药专属', desc: '标识该商品是否带有追溯码' },
  
  // ========== 价格 ==========
  { value: 'price_retail', label: '建议零售价/标价', type: 'decimal', group: '价格', desc: '面向终端消费者的建议零售价' },
  { value: 'price_current', label: '当前基础供货价', type: 'decimal', group: '价格', desc: '单件商品的默认采购价格' },
  { value: 'price_step_rules', label: '阶梯价规则列表', type: 'json', group: '价格', desc: 'B2B核心价格机制。包含 min_qty, max_qty, step_price 等区间信息' },
  { value: 'price_assemble', label: '拼团/活动底价', type: 'decimal', group: '价格', desc: '参与特殊活动时能达到的最低价格' },
  
  // ========== 物流与税务 ==========
  { value: 'is_tax_included', label: '是否含税', type: 'boolean', group: '物流与税务', desc: 'B2B开票核心属性：价格是否包含增值税' },
  { value: 'freight_amount', label: '基础运费', type: 'decimal', group: '物流与税务', desc: '该商品/订单的基础物流费用' },
  { value: 'free_shipping_threshold', label: '免邮门槛', type: 'decimal', group: '物流与税务', desc: '达到包邮条件的最小金额或件数' },
  
  // ========== 供应商 ==========
  { value: 'shop_name', label: '店铺/供应商名称', type: 'string', group: '供应商', desc: '提供该商品的商家、药店或供应商名称' },
  
  // ========== 标签与活动 ==========
  { value: 'tags', label: '商品标签', type: 'json', group: '标签与活动', desc: '商品自带的各类型服务或属性标签（如：正品追溯、包邮、医保可用）' },
  { value: 'marketing_tags', label: '营销短标签', type: 'json', group: '标签与活动', desc: '用于前端高亮展示的简短促销词（如：20盒包邮、限购1000盒）' },
  { value: 'activity_details', label: '复杂活动明细列表', type: 'json', group: '标签与活动', desc: '结构化的活动规则对象数组（包含：activity_type, rule_desc, start_time, end_time）' },
  { value: 'purchase_limits', label: '限购与起批规则', type: 'json', group: '标签与活动', desc: '包含 min_purchase_qty(起批量), max_purchase_qty(最大购买量), step_qty(按多少件递增)' }
]

// 按分组组织
export const FIELD_GROUPS = {}
STANDARD_FIELDS.forEach(f => {
  if (!FIELD_GROUPS[f.group]) {
    FIELD_GROUPS[f.group] = []
  }
  FIELD_GROUPS[f.group].push(f)
})

// 字段值到标签的映射
export const FIELD_LABELS = {}
STANDARD_FIELDS.forEach(f => {
  FIELD_LABELS[f.value] = f.label
})

// 字段值到类型的映射
export const FIELD_TYPES = {}
STANDARD_FIELDS.forEach(f => {
  FIELD_TYPES[f.value] = f.type
})

// 供 el-select 使用的选项格式
export const FIELD_OPTIONS = STANDARD_FIELDS.map(f => ({
  value: f.value,
  label: `${f.label} (${f.value})`,
  type: f.type,
  group: f.group,
  desc: f.desc
}))

// 测试结果表格列配置
export const TEST_RESULT_COLUMNS = [
  { prop: 'commonName', label: '通用名', minWidth: 150 },
  { prop: 'productName', label: '商品名称', minWidth: 150 },
  { prop: 'specification', label: '规格', width: 120 },
  { prop: 'manufacturer', label: '厂家', minWidth: 120 },
  { prop: 'brandName', label: '品牌', width: 100 },
  { prop: 'approvalNumber', label: '批准文号', width: 140 },
  { prop: 'priceCurrent', label: '供货价', width: 80, align: 'right' },
  { prop: 'priceRetail', label: '零售价', width: 80, align: 'right' },
  { prop: 'stockQuantity', label: '库存', width: 70, align: 'right' },
  { prop: 'expirationDate', label: '有效期', width: 100 },
  { prop: 'shopName', label: '供应商', minWidth: 100 }
]
