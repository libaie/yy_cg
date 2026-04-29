package com.ruoyi.web.controller.yy;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.service.ISalesForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销售预测 Controller
 */
@RestController
@RequestMapping("/yy/forecast")
public class YySalesForecastController extends BaseController {

    @Autowired
    private ISalesForecastService salesForecastService;

    // ========== 热销预测 ==========

    /**
     * 15天热销预测
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/15days")
    public AjaxResult forecast15Days(
            @RequestParam(required = false) String skuId,
            @RequestParam(required = false) String regionCode) {
        List<Map<String, Object>> result = salesForecastService.forecast15Days(skuId, regionCode);
        return AjaxResult.success(result);
    }

    /**
     * 30天热销预测
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/30days")
    public AjaxResult forecast30Days(
            @RequestParam(required = false) String skuId,
            @RequestParam(required = false) String regionCode) {
        List<Map<String, Object>> result = salesForecastService.forecast30Days(skuId, regionCode);
        return AjaxResult.success(result);
    }

    /**
     * 按品类预测
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/byCategory/{categoryId}")
    public AjaxResult forecastByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "15") int days,
            @RequestParam(required = false) String regionCode) {
        List<Map<String, Object>> result = salesForecastService.forecastByCategory(categoryId, days, regionCode);
        return AjaxResult.success(result);
    }

    // ========== 热销榜单 ==========

    /**
     * 获取热销TOP榜单
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/ranking")
    public AjaxResult getHotProductRanking(
            @RequestParam(defaultValue = "daily") String rankingType,
            @RequestParam(required = false) String regionCode,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = salesForecastService.getHotProductRanking(rankingType, regionCode, limit);
        return AjaxResult.success(result);
    }

    /**
     * 获取品类热销榜
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/ranking/category/{categoryId}")
    public AjaxResult getHotProductByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "daily") String rankingType,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = salesForecastService.getHotProductByCategory(categoryId, rankingType, limit);
        return AjaxResult.success(result);
    }

    // ========== 缺货预警 ==========

    /**
     * 检查缺货预警
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/alert")
    public AjaxResult checkStockAlert(
            @RequestParam(required = false) String skuId,
            @RequestParam(required = false) String platformCode) {
        List<Map<String, Object>> result = salesForecastService.checkStockAlert(skuId, platformCode);
        return AjaxResult.success(result);
    }

    /**
     * 获取预警统计
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/alert/stats")
    public AjaxResult getAlertStats() {
        Map<String, Object> result = salesForecastService.getAlertStats();
        return AjaxResult.success(result);
    }

    /**
     * 处理预警
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:edit')")
    @PutMapping("/alert/{alertId}")
    public AjaxResult handleAlert(
            @PathVariable Long alertId,
            @RequestParam String action,
            @RequestParam String operator) {
        boolean result = salesForecastService.handleAlert(alertId, action, operator);
        return result ? AjaxResult.success() : AjaxResult.error("处理失败");
    }

    // ========== 补货建议 ==========

    /**
     * 生成补货建议
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/replenishment/{skuId}")
    public AjaxResult generateReplenishmentSuggestion(
            @PathVariable String skuId,
            @RequestParam(required = false) String platformCode) {
        Map<String, Object> result = salesForecastService.generateReplenishmentSuggestion(skuId, platformCode);
        return AjaxResult.success(result);
    }

    /**
     * 批量生成补货建议
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @PostMapping("/replenishment/batch")
    public AjaxResult batchReplenishmentSuggestion(@RequestBody(required = false) List<String> skuIds) {
        List<Map<String, Object>> result = salesForecastService.batchReplenishmentSuggestion(skuIds);
        return AjaxResult.success(result);
    }

    // ========== 统计分析 ==========

    /**
     * 获取销售趋势
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/trend/{skuId}")
    public AjaxResult getSalesTrend(
            @PathVariable String skuId,
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> result = salesForecastService.getSalesTrend(skuId, days);
        return AjaxResult.success(result);
    }

    /**
     * 获取预测准确率
     */
    @PreAuthorize("@ss.hasPermi('yy:forecast:list')")
    @GetMapping("/accuracy")
    public AjaxResult getForecastAccuracy(@RequestParam(defaultValue = "30") int days) {
        Map<String, Object> result = salesForecastService.getForecastAccuracy(days);
        return AjaxResult.success(result);
    }
}
