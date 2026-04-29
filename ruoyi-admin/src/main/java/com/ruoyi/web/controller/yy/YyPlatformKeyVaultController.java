package com.ruoyi.web.controller.yy;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.service.IYyPlatformKeyVaultService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 平台密钥金库Controller
 * 
 * @author ruoyi
 * @date 2026-04-12
 */
@RestController
@RequestMapping({"/system/vault", "/yy/vault"})
public class YyPlatformKeyVaultController extends BaseController
{
    @Autowired
    private IYyPlatformKeyVaultService yyPlatformKeyVaultService;

    /**
     * 查询平台密钥金库列表
     */
    @PreAuthorize("@ss.hasPermi('system:vault:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyPlatformKeyVault yyPlatformKeyVault)
    {
        startPage();
        List<YyPlatformKeyVault> list = yyPlatformKeyVaultService.selectYyPlatformKeyVaultList(yyPlatformKeyVault);
        return getDataTable(list);
    }

    /**
     * 导出平台密钥金库列表
     */
    @PreAuthorize("@ss.hasPermi('system:vault:export')")
    @Log(title = "平台密钥金库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyPlatformKeyVault yyPlatformKeyVault)
    {
        List<YyPlatformKeyVault> list = yyPlatformKeyVaultService.selectYyPlatformKeyVaultList(yyPlatformKeyVault);
        ExcelUtil<YyPlatformKeyVault> util = new ExcelUtil<YyPlatformKeyVault>(YyPlatformKeyVault.class);
        util.exportExcel(response, list, "平台密钥金库数据");
    }

    /**
     * 获取平台密钥金库详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:vault:query')")
    @GetMapping(value = "/{vaultId}")
    public AjaxResult getInfo(@PathVariable("vaultId") Long vaultId)
    {
        return success(yyPlatformKeyVaultService.selectYyPlatformKeyVaultByVaultId(vaultId));
    }

    /**
     * 获取平台密钥金库信息（按平台ID）
     */
    @PreAuthorize("@ss.hasPermi('system:vault:query')")
    @GetMapping(value = "/platform/{platformId}")
    public AjaxResult getByPlatformId(@PathVariable("platformId") Long platformId)
    {
        YyPlatformKeyVault vault = yyPlatformKeyVaultService.selectYyPlatformKeyVaultByPlatformId(platformId);
        if (vault == null)
        {
            vault = new YyPlatformKeyVault();
            vault.setPlatformId(platformId);
        }
        else
        {
            maskSensitiveFields(vault);
        }
        return success(vault);
    }

    /**
     * 新增平台密钥金库
     */
    @PreAuthorize("@ss.hasPermi('system:vault:add')")
    @Log(title = "平台密钥金库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyPlatformKeyVault yyPlatformKeyVault)
    {
        return toAjax(yyPlatformKeyVaultService.insertYyPlatformKeyVault(yyPlatformKeyVault));
    }

    /**
     * 修改平台密钥金库
     */
    @PreAuthorize("@ss.hasPermi('system:vault:edit')")
    @Log(title = "平台密钥金库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyPlatformKeyVault yyPlatformKeyVault)
    {
        maskSensitiveFieldsForUpdate(yyPlatformKeyVault);
        return toAjax(yyPlatformKeyVaultService.updateYyPlatformKeyVault(yyPlatformKeyVault));
    }

    /**
     * 删除平台密钥金库
     */
    @PreAuthorize("@ss.hasPermi('system:vault:remove')")
    @Log(title = "平台密钥金库", businessType = BusinessType.DELETE)
	@DeleteMapping("/{vaultIds}")
    public AjaxResult remove(@PathVariable Long[] vaultIds)
    {
        return toAjax(yyPlatformKeyVaultService.deleteYyPlatformKeyVaultByVaultIds(vaultIds));
    }

    private void maskSensitiveFields(YyPlatformKeyVault vault) {
        if (StringUtils.isNotBlank(vault.getSymmetricKey())) vault.setSymmetricKey("********");
        if (StringUtils.isNotBlank(vault.getSymmetricIv())) vault.setSymmetricIv("********");
        if (StringUtils.isNotBlank(vault.getRsaPrivateKey())) vault.setRsaPrivateKey("********");
        if (StringUtils.isNotBlank(vault.getAppKey())) vault.setAppKey("********");
        if (StringUtils.isNotBlank(vault.getAppSecret())) vault.setAppSecret("********");
    }

    private void maskSensitiveFieldsForUpdate(YyPlatformKeyVault yyPlatformKeyVault) {
        if (StringUtils.equals(yyPlatformKeyVault.getSymmetricKey(), "********")) yyPlatformKeyVault.setSymmetricKey(null);
        if (StringUtils.equals(yyPlatformKeyVault.getSymmetricIv(), "********")) yyPlatformKeyVault.setSymmetricIv(null);
        if (StringUtils.equals(yyPlatformKeyVault.getRsaPrivateKey(), "********")) yyPlatformKeyVault.setRsaPrivateKey(null);
        if (StringUtils.equals(yyPlatformKeyVault.getAppKey(), "********")) yyPlatformKeyVault.setAppKey(null);
        if (StringUtils.equals(yyPlatformKeyVault.getAppSecret(), "********")) yyPlatformKeyVault.setAppSecret(null);
    }
}
