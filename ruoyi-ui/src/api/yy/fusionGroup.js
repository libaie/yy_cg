import request from '@/utils/request'

// 查询商品融合分组列表
export function listFusionGroup(query) {
  return request({
    url: '/yy/fusionGroup/list',
    method: 'get',
    params: query
  })
}

// 获取融合分组详细信息
export function getFusionGroup(id) {
  return request({
    url: '/yy/fusionGroup/' + id,
    method: 'get'
  })
}

// 修改融合分组
export function updateFusionGroup(data) {
  return request({
    url: '/yy/fusionGroup',
    method: 'put',
    data: data
  })
}

// 删除融合分组
export function delFusionGroup(ids) {
  return request({
    url: '/yy/fusionGroup/' + ids,
    method: 'delete'
  })
}
