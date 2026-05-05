package com.ruoyi.yy.service;

/**
 * 平台数据解密服务接口
 * <p>
 * 面向不同平台的加密数据提供统一解密入口，
 * 将平台查找、密钥金库查询、加密类型字典解析和底层解密算法整合为一体。
 *
 * @author ruoyi
 * @date 2026-05-06
 */
public interface IYyPlatformDecryptService {

    /**
     * 根据平台编码和加密类型解密数据
     *
     * @param platformCode    平台编码
     * @param encryptedData   加密数据
     * @param dataEncryptType 加密类型（与字典yy_platform_encrypt_type对应；-1表示明文直传）
     * @return 解密后的明文
     */
    String decrypt(String platformCode, String encryptedData, int dataEncryptType);
}
