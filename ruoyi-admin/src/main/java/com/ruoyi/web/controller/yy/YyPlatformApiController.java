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
import com.ruoyi.yy.domain.YyPlatformApi;
import com.ruoyi.yy.service.IYyPlatformApiService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 平台API配置Controller
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
@RestController
@RequestMapping("/yy/platformApi")
public class YyPlatformApiController extends BaseController
{
    @Autowired
    private IYyPlatformApiService yyPlatformApiService;

    /**
     * 查询平台API配置列表
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyPlatformApi yyPlatformApi)
    {
        startPage();
        List<YyPlatformApi> list = yyPlatformApiService.selectYyPlatformApiList(yyPlatformApi);
        return getDataTable(list);
    }

    /**
     * 导出平台API配置列表
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:export')")
    @Log(title = "平台API配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyPlatformApi yyPlatformApi)
    {
        List<YyPlatformApi> list = yyPlatformApiService.selectYyPlatformApiList(yyPlatformApi);
        ExcelUtil<YyPlatformApi> util = new ExcelUtil<YyPlatformApi>(YyPlatformApi.class);
        util.exportExcel(response, list, "平台API配置数据");
    }

    /**
     * 获取平台API配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:query')")
    @GetMapping(value = "/{apiId}")
    public AjaxResult getInfo(@PathVariable("apiId") Long apiId)
    {
        return success(yyPlatformApiService.selectYyPlatformApiByApiId(apiId));
    }

    /**
     * 新增平台API配置
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:add')")
    @Log(title = "平台API配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyPlatformApi yyPlatformApi)
    {
        return toAjax(yyPlatformApiService.insertYyPlatformApi(yyPlatformApi));
    }

    /**
     * 修改平台API配置
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:edit')")
    @Log(title = "平台API配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyPlatformApi yyPlatformApi)
    {
        return toAjax(yyPlatformApiService.updateYyPlatformApi(yyPlatformApi));
    }

    /**
     * 删除平台API配置
     */
    @PreAuthorize("@ss.hasPermi('yy:platformApi:remove')")
    @Log(title = "平台API配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{apiIds}")
    public AjaxResult remove(@PathVariable Long[] apiIds)
    {
        return toAjax(yyPlatformApiService.deleteYyPlatformApiByApiIds(apiIds));
    }
}
