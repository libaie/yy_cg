import request from '@/utils/request'

// 查询AI模型配置列表
export function listAiModel(query) {
  return request({
    url: '/yy/ai/model-config/list',
    method: 'get',
    params: query
  })
}

// 查询AI模型配置详细
export function getAiModel(id) {
  return request({
    url: '/yy/ai/model-config/' + id,
    method: 'get'
  })
}

// 新增AI模型配置
export function addAiModel(data) {
  return request({
    url: '/yy/ai/model-config',
    method: 'post',
    data: data
  })
}

// 修改AI模型配置
export function updateAiModel(data) {
  return request({
    url: '/yy/ai/model-config',
    method: 'put',
    data: data
  })
}

// 删除AI模型配置
export function delAiModel(id) {
  return request({
    url: '/yy/ai/model-config/' + id,
    method: 'delete'
  })
}
