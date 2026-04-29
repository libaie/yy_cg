import request from '@/utils/request'

// ========== 商品比价 ==========

// 同品多平台比价
export function comparePrices(skuId, customerType = 'single') {
  return request({
    url: `/yy/priceComparison/compare/${skuId}`,
    method: 'get',
    params: { customerType }
  })
}

// 批量商品比价
export function batchComparePrices(skuIds, customerType = 'single') {
  return request({
    url: '/yy/priceComparison/batchCompare',
    method: 'post',
    data: skuIds,
    params: { customerType }
  })
}

// 按条件比价
export function comparePricesByCondition(data) {
  return request({
    url: '/yy/priceComparison/compareByCondition',
    method: 'post',
    data
  })
}

// ========== 推荐与优化 ==========

// 最优平台推荐
export function recommendPlatform(skuId, customerType = 'single', regionCode) {
  return request({
    url: `/yy/priceComparison/recommend/${skuId}`,
    method: 'get',
    params: { customerType, regionCode }
  })
}

// 生成采购组合方案
export function generatePurchasePlan(skuIds, customerType = 'single') {
  return request({
    url: '/yy/priceComparison/purchasePlan',
    method: 'post',
    data: skuIds,
    params: { customerType }
  })
}

// ========== 数据查询 ==========

// 获取商品历史价格趋势
export function getPriceTrend(skuId, platformCode, days = 30) {
  return request({
    url: `/yy/priceComparison/trend/${skuId}`,
    method: 'get',
    params: { platformCode, days }
  })
}

// 获取平台活动列表
export function getPlatformActivities(platformCode, activeOnly = true) {
  return request({
    url: '/yy/priceComparison/activities',
    method: 'get',
    params: { platformCode, activeOnly }
  })
}

// 获取比价统计
export function getComparisonStats(customerType = 'single') {
  return request({
    url: '/yy/priceComparison/stats',
    method: 'get',
    params: { customerType }
  })
}

// ========== 数据管理 ==========

// 导入比价数据
export function importPriceComparisons(data) {
  return request({
    url: '/yy/priceComparison/import',
    method: 'post',
    data
  })
}

// 采集平台价格数据
export function collectPriceData(platformCode, skuIds) {
  return request({
    url: '/yy/priceComparison/collect',
    method: 'post',
    data: skuIds,
    params: { platformCode }
  })
}
