package com.ruoyi.web.controller.yy;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.yy.domain.YyAiModelConfig;
import com.ruoyi.yy.mapper.YyAiModelConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/yy/ai/model-config")
public class YyAiModelConfigController extends BaseController {

    @Autowired
    private YyAiModelConfigMapper modelConfigMapper;

    /** 客户端获取已启用的模型列表 */
    @GetMapping("/enabled")
    public AjaxResult enabledModels() {
        List<YyAiModelConfig> models = modelConfigMapper.selectEnabled();
        return success(models);
    }

    /** 管理端：全部模型列表 */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public TableDataInfo list() {
        startPage();
        List<YyAiModelConfig> list = modelConfigMapper.selectAll();
        return getDataTable(list);
    }

    /** 管理端：详情 */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(modelConfigMapper.selectById(id));
    }

    /** 管理端：新增 */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult add(@RequestBody YyAiModelConfig config) {
        return toAjax(modelConfigMapper.insert(config));
    }

    /** 管理端：更新 */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult edit(@RequestBody YyAiModelConfig config) {
        return toAjax(modelConfigMapper.updateById(config));
    }

    /** 管理端：删除 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('yy:admin:ai:quota')")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(modelConfigMapper.deleteById(id));
    }
}
