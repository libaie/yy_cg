package com.ruoyi.web.controller.yy;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.service.ICPlatformAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端平台行情分析 Controller
 */
@RestController
@RequestMapping("/yy/cplatform")
public class YyCPlatformController extends BaseController {

    @Autowired
    private ICPlatformAnalysisService cPlatformAnalysisService;

    // ========== C端数据采集 ==========

    /**
     * 采集C端平台数据
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:collect')")
    @PostMapping("/collect")
    public AjaxResult collectCPlatformData(
            @RequestParam String cplatform,
            @RequestParam String keyword) {
        Map<String, Object> result = cPlatformAnalysisService.collectCPlatformData(cplatform, keyword);
        return AjaxResult.success(result);
    }

    /**
     * 批量采集C端数据
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:collect')")
    @PostMapping("/batchCollect")
    public AjaxResult batchCollectCPlatformData(
            @RequestParam String cplatform,
            @RequestBody List<String> keywords) {
        Map<String, Object> result = cPlatformAnalysisService.batchCollectCPlatformData(cplatform, keywords);
        return AjaxResult.success(result);
    }

    // ========== C端数据分析 ==========

    /**
     * 获取C端热销排行
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/hotRanking")
    public AjaxResult getCHotRanking(
            @RequestParam(defaultValue = "meituan") String cplatform,
            @RequestParam(defaultValue = "daily") String rankingType,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = cPlatformAnalysisService.getCHotRanking(cplatform, rankingType, limit);
        return AjaxResult.success(result);
    }

    /**
     * 获取C端价格对比
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/priceCompare")
    public AjaxResult getCPriceComparison(@RequestParam String productName) {
        Map<String, Object> result = cPlatformAnalysisService.getCPriceComparison(productName);
        return AjaxResult.success(result);
    }

    /**
     * 获取C端销售趋势
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/salesTrend")
    public AjaxResult getCSalesTrend(
            @RequestParam String productName,
            @RequestParam String cplatform,
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> result = cPlatformAnalysisService.getCSalesTrend(productName, cplatform, days);
        return AjaxResult.success(result);
    }

    // ========== 选品推荐 ==========

    /**
     * 识别高毛利品种
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/highMargin")
    public AjaxResult identifyHighMarginProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = cPlatformAnalysisService.identifyHighMarginProducts(category, limit);
        return AjaxResult.success(result);
    }

    /**
     * 识别C端热销品种
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/hotProducts")
    public AjaxResult identifyCHotProducts(
            @RequestParam(defaultValue = "meituan") String cplatform,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = cPlatformAnalysisService.identifyCHotProducts(cplatform, limit);
        return AjaxResult.success(result);
    }

    /**
     * 生成上架建议
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/listingSuggestion")
    public AjaxResult generateListingSuggestion(@RequestParam String productName) {
        Map<String, Object> result = cPlatformAnalysisService.generateListingSuggestion(productName);
        return AjaxResult.success(result);
    }

    /**
     * 推荐最优销售渠道
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/bestChannel")
    public AjaxResult recommendBestChannel(@RequestParam String productName) {
        Map<String, Object> result = cPlatformAnalysisService.recommendBestChannel(productName);
        return AjaxResult.success(result);
    }

    // ========== 统计分析 ==========

    /**
     * 获取C端行情统计
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/stats")
    public AjaxResult getCPlatformStats(@RequestParam(required = false) String cplatform) {
        Map<String, Object> result = cPlatformAnalysisService.getCPlatformStats(cplatform);
        return AjaxResult.success(result);
    }

    /**
     * 获取价格竞争力分析
     */
    @PreAuthorize("@ss.hasPermi('yy:cplatform:list')")
    @GetMapping("/competitiveness")
    public AjaxResult getPriceCompetitiveness(@RequestParam String productName) {
        Map<String, Object> result = cPlatformAnalysisService.getPriceCompetitiveness(productName);
        return AjaxResult.success(result);
    }
}
