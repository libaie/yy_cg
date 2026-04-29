import request from '@/utils/request'

// 根据平台ID获取密钥金库信息
export function getVaultByPlatform(platformId) {
  return request({
    url: '/yy/vault/platform/' + platformId,
    method: 'get'
  })
}

// 新增平台密钥金库
export function addVault(data) {
  return request({
    url: '/yy/vault',
    method: 'post',
    data: data
  })
}

// 修改平台密钥金库
export function updateVault(data) {
  return request({
    url: '/yy/vault',
    method: 'put',
    data: data
  })
}
