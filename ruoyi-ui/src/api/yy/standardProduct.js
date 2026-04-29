import request from '@/utils/request'

// 查询标准商品列表
export function listStandardProduct(query) {
  return request({
    url: '/yy/standardProduct/list',
    method: 'get',
    params: query
  })
}

// 获取标准商品详细信息
export function getStandardProduct(id) {
  return request({
    url: '/yy/standardProduct/' + id,
    method: 'get'
  })
}

// 根据融合分组ID查询该药品在各平台的数据
export function getStandardProductByFusionGroup(fusionGroupId) {
  return request({
    url: '/yy/standardProduct/byFusionGroup/' + fusionGroupId,
    method: 'get'
  })
}

// 修改标准商品
export function updateStandardProduct(data) {
  return request({
    url: '/yy/standardProduct',
    method: 'put',
    data: data
  })
}

// 删除标准商品
export function delStandardProduct(ids) {
  return request({
    url: '/yy/standardProduct/' + ids,
    method: 'delete'
  })
}
