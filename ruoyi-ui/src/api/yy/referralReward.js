import request from '@/utils/request'

// 查询推荐奖励记录列表
export function listReferralReward(query) {
  return request({
    url: '/yy/referral/reward/list',
    method: 'get',
    params: query
  })
}

// 查询推荐奖励记录详细
export function getReferralReward(id) {
  return request({
    url: '/yy/referral/reward/' + id,
    method: 'get'
  })
}
