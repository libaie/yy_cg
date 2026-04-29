import request from '@/utils/request'

// 查询用户列表
export function listUser(query) {
  return request({
    url: '/yy/user/list',
    method: 'get',
    params: query
  })
}

// 查询用户详细
export function getUser(userId) {
  return request({
    url: '/yy/user/' + userId,
    method: 'get'
  })
}

// 新增用户
export function addUser(data) {
  return request({
    url: '/yy/user',
    method: 'post',
    data: data
  })
}

// 修改用户
export function updateUser(data) {
  return request({
    url: '/yy/user',
    method: 'put',
    data: data
  })
}

// 删除用户
export function delUser(userId) {
  return request({
    url: '/yy/user/' + userId,
    method: 'delete'
  })
}

// 获取个人资料（包含会员信息）
export function getProfile() {
  return request({
    url: '/yy/user/profile',
    method: 'get'
  })
}

// 会员预下单
export function preorder(data) {
  return request({
    url: '/yy/user/preorder',
    method: 'post',
    data: data
  })
}

// 绑定推荐人
export function bindReferrer(data) {
  return request({
    url: '/yy/user/bindReferrer',
    method: 'post',
    data: data
  })
}

// 获取我的邀请码
export function getMyInviteCode() {
  return request({
    url: '/yy/user/myInviteCode',
    method: 'get'
  })
}
