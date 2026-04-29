package com.ruoyi.web.controller.yy;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.dto.PriceComparisonDTO;
import com.ruoyi.yy.service.IPriceComparisonService;
import com.ruoyi.yy.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多平台比价 Controller
 */
@RestController
@RequestMapping("/yy/priceComparison")
public class YyPriceComparisonController extends BaseController {
    
    @Autowired
    private IPriceComparisonService priceComparisonService;
    
    // ========== 商品比价 ==========
    
    /**
     * 同品多平台比价
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @GetMapping("/compare/{skuId}")
    public AjaxResult comparePrices(
            @PathVariable String skuId,
            @RequestParam(defaultValue = "single") String customerType) {
        List<PriceComparisonVO> result = priceComparisonService.comparePrices(skuId, customerType);
        return AjaxResult.success(result);
    }
    
    /**
     * 批量商品比价
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @PostMapping("/batchCompare")
    public AjaxResult batchComparePrices(
            @RequestBody List<String> skuIds,
            @RequestParam(defaultValue = "single") String customerType) {
        Map<String, List<PriceComparisonVO>> result = priceComparisonService.batchComparePrices(skuIds, customerType);
        return AjaxResult.success(result);
    }
    
    /**
     * 按条件比价
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @PostMapping("/compareByCondition")
    public AjaxResult comparePricesByCondition(@RequestBody PriceComparisonDTO dto) {
        List<PriceComparisonVO> result = priceComparisonService.comparePricesByCondition(dto);
        return AjaxResult.success(result);
    }
    
    // ========== 推荐与优化 ==========
    
    /**
     * 最优平台推荐
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @GetMapping("/recommend/{skuId}")
    public AjaxResult recommendPlatform(
            @PathVariable String skuId,
            @RequestParam(defaultValue = "single") String customerType,
            @RequestParam(required = false) String regionCode) {
        PlatformRecommendationVO result = priceComparisonService.recommendPlatform(skuId, customerType, regionCode);
        return AjaxResult.success(result);
    }
    
    /**
     * 生成采购组合方案
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @PostMapping("/purchasePlan")
    public AjaxResult generatePurchasePlan(
            @RequestBody List<String> skuIds,
            @RequestParam(defaultValue = "single") String customerType) {
        PurchasePlanVO result = priceComparisonService.generatePurchasePlan(skuIds, customerType, null);
        return AjaxResult.success(result);
    }
    
    // ========== 数据查询 ==========
    
    /**
     * 获取商品历史价格趋势
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @GetMapping("/trend/{skuId}")
    public AjaxResult getPriceTrend(
            @PathVariable String skuId,
            @RequestParam String platformCode,
            @RequestParam(defaultValue = "30") int days) {
        List<PriceTrendVO> result = priceComparisonService.getPriceTrend(skuId, platformCode, days);
        return AjaxResult.success(result);
    }
    
    /**
     * 获取平台活动列表
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @GetMapping("/activities")
    public AjaxResult getPlatformActivities(
            @RequestParam(required = false) String platformCode,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<PlatformActivityVO> result = priceComparisonService.getPlatformActivities(platformCode, activeOnly);
        return AjaxResult.success(result);
    }
    
    /**
     * 获取比价统计
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:list')")
    @GetMapping("/stats")
    public AjaxResult getComparisonStats(
            @RequestParam(defaultValue = "single") String customerType) {
        PriceComparisonStatsVO result = priceComparisonService.getComparisonStats(customerType);
        return AjaxResult.success(result);
    }
    
    // ========== 数据管理 ==========
    
    /**
     * 导入比价数据
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:add')")
    @Log(title = "比价数据", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importPriceComparisons(@RequestBody List<PriceComparisonVO> comparisonList) {
        Map<String, Object> result = priceComparisonService.importPriceComparisons(comparisonList);
        return AjaxResult.success(result);
    }
    
    /**
     * 采集平台价格数据
     */
    @PreAuthorize("@ss.hasPermi('yy:priceComparison:add')")
    @Log(title = "价格采集", businessType = BusinessType.OTHER)
    @PostMapping("/collect")
    public AjaxResult collectPriceData(
            @RequestParam String platformCode,
            @RequestBody(required = false) List<String> skuIds) {
        Map<String, Object> result = priceComparisonService.collectPriceData(platformCode, skuIds);
        return AjaxResult.success(result);
    }
}
