/**
 * 比价系统字段配置
 */

// ========== 客户业态选项 ==========
export const CUSTOMER_TYPE_OPTIONS = [
  { label: '单体药店', value: 'single' },
  { label: '连锁药店', value: 'chain' },
  { label: '终端诊所', value: 'clinic' },
  { label: '批发客户', value: 'wholesale' }
]

// ========== 活动类型选项 ==========
export const ACTIVITY_TYPE_OPTIONS = [
  { label: '无活动', value: 'none' },
  { label: '拼团', value: '拼团' },
  { label: '凑单满减', value: '凑单' },
  { label: '会员价', value: '会员价' },
  { label: '限时折扣', value: '限时折扣' },
  { label: '专享价', value: '专享价' }
]

// ========== 搜索字段配置 ==========
export const SEARCH_FIELDS = [
  {
    key: 'skuId',
    label: 'SKU ID',
    type: 'input',
    placeholder: '请输入SKU ID'
  },
  {
    key: 'productName',
    label: '商品名称',
    type: 'input',
    placeholder: '请输入商品名称'
  },
  {
    key: 'commonName',
    label: '通用名',
    type: 'input',
    placeholder: '请输入通用名'
  },
  {
    key: 'customerType',
    label: '客户业态',
    type: 'select',
    placeholder: '全部',
    options: CUSTOMER_TYPE_OPTIONS
  },
  {
    key: 'platformCodes',
    label: '平台',
    type: 'select',
    placeholder: '全部',
    multiple: true,
    options: [] // 动态加载
  },
  {
    key: 'hasActivity',
    label: '有活动',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '是', value: true },
      { label: '否', value: false }
    ]
  }
]

// ========== 比价结果表格列配置 ==========
export const COMPARISON_TABLE_COLUMNS = [
  { prop: 'platformName', label: '平台', width: 100, fixed: true },
  { prop: 'currentPrice', label: '售价', width: 90, align: 'right', type: 'price' },
  { prop: 'activityType', label: '活动类型', width: 100, type: 'tag' },
  { prop: 'activityPrice', label: '活动价', width: 90, align: 'right', type: 'price' },
  { prop: 'minOrderQty', label: '起订量', width: 70, align: 'center' },
  { prop: 'freightAmount', label: '运费', width: 80, align: 'right', type: 'price' },
  { prop: 'deliveryDays', label: '配送天数', width: 80, align: 'center' },
  { 
    prop: 'totalCost', 
    label: '到手价', 
    width: 100, 
    align: 'right', 
    type: 'price',
    style: 'color: #f56c6c; font-weight: bold;'
  },
  { prop: 'savedAmount', label: '节省', width: 80, align: 'right', type: 'price' },
  { prop: 'isFreeShipping', label: '包邮', width: 60, align: 'center', type: 'boolean' },
  { prop: 'isBestPrice', label: '最优', width: 60, align: 'center', type: 'boolean' }
]

// ========== 采购方案表格列配置 ==========
export const PURCHASE_PLAN_COLUMNS = [
  { prop: 'productName', label: '商品名称', minWidth: 150 },
  { prop: 'platformName', label: '采购平台', width: 100 },
  { prop: 'unitPrice', label: '单价', width: 90, align: 'right', type: 'price' },
  { prop: 'quantity', label: '数量', width: 70, align: 'center' },
  { prop: 'subtotal', label: '小计', width: 100, align: 'right', type: 'price' },
  { prop: 'activityType', label: '活动', width: 100, type: 'tag' },
  { prop: 'freight', label: '运费', width: 80, align: 'right', type: 'price' },
  { prop: 'deliveryDays', label: '配送天数', width: 80, align: 'center' }
]

// ========== 价格趋势图表配置 ==========
export const TREND_CHART_OPTIONS = {
  title: {
    text: '价格趋势'
  },
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['基础价格', '活动价格', '到手价']
  },
  xAxis: {
    type: 'category',
    boundaryGap: false
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      formatter: '¥{value}'
    }
  },
  series: [
    {
      name: '基础价格',
      type: 'line',
      smooth: true,
      itemStyle: { color: '#909399' }
    },
    {
      name: '活动价格',
      type: 'line',
      smooth: true,
      itemStyle: { color: '#E6A23C' }
    },
    {
      name: '到手价',
      type: 'line',
      smooth: true,
      itemStyle: { color: '#F56C6C' }
    }
  ]
}

// ========== 平台颜色映射 ==========
export const PLATFORM_COLORS = {
  ysbang: '#1890ff',  // 药师帮
  ykd: '#52c41a',     // 药京多
  '1yc': '#722ed1',   // 1药城
  yaozen: '#fa8c16',  // 药 zen
  default: '#d9d9d9'
}
