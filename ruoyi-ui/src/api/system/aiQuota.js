import request from '@/utils/request'

// 查询AI配额列表
export function listAiQuota() {
  return request({
    url: '/yy/ai-quota/list',
    method: 'get'
  })
}

// 查询AI配额详细
export function getAiQuota(id) {
  return request({
    url: '/yy/ai-quota/' + id,
    method: 'get'
  })
}

// 修改AI配额
export function updateAiQuota(data) {
  return request({
    url: '/yy/ai-quota',
    method: 'put',
    data: data
  })
}
