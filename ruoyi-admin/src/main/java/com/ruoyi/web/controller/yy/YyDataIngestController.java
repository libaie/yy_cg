package com.ruoyi.web.controller.yy;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.dto.YyDataIngestDTO;
import com.ruoyi.yy.service.IDataFusionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据融合接入 Controller
 * Chrome 插件采集数据后通过此接口提交
 */
@RestController
@RequestMapping("/yy/data/ingest")
public class YyDataIngestController extends BaseController {

    @Autowired
    private IDataFusionService dataFusionService;

    /**
     * 数据接入接口
     * 插件采集加密数据 -> 提交 -> 解密 -> 字段映射 -> 融合 -> 入库
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping
    public AjaxResult ingest(@RequestBody YyDataIngestDTO dto) {
        Map<String, Object> result = dataFusionService.ingest(dto);
        String message = (String) result.get("message");
        
        if ("融合成功".equals(message) || (int) result.get("total") > 0) {
            return AjaxResult.success(message, result);
        } else {
            return AjaxResult.error(message);
        }
    }
}
