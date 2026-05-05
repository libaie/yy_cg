package com.ruoyi.yy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Demo数据加载器
 * 从 classpath:demo/demo-products.json 加载50种常见药品数据到内存
 * 无需认证，无需数据库，供演示模式使用
 *
 * @author ruoyi
 */
@Component
public class YyDemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(YyDemoDataLoader.class);

    private static final String DEMO_DATA_PATH = "demo/demo-products.json";

    private JSONObject data;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(DEMO_DATA_PATH);
            try (InputStream is = resource.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                data = JSON.parseObject(content);
                JSONArray products = data.getJSONArray("products");
                int count = products != null ? products.size() : 0;
                log.info("Demo数据加载成功: {} 条药品, 更新时间={}", count, data.getString("updatedAt"));
            }
        } catch (Exception e) {
            log.warn("Demo数据文件未找到或解析失败, 以空数据集运行: {}", e.getMessage());
            data = new JSONObject();
            data.put("products", new JSONArray());
            data.put("updatedAt", "");
        }
    }

    /**
     * 根据关键词搜索药品
     * 支持按 productName / commonName / manufacturer 进行大小写不敏感的模糊匹配
     *
     * @param keyword 搜索关键词
     * @return 匹配的药品列表
     */
    public JSONArray search(String keyword) {
        JSONArray allProducts = getAllProducts();
        if (keyword == null || keyword.trim().isEmpty()) {
            return allProducts;
        }

        String kw = keyword.trim().toLowerCase();
        JSONArray result = new JSONArray();

        for (int i = 0; i < allProducts.size(); i++) {
            JSONObject product = allProducts.getJSONObject(i);
            String productName = product.getString("productName");
            String commonName = product.getString("commonName");
            String manufacturer = product.getString("manufacturer");

            if ((productName != null && productName.toLowerCase().contains(kw))
                    || (commonName != null && commonName.toLowerCase().contains(kw))
                    || (manufacturer != null && manufacturer.toLowerCase().contains(kw))) {
                result.add(product);
            }
        }

        return result;
    }

    /**
     * 获取所有产品
     *
     * @return 全部药品列表
     */
    public JSONArray getAllProducts() {
        JSONArray products = data.getJSONArray("products");
        return products != null ? products : new JSONArray();
    }

    /**
     * 获取数据更新时间
     *
     * @return 更新时间字符串
     */
    public String getUpdatedAt() {
        return data.getString("updatedAt");
    }
}
