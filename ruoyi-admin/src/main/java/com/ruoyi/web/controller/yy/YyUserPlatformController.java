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
import com.ruoyi.yy.domain.YyUserPlatform;
import com.ruoyi.yy.service.IYyUserPlatformService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户平台关联Controller
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
@RestController
@RequestMapping("/yy/userPlatform")
public class YyUserPlatformController extends BaseController
{
    @Autowired
    private IYyUserPlatformService yyUserPlatformService;

    /**
     * 查询用户平台关联列表
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyUserPlatform yyUserPlatform)
    {
        startPage();
        List<YyUserPlatform> list = yyUserPlatformService.selectYyUserPlatformList(yyUserPlatform);
        return getDataTable(list);
    }

    /**
     * 导出用户平台关联列表
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:export')")
    @Log(title = "用户平台关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyUserPlatform yyUserPlatform)
    {
        List<YyUserPlatform> list = yyUserPlatformService.selectYyUserPlatformList(yyUserPlatform);
        ExcelUtil<YyUserPlatform> util = new ExcelUtil<YyUserPlatform>(YyUserPlatform.class);
        util.exportExcel(response, list, "用户平台关联数据");
    }

    /**
     * 获取用户平台关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(yyUserPlatformService.selectYyUserPlatformById(id));
    }

    /**
     * 新增用户平台关联
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:add')")
    @Log(title = "用户平台关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyUserPlatform yyUserPlatform)
    {
        return toAjax(yyUserPlatformService.insertYyUserPlatform(yyUserPlatform));
    }

    /**
     * 修改用户平台关联
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:edit')")
    @Log(title = "用户平台关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyUserPlatform yyUserPlatform)
    {
        return toAjax(yyUserPlatformService.updateYyUserPlatform(yyUserPlatform));
    }

    /**
     * 删除用户平台关联
     */
    @PreAuthorize("@ss.hasPermi('yy:userPlatform:remove')")
    @Log(title = "用户平台关联", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(yyUserPlatformService.deleteYyUserPlatformByIds(ids));
    }
}
