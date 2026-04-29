package com.ruoyi.yy.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.decrypt.DataDecryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yy.mapper.YyPlatformKeyVaultMapper;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.service.IYyPlatformKeyVaultService;

/**
 * 平台密钥金库Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-12
 */
@Service
public class YyPlatformKeyVaultServiceImpl implements IYyPlatformKeyVaultService 
{
    private static final String VAULT_ENCRYPT_KEY = "platformVaultKey1234567890123456";

    @Autowired
    private YyPlatformKeyVaultMapper yyPlatformKeyVaultMapper;

    /**
     * 查询平台密钥金库
     * 
     * @param vaultId 平台密钥金库主键
     * @return 平台密钥金库
     */
    @Override
    public YyPlatformKeyVault selectYyPlatformKeyVaultByVaultId(Long vaultId)
    {
        return yyPlatformKeyVaultMapper.selectYyPlatformKeyVaultByVaultId(vaultId);
    }

    /**
     * 查询平台密钥金库列表
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 平台密钥金库
     */
    @Override
    public List<YyPlatformKeyVault> selectYyPlatformKeyVaultList(YyPlatformKeyVault yyPlatformKeyVault)
    {
        return yyPlatformKeyVaultMapper.selectYyPlatformKeyVaultList(yyPlatformKeyVault);
    }

    /**
     * 根据平台ID查询平台密钥金库
     * 
     * @param platformId 平台ID
     * @return 平台密钥金库
     */
    @Override
    public YyPlatformKeyVault selectYyPlatformKeyVaultByPlatformId(Long platformId)
    {
        YyPlatformKeyVault vault = yyPlatformKeyVaultMapper.selectYyPlatformKeyVaultByPlatformId(platformId);
        if (vault != null) {
            decryptSensitiveFields(vault);
        }
        return vault;
    }

    /**
     * 新增平台密钥金库
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 结果
     */
    @Override
    public int insertYyPlatformKeyVault(YyPlatformKeyVault yyPlatformKeyVault)
    {
        encryptSensitiveFields(yyPlatformKeyVault);
        return yyPlatformKeyVaultMapper.insertYyPlatformKeyVault(yyPlatformKeyVault);
    }

    /**
     * 修改平台密钥金库
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 结果
     */
    @Override
    public int updateYyPlatformKeyVault(YyPlatformKeyVault yyPlatformKeyVault)
    {
        encryptSensitiveFields(yyPlatformKeyVault);
        yyPlatformKeyVault.setUpdateTime(DateUtils.getNowDate());
        return yyPlatformKeyVaultMapper.updateYyPlatformKeyVault(yyPlatformKeyVault);
    }

    /**
     * 批量删除平台密钥金库
     * 
     * @param vaultIds 需要删除的平台密钥金库主键
     * @return 结果
     */
    @Override
    public int deleteYyPlatformKeyVaultByVaultIds(Long[] vaultIds)
    {
        return yyPlatformKeyVaultMapper.deleteYyPlatformKeyVaultByVaultIds(vaultIds);
    }

    /**
     * 删除平台密钥金库信息
     * 
     * @param vaultId 平台密钥金库主键
     * @return 结果
     */
    @Override
    public int deleteYyPlatformKeyVaultByVaultId(Long vaultId)
    {
        return yyPlatformKeyVaultMapper.deleteYyPlatformKeyVaultByVaultId(vaultId);
    }

    private void encryptSensitiveFields(YyPlatformKeyVault yyPlatformKeyVault) {
        encryptField(yyPlatformKeyVault.getSymmetricKey(), value -> yyPlatformKeyVault.setSymmetricKey(value));
        encryptField(yyPlatformKeyVault.getSymmetricIv(), value -> yyPlatformKeyVault.setSymmetricIv(value));
        encryptField(yyPlatformKeyVault.getRsaPrivateKey(), value -> yyPlatformKeyVault.setRsaPrivateKey(value));
        encryptField(yyPlatformKeyVault.getAppKey(), value -> yyPlatformKeyVault.setAppKey(value));
        encryptField(yyPlatformKeyVault.getAppSecret(), value -> yyPlatformKeyVault.setAppSecret(value));
    }

    private void decryptSensitiveFields(YyPlatformKeyVault yyPlatformKeyVault) {
        decryptField(yyPlatformKeyVault.getSymmetricKey(), value -> yyPlatformKeyVault.setSymmetricKey(value));
        decryptField(yyPlatformKeyVault.getSymmetricIv(), value -> yyPlatformKeyVault.setSymmetricIv(value));
        decryptField(yyPlatformKeyVault.getRsaPrivateKey(), value -> yyPlatformKeyVault.setRsaPrivateKey(value));
        decryptField(yyPlatformKeyVault.getAppKey(), value -> yyPlatformKeyVault.setAppKey(value));
        decryptField(yyPlatformKeyVault.getAppSecret(), value -> yyPlatformKeyVault.setAppSecret(value));
    }

    private void encryptField(String fieldValue, java.util.function.Consumer<String> setter) {
        if (StringUtils.isNotBlank(fieldValue) && !"********".equals(fieldValue)) {
            try {
                setter.accept(DataDecryptUtil.encryptAES128ECB(fieldValue, VAULT_ENCRYPT_KEY));
            } catch (Exception e) {
                // 加密失败，保持原值
            }
        } else if ("********".equals(fieldValue)) {
            setter.accept(null);
        }
    }

    private void decryptField(String fieldValue, java.util.function.Consumer<String> setter) {
        if (StringUtils.isNotBlank(fieldValue)) {
            try {
                setter.accept(DataDecryptUtil.decryptAES128ECB(fieldValue, VAULT_ENCRYPT_KEY));
            } catch (Exception e) {
                // 解密失败，保持原值
            }
        }
    }
}
