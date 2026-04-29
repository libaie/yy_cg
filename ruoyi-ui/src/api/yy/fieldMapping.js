import request from '@/utils/request'

// 查询字段映射配置列表
export function listFieldMapping(query) {
  return request({
    url: '/yy/fieldMapping/list',
    method: 'get',
    params: query
  })
}

// 获取指定平台的所有映射
export function getFieldMappingByPlatform(platformId) {
  return request({
    url: '/yy/fieldMapping/byPlatform/' + platformId,
    method: 'get'
  })
}

// 新增字段映射配置
export function addFieldMapping(data) {
  return request({
    url: '/yy/fieldMapping',
    method: 'post',
    data: data
  })
}

// 修改字段映射配置
export function updateFieldMapping(data) {
  return request({
    url: '/yy/fieldMapping',
    method: 'put',
    data: data
  })
}

// 批量保存某个平台的映射配置
export function batchSaveFieldMapping(platformId, data) {
  return request({
    url: '/yy/fieldMapping/batch/' + platformId,
    method: 'put',
    data: data
  })
}

// 删除字段映射配置
export function delFieldMapping(ids) {
  return request({
    url: '/yy/fieldMapping/' + ids,
    method: 'delete'
  })
}
