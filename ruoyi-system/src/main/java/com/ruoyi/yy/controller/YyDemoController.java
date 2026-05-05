package com.ruoyi.yy.controller;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.YyDemoDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo演示模式Controller
 * 无需登录即可浏览50种常见药品的多平台比价数据
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/yy/demo")
public class YyDemoController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(YyDemoController.class);

    @Autowired
    private YyDemoDataLoader demoDataLoader;

    /**
     * 搜索药品
     * 根据关键词模糊匹配 productName/commonName/manufacturer
     *
     * @param keyword 搜索关键词（可选，为空时返回全部数据）
     * @return 匹配的药品列表
     */
    @Anonymous
    @GetMapping("/search")
    public AjaxResult search(@RequestParam(defaultValue = "") String keyword) {
        JSONArray products = demoDataLoader.search(keyword);
        log.info("Demo搜索: keyword=\"{}\", 命中 {} 条", keyword, products.size());
        return AjaxResult.success(products);
    }

    /**
     * 获取全部Demo药品
     *
     * @return 全部50种药品及其多平台价格
     */
    @Anonymous
    @GetMapping("/products")
    public AjaxResult products() {
        JSONArray allProducts = demoDataLoader.getAllProducts();
        log.info("Demo全部产品查询: 返回 {} 条", allProducts.size());
        return AjaxResult.success(allProducts);
    }
}
