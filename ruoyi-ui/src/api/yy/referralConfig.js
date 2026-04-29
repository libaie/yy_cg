import request from '@/utils/request'

// 查询推荐奖励配置列表
export function listReferralConfig(query) {
  return request({
    url: '/yy/referral/config/list',
    method: 'get',
    params: query
  })
}

// 查询推荐奖励配置详细
export function getReferralConfig(id) {
  return request({
    url: '/yy/referral/config/' + id,
    method: 'get'
  })
}

// 新增推荐奖励配置
export function addReferralConfig(data) {
  return request({
    url: '/yy/referral/config',
    method: 'post',
    data: data
  })
}

// 修改推荐奖励配置
export function updateReferralConfig(data) {
  return request({
    url: '/yy/referral/config',
    method: 'put',
    data: data
  })
}

// 删除推荐奖励配置
export function delReferralConfig(ids) {
  return request({
    url: '/yy/referral/config/' + ids,
    method: 'delete'
  })
}
