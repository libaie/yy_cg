package com.ruoyi.web.controller.yy;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.service.IYyStandardProductService;
import com.ruoyi.yy.vo.PriceComparisonVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标准商品 Controller
 */
@RestController
@RequestMapping("/yy/standardProduct")
public class YyStandardProductController extends BaseController {

    @Autowired
    private IYyStandardProductService yyStandardProductService;

    /** 查询标准商品列表 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyStandardProduct yyStandardProduct) {
        startPage();
        List<YyStandardProduct> list = yyStandardProductService.selectYyStandardProductList(yyStandardProduct);
        return getDataTable(list);
    }

    /** 获取标准商品详细信息 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(yyStandardProductService.selectYyStandardProductById(id));
    }

    /** 根据融合分组ID查询该药品在各平台的数据 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/byFusionGroup/{fusionGroupId}")
    public AjaxResult getByFusionGroup(@PathVariable Long fusionGroupId) {
        List<YyStandardProduct> list = yyStandardProductService.selectByFusionGroupId(fusionGroupId);
        return AjaxResult.success(list);
    }

    /** 新增标准商品 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:add')")
    @Log(title = "标准商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyStandardProduct yyStandardProduct) {
        return toAjax(yyStandardProductService.insertYyStandardProduct(yyStandardProduct));
    }

    /** 修改标准商品 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:edit')")
    @Log(title = "标准商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyStandardProduct yyStandardProduct) {
        return toAjax(yyStandardProductService.updateYyStandardProduct(yyStandardProduct));
    }

    /** 删除标准商品 */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:remove')")
    @Log(title = "标准商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(yyStandardProductService.deleteYyStandardProductByIds(ids));
    }

    // ========== 比价功能（增量扩展） ==========

    /**
     * 同品多平台比价
     * @param productId 商品ID或SKU ID
     * @param customerType 客户业态
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/compare/{productId}")
    public AjaxResult comparePrices(
            @PathVariable String productId,
            @RequestParam(defaultValue = "single") String customerType) {
        List<PriceComparisonVO> result = yyStandardProductService.comparePrices(productId, customerType);
        return AjaxResult.success(result);
    }

    /**
     * 批量商品比价
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @PostMapping("/batchCompare")
    public AjaxResult batchComparePrices(
            @RequestBody List<String> productIds,
            @RequestParam(defaultValue = "single") String customerType) {
        Map<String, List<PriceComparisonVO>> result = yyStandardProductService.batchComparePrices(productIds, customerType);
        return AjaxResult.success(result);
    }

    /**
     * 最优平台推荐
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/recommend/{productId}")
    public AjaxResult recommendPlatform(
            @PathVariable String productId,
            @RequestParam(defaultValue = "single") String customerType) {
        Map<String, Object> result = yyStandardProductService.recommendPlatform(productId, customerType);
        return AjaxResult.success(result);
    }

    /**
     * 根据SKU ID查询各平台价格
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/bySku/{skuId}")
    public AjaxResult getBySkuId(
            @PathVariable String skuId,
            @RequestParam(required = false) String sourcePlatform) {
        List<YyStandardProduct> result = yyStandardProductService.selectBySkuId(skuId, sourcePlatform);
        return AjaxResult.success(result);
    }

    /**
     * 根据融合键查询各平台价格
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/byFusionKey/{fusionKey}")
    public AjaxResult getByFusionKey(@PathVariable String fusionKey) {
        List<YyStandardProduct> result = yyStandardProductService.selectByFusionKey(fusionKey);
        return AjaxResult.success(result);
    }

    /**
     * 根据通用名查询各平台价格
     */
    @PreAuthorize("@ss.hasPermi('yy:standardProduct:list')")
    @GetMapping("/byCommonName")
    public AjaxResult getByCommonName(@RequestParam String commonName) {
        List<YyStandardProduct> result = yyStandardProductService.selectByCommonName(commonName);
        return AjaxResult.success(result);
    }
}
