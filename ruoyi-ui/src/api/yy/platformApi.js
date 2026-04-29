import request from '@/utils/request'

// 查询平台API配置列表
export function listPlatformApi(query) {
  return request({
    url: '/yy/platformApi/list',
    method: 'get',
    params: query
  })
}

// 查询平台API配置详细
export function getPlatformApi(apiId) {
  return request({
    url: '/yy/platformApi/' + apiId,
    method: 'get'
  })
}

// 新增平台API配置
export function addPlatformApi(data) {
  return request({
    url: '/yy/platformApi',
    method: 'post',
    data: data
  })
}

// 修改平台API配置
export function updatePlatformApi(data) {
  return request({
    url: '/yy/platformApi',
    method: 'put',
    data: data
  })
}

// 删除平台API配置
export function delPlatformApi(apiId) {
  return request({
    url: '/yy/platformApi/' + apiId,
    method: 'delete'
  })
}
