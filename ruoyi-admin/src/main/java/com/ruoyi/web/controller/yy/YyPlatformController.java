package com.ruoyi.web.controller.yy;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyUserPlatform;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 平台信息Controller
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@RestController
@RequestMapping("/yy/platform")
public class YyPlatformController extends BaseController
{
    @Autowired
    private IYyPlatformService yyPlatformService;

    /**
     * 查询平台信息列表（管理后台用，分页）
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyPlatform yyPlatform)
    {
        startPage();
        List<YyPlatform> list = yyPlatformService.selectYyPlatformList(yyPlatform);
        return getDataTable(list);
    }

    /**
     * 获取所有启用的平台（含API配置 + 当前用户Token），前端比价用
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        Long userId = SecurityUtils.getUserId();
        List<YyPlatform> list = yyPlatformService.selectActivePlatformsWithApis(userId);
        return success(list);
    }

    /**
     * 获取当前用户的平台绑定列表
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/myList")
    public AjaxResult myList()
    {
        Long userId = SecurityUtils.getUserId();
        List<YyUserPlatform> list = yyPlatformService.selectMyPlatformList(userId);
        return success(list);
    }

    /**
     * 获取指定平台的Token（采集用）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/token/{platformId}")
    public AjaxResult getToken(@PathVariable("platformId") Long platformId)
    {
        Long userId = SecurityUtils.getUserId();
        YyUserPlatform up = yyPlatformService.getPlatformToken(userId, platformId);
        if (up == null) {
            return error("未绑定该平台或Token已失效");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", up.getToken());
        data.put("tokenExpireTime", up.getTokenExpireTime());
        data.put("isValid", up.getLoginStatus() != null && up.getLoginStatus() == 1);
        return success(data);
    }

    /**
     * 绑定平台（保存Token）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody YyUserPlatform bindInfo)
    {
        Long userId = SecurityUtils.getUserId();
        bindInfo.setUserId(userId);
        int rows = yyPlatformService.bindPlatform(userId, bindInfo);
        return rows > 0 ? success("绑定成功") : error("绑定失败");
    }

    /**
     * 解绑平台（清除Token）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/unbind")
    public AjaxResult unbind(@RequestBody Map<String, Long> params)
    {
        Long userId = SecurityUtils.getUserId();
        Long platformId = params.get("platformId");
        int rows = yyPlatformService.unbindPlatform(userId, platformId);
        return rows > 0 ? success("解绑成功") : error("未找到绑定记录");
    }

    /**
     * 同步Token到后端（Chrome扩展调用）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/syncToken")
    public AjaxResult syncToken(@RequestBody Map<String, String> params)
    {
        Long userId = SecurityUtils.getUserId();
        String platformIdStr = params.get("platformId");
        if (platformIdStr == null) { return error("platformId不能为空"); }
        Long platformId;
        try { platformId = Long.parseLong(platformIdStr); } catch (NumberFormatException e) { return error("platformId格式错误"); }
        String token = params.get("token");
        String tokenExpireTime = params.get("tokenExpireTime");
        String lastLoginTime = params.get("lastLoginTime");
        String loginTypeStr = params.get("loginType");
        Integer loginType = null;
        if (loginTypeStr != null) {
            try { loginType = Integer.parseInt(loginTypeStr); } catch (NumberFormatException ignored) {}
        }
        int rows = yyPlatformService.syncToken(userId, platformId, token, tokenExpireTime, lastLoginTime, loginType);
        return rows > 0 ? success("同步成功") : error("同步失败");
    }

    /**
     * 导出平台信息列表
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:export')")
    @Log(title = "平台信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyPlatform yyPlatform)
    {
        List<YyPlatform> list = yyPlatformService.selectYyPlatformList(yyPlatform);
        ExcelUtil<YyPlatform> util = new ExcelUtil<YyPlatform>(YyPlatform.class);
        util.exportExcel(response, list, "平台信息数据");
    }

    /**
     * 获取平台信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:query')")
    @GetMapping(value = "/{pId}")
    public AjaxResult getInfo(@PathVariable("pId") Long pId)
    {
        return success(yyPlatformService.selectYyPlatformByPId(pId));
    }

    /**
     * 新增平台信息
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:add')")
    @Log(title = "平台信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyPlatform yyPlatform)
    {
        return toAjax(yyPlatformService.insertYyPlatform(yyPlatform));
    }

    /**
     * 修改平台信息
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:edit')")
    @Log(title = "平台信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyPlatform yyPlatform)
    {
        return toAjax(yyPlatformService.updateYyPlatform(yyPlatform));
    }

    /**
     * 删除平台信息
     */
    @PreAuthorize("@ss.hasPermi('yy:platform:remove')")
    @Log(title = "平台信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{pIds}")
    public AjaxResult remove(@PathVariable Long[] pIds)
    {
        return toAjax(yyPlatformService.deleteYyPlatformByPIds(pIds));
    }
}
