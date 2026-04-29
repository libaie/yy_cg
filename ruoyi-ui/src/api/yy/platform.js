import request from '@/utils/request'

// 查询平台信息列表（管理后台分页）
export function listPlatform(query) {
  return request({
    url: '/yy/platform/list',
    method: 'get',
    params: query
  })
}

// 获取所有启用平台（业务用，不分页）
export function listAllPlatform() {
  return request({
    url: '/yy/platform/listAll',
    method: 'get'
  })
}

// 查询平台信息详细
export function getPlatform(pId) {
  return request({
    url: '/yy/platform/' + pId,
    method: 'get'
  })
}

// 新增平台信息
export function addPlatform(data) {
  return request({
    url: '/yy/platform',
    method: 'post',
    data: data
  })
}

// 修改平台信息
export function updatePlatform(data) {
  return request({
    url: '/yy/platform',
    method: 'put',
    data: data
  })
}

// 删除平台信息
export function delPlatform(pId) {
  return request({
    url: '/yy/platform/' + pId,
    method: 'delete'
  })
}
