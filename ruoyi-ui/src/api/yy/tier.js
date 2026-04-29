import request from '@/utils/request'

// 查询会员套餐配置列表
export function listTier(query) {
  return request({
    url: '/yy/tier/list',
    method: 'get',
    params: query
  })
}

// 查询会员套餐配置详细
export function getTier(tierId) {
  return request({
    url: '/yy/tier/' + tierId,
    method: 'get'
  })
}

// 新增会员套餐配置
export function addTier(data) {
  return request({
    url: '/yy/tier',
    method: 'post',
    data: data
  })
}

// 修改会员套餐配置
export function updateTier(data) {
  return request({
    url: '/yy/tier',
    method: 'put',
    data: data
  })
}

// 删除会员套餐配置
export function delTier(tierId) {
  return request({
    url: '/yy/tier/' + tierId,
    method: 'delete'
  })
}
