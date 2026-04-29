package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyPlatformKeyVault;

/**
 * 平台密钥金库Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-12
 */
public interface YyPlatformKeyVaultMapper 
{
    /**
     * 查询平台密钥金库
     * 
     * @param vaultId 平台密钥金库主键
     * @return 平台密钥金库
     */
    public YyPlatformKeyVault selectYyPlatformKeyVaultByVaultId(Long vaultId);

    /**
     * 查询平台密钥金库列表
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 平台密钥金库集合
     */
    public List<YyPlatformKeyVault> selectYyPlatformKeyVaultList(YyPlatformKeyVault yyPlatformKeyVault);

    /**
     * 根据平台ID查询平台密钥金库
     * 
     * @param platformId 平台ID
     * @return 平台密钥金库
     */
    public YyPlatformKeyVault selectYyPlatformKeyVaultByPlatformId(Long platformId);

    /**
     * 新增平台密钥金库
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 结果
     */
    public int insertYyPlatformKeyVault(YyPlatformKeyVault yyPlatformKeyVault);

    /**
     * 修改平台密钥金库
     * 
     * @param yyPlatformKeyVault 平台密钥金库
     * @return 结果
     */
    public int updateYyPlatformKeyVault(YyPlatformKeyVault yyPlatformKeyVault);

    /**
     * 删除平台密钥金库
     * 
     * @param vaultId 平台密钥金库主键
     * @return 结果
     */
    public int deleteYyPlatformKeyVaultByVaultId(Long vaultId);

    /**
     * 批量删除平台密钥金库
     * 
     * @param vaultIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYyPlatformKeyVaultByVaultIds(Long[] vaultIds);
}
