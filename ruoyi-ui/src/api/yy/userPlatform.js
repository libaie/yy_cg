import request from '@/utils/request'

// 查询用户平台关联列表
export function listUserPlatform(query) {
  return request({
    url: '/yy/userPlatform/list',
    method: 'get',
    params: query
  })
}

// 查询用户平台关联详细
export function getUserPlatform(id) {
  return request({
    url: '/yy/userPlatform/' + id,
    method: 'get'
  })
}

// 新增用户平台关联
export function addUserPlatform(data) {
  return request({
    url: '/yy/userPlatform',
    method: 'post',
    data: data
  })
}

// 修改用户平台关联
export function updateUserPlatform(data) {
  return request({
    url: '/yy/userPlatform',
    method: 'put',
    data: data
  })
}

// 删除用户平台关联
export function delUserPlatform(id) {
  return request({
    url: '/yy/userPlatform/' + id,
    method: 'delete'
  })
}
