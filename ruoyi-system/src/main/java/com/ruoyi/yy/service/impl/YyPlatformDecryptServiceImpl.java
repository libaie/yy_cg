package com.ruoyi.yy.service.impl;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.decrypt.DataDecryptUtil;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.mapper.YyPlatformKeyVaultMapper;
import com.ruoyi.yy.service.IYyPlatformDecryptService;
import com.ruoyi.yy.service.IYyPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台数据解密服务实现
 *
 * @author ruoyi
 * @date 2026-05-06
 */
@Service
public class YyPlatformDecryptServiceImpl implements IYyPlatformDecryptService {

    private static final Logger log = LoggerFactory.getLogger(YyPlatformDecryptServiceImpl.class);

    @Autowired
    private IYyPlatformService yyPlatformService;

    @Autowired
    private YyPlatformKeyVaultMapper vaultMapper;

    @Override
    public String decrypt(String platformCode, String encryptedData, int dataEncryptType) {
        // -1 表示明文直传，无需解密
        if (dataEncryptType == -1) {
            return encryptedData;
        }

        try {
            // 1. 根据平台编码查找平台
            YyPlatform platform = yyPlatformService.selectYyPlatformByCode(platformCode);
            if (platform == null) {
                throw new RuntimeException("平台不存在: " + platformCode);
            }

            // 2. 查找密钥金库
            YyPlatformKeyVault vault = vaultMapper.selectYyPlatformKeyVaultByPlatformId(platform.getPId());
            if (vault == null) {
                throw new RuntimeException("密钥配置不存在，平台ID: " + platform.getPId());
            }

            // 3. 查询加密类型字典
            List<SysDictData> encryptTypeList = DictUtils.getDictCache("yy_platform_encrypt_type");
            if (encryptTypeList == null || encryptTypeList.isEmpty()) {
                throw new RuntimeException("字典配置不存在: yy_platform_encrypt_type");
            }

            // 4. 构建字典值→标签映射
            Map<String, String> encryptTypeMap = new HashMap<>();
            for (SysDictData d : encryptTypeList) {
                encryptTypeMap.put(d.getDictValue(), d.getDictLabel());
            }

            // 5. 根据加密类型数值查找对应的标签
            String encryptTypeLabel = encryptTypeMap.get(String.valueOf(dataEncryptType));
            if (encryptTypeLabel == null) {
                throw new RuntimeException("不支持的加密类型: " + dataEncryptType);
            }

            // 6. 委托底层解密
            return decryptByType(encryptedData, encryptTypeLabel, vault);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("解密异常", e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据加密类型标签执行具体解密算法
     */
    private String decryptByType(String encryptedData, String encryptTypeLabel, YyPlatformKeyVault vault) {
        try {
            switch (encryptTypeLabel) {
                case "AES-128-ECB":
                    return DataDecryptUtil.decryptAES128ECB(encryptedData, vault.getSymmetricKey());
                case "AES-128-CBC":
                    return DataDecryptUtil.decryptAES128CBC(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "AES-128-GCM":
                    return DataDecryptUtil.decryptAES128GCM(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "DES-ECB":
                    return DataDecryptUtil.decryptDES(encryptedData, vault.getSymmetricKey());
                case "RSA-PRIVATE-KEY":
                    return DataDecryptUtil.decryptRSA(encryptedData, vault.getRsaPrivateKey());
                case "RSA-PUBLIC-KEY":
                    return DataDecryptUtil.decryptRSAWithPublicKey(encryptedData, vault.getRsaPublicKey());
                case "SM4-ECB":
                    return DataDecryptUtil.decryptSM4ECB(encryptedData, vault.getSymmetricKey());
                case "SM4-CBC":
                    return DataDecryptUtil.decryptSM4CBC(encryptedData, vault.getSymmetricKey(), vault.getSymmetricIv());
                case "BASE64":
                    byte[] decoded = com.ruoyi.common.utils.sign.Base64.decode(encryptedData);
                    return decoded != null ? new String(decoded, "UTF-8") : null;
                default:
                    throw new RuntimeException("不支持的加密类型: " + encryptTypeLabel);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("解密失败: {}", encryptTypeLabel, e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }
}
