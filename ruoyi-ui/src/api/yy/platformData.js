import request from '@/utils/request'

// 数据解密 + 字段映射
export function decryptAndMap(data) {
  return request({
    url: '/yy/platform/data/decrypt',
    method: 'post',
    data: data
  })
}
