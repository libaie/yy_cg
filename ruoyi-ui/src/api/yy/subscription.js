import request from '@/utils/request'

// 查询会员订阅订单列表（支持管理员和普通用户）
export function listSubscription(query) {
  return request({
    url: '/yy/subscription/list',
    method: 'get',
    params: query
  })
}

// 查询会员订阅订单详细（支持管理员和普通用户）
export function getSubscription(subId) {
  return request({
    url: '/yy/subscription/' + subId,
    method: 'get'
  })
}

// 修改会员订阅订单（仅允许修改业务可调字段）
export function updateSubscription(data) {
  return request({
    url: '/yy/subscription',
    method: 'put',
    data: data
  })
}

// 删除会员订阅订单
export function delSubscription(subId) {
  return request({
    url: '/yy/subscription/' + subId,
    method: 'delete'
  })
}

// 查询我的订阅订单列表（普通用户专用）
export function mySubscriptionList(query) {
  return request({
    url: '/yy/subscription/myList',
    method: 'get',
    params: query
  })
}

// 创建预付款订单
export function createPreOrder(data) {
  return request({
    url: '/yy/subscription/preorder',
    method: 'post',
    data: data
  })
}
