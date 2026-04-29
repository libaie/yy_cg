package com.ruoyi.web.controller.yy;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.service.IYyProductFusionGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品融合分组 Controller
 */
@RestController
@RequestMapping("/yy/fusionGroup")
public class YyFusionGroupController extends BaseController {

    @Autowired
    private IYyProductFusionGroupService yyProductFusionGroupService;

    /** 查询商品融合分组列表 */
    @PreAuthorize("@ss.hasPermi('yy:fusionGroup:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyProductFusionGroup yyProductFusionGroup) {
        startPage();
        List<YyProductFusionGroup> list = yyProductFusionGroupService.selectYyProductFusionGroupList(yyProductFusionGroup);
        return getDataTable(list);
    }

    /** 获取商品融合分组详细信息 */
    @PreAuthorize("@ss.hasPermi('yy:fusionGroup:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(yyProductFusionGroupService.selectYyProductFusionGroupById(id));
    }

    /** 修改商品融合分组 */
    @PreAuthorize("@ss.hasPermi('yy:fusionGroup:edit')")
    @Log(title = "商品融合分组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyProductFusionGroup yyProductFusionGroup) {
        return toAjax(yyProductFusionGroupService.updateYyProductFusionGroup(yyProductFusionGroup));
    }

    /** 删除商品融合分组 */
    @PreAuthorize("@ss.hasPermi('yy:fusionGroup:remove')")
    @Log(title = "商品融合分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(yyProductFusionGroupService.deleteYyProductFusionGroupByIds(ids));
    }
}
