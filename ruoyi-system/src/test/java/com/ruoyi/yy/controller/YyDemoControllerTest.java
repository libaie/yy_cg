package com.ruoyi.yy.controller;

import com.ruoyi.yy.YyDemoDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * YyDemoController 集成测试
 * 验证演示模式端点无需认证即可访问
 *
 * @author ruoyi
 */
@SpringBootTest(
    classes = YyDemoControllerTest.TestConfig.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
    }
)
@AutoConfigureMockMvc(addFilters = false)
class YyDemoControllerTest {

    @Configuration
    @ComponentScan(
        basePackages = "com.ruoyi.yy",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {YyDemoDataLoader.class, YyDemoController.class})
    )
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test 1: 无需认证即可访问全部产品，返回200且数据非空
     */
    @Test
    void getProducts_withoutAuth_returns200WithData() throws Exception {
        mockMvc.perform(get("/yy/demo/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data[0].productName").exists())
                .andExpect(jsonPath("$.data[0].prices").isArray())
                .andExpect(jsonPath("$.data[0].prices.length()").value(6));
    }

    /**
     * Test 2: 搜索不存在药品返回空数组
     */
    @Test
    void search_nonExistentKeyword_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/yy/demo/search").param("keyword", "不存在的药XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * Test 3: 搜索真实药品应返回匹配结果
     */
    @Test
    void search_validKeyword_returnsMatchingResults() throws Exception {
        mockMvc.perform(get("/yy/demo/search").param("keyword", "阿莫西林"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data[0].productName").value(containsString("阿莫西林")));
    }
}
