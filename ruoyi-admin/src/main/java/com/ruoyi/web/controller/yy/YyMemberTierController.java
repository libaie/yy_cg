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
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.service.IYyMemberTierService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.common.core.domain.entity.SysDictData;

/**
 * 会员套餐配置Controller
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@RestController
@RequestMapping("/yy/tier")
public class YyMemberTierController extends BaseController
{
    @Autowired
    private IYyMemberTierService yyMemberTierService;

    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 查询会员套餐配置列表
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyMemberTier yyMemberTier)
    {
        startPage();
        List<YyMemberTier> list = yyMemberTierService.selectYyMemberTierList(yyMemberTier);
        return getDataTable(list);
    }

    /**
     * 获取所有可用的会员套餐列表（医药助手前端用）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        YyMemberTier query = new YyMemberTier();
        query.setIsActive(1); // 仅查询已上架/激活的套餐
        List<YyMemberTier> list = yyMemberTierService.selectYyMemberTierList(query);
        
        // 同时获取字典数据，方便前端渲染等级名称
        SysDictData dictDataQuery = new SysDictData();
        dictDataQuery.setDictType("yy_member_tier_name");
        dictDataQuery.setStatus("0"); // 仅查询正常的字典项
        List<SysDictData> dictDatas = dictDataService.selectDictDataList(dictDataQuery);
        
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", list);
        ajax.put("tierDict", dictDatas);
        return ajax;
    }

    /**
     * 导出会员套餐配置列表
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:export')")
    @Log(title = "会员套餐配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyMemberTier yyMemberTier)
    {
        List<YyMemberTier> list = yyMemberTierService.selectYyMemberTierList(yyMemberTier);
        ExcelUtil<YyMemberTier> util = new ExcelUtil<YyMemberTier>(YyMemberTier.class);
        util.exportExcel(response, list, "会员套餐配置数据");
    }

    /**
     * 获取会员套餐配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:query')")
    @GetMapping(value = "/{tierId}")
    public AjaxResult getInfo(@PathVariable("tierId") Long tierId)
    {
        return success(yyMemberTierService.selectYyMemberTierByTierId(tierId));
    }

    /**
     * 新增会员套餐配置
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:add')")
    @Log(title = "会员套餐配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyMemberTier yyMemberTier)
    {
        return toAjax(yyMemberTierService.insertYyMemberTier(yyMemberTier));
    }

    /**
     * 修改会员套餐配置
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:edit')")
    @Log(title = "会员套餐配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyMemberTier yyMemberTier)
    {
        return toAjax(yyMemberTierService.updateYyMemberTier(yyMemberTier));
    }

    /**
     * 删除会员套餐配置
     */
    @PreAuthorize("@ss.hasPermi('yy:tier:remove')")
    @Log(title = "会员套餐配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{tierIds}")
    public AjaxResult remove(@PathVariable Long[] tierIds)
    {
        return toAjax(yyMemberTierService.deleteYyMemberTierByTierIds(tierIds));
    }
}
