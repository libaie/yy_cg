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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyReferralConfig;
import com.ruoyi.yy.service.IYyReferralConfigService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 推荐奖励配置Controller
 */
@RestController
@RequestMapping("/yy/referral/config")
public class YyReferralConfigController extends BaseController
{
    @Autowired
    private IYyReferralConfigService yyReferralConfigService;

    @PreAuthorize("@ss.hasPermi('yy:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyReferralConfig config) {
        startPage();
        List<YyReferralConfig> list = yyReferralConfigService.selectYyReferralConfigList(config);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('yy:config:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(yyReferralConfigService.selectYyReferralConfigById(id));
    }

    @PreAuthorize("@ss.hasPermi('yy:config:add')")
    @Log(title = "推荐奖励配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyReferralConfig config) {
        return toAjax(yyReferralConfigService.insertYyReferralConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('yy:config:edit')")
    @Log(title = "推荐奖励配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyReferralConfig config) {
        return toAjax(yyReferralConfigService.updateYyReferralConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('yy:config:remove')")
    @Log(title = "推荐奖励配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(yyReferralConfigService.deleteYyReferralConfigByIds(ids));
    }
}
