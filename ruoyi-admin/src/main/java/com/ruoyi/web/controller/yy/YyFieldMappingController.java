package com.ruoyi.web.controller.yy;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyFieldMapping;
import com.ruoyi.yy.service.IYyFieldMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字段映射配置 Controller
 */
@RestController
@RequestMapping("/yy/fieldMapping")
public class YyFieldMappingController extends BaseController {

    @Autowired
    private IYyFieldMappingService yyFieldMappingService;

    /** 查询字段映射配置列表 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyFieldMapping yyFieldMapping) {
        startPage();
        List<YyFieldMapping> list = yyFieldMappingService.selectYyFieldMappingList(yyFieldMapping);
        return getDataTable(list);
    }

    /** 获取指定平台的所有映射（不分页，用于配置页面加载） */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:list')")
    @GetMapping("/byPlatform/{platformId}")
    public AjaxResult getByPlatform(@PathVariable Long platformId) {
        List<YyFieldMapping> list = yyFieldMappingService.selectYyFieldMappingList(
            new YyFieldMapping() {{ setPlatformId(platformId); }}
        );
        Map<String, String> mappings = yyFieldMappingService.getPlatformMappings(platformId);
        return AjaxResult.success().put("list", list).put("mappings", mappings);
    }

    /** 获取字段映射配置详细信息 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(yyFieldMappingService.selectYyFieldMappingById(id));
    }

    /** 新增字段映射配置 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:add')")
    @Log(title = "字段映射配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyFieldMapping yyFieldMapping) {
        return toAjax(yyFieldMappingService.insertYyFieldMapping(yyFieldMapping));
    }

    /** 修改字段映射配置 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:edit')")
    @Log(title = "字段映射配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyFieldMapping yyFieldMapping) {
        return toAjax(yyFieldMappingService.updateYyFieldMapping(yyFieldMapping));
    }

    /** 批量保存某个平台的映射配置 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:edit')")
    @Log(title = "字段映射配置", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/{platformId}")
    public AjaxResult batchSave(@PathVariable Long platformId, @RequestBody List<YyFieldMapping> list) {
        return toAjax(yyFieldMappingService.batchSave(platformId, list));
    }

    /** 删除字段映射配置 */
    @PreAuthorize("@ss.hasPermi('yy:fieldMapping:remove')")
    @Log(title = "字段映射配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(yyFieldMappingService.deleteYyFieldMappingByIds(ids));
    }
}
