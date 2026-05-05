package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyFieldMappingRule;
import com.ruoyi.yy.mapper.YyFieldMappingRuleMapper;
import com.ruoyi.yy.model.MappingResult;
import com.ruoyi.yy.service.impl.YyFieldMappingEngine;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class YyFieldMappingEngineTest {

    @Mock YyFieldMappingRuleMapper ruleMapper;
    @InjectMocks YyFieldMappingEngine engine;

    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }

    // Test 1: basic path extraction
    @Test void execute_basicPathExtraction() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("product_name", "[\"data.goodsName\",\"data.title\"]", "none", null, 0, null, null)
        ));
        JSONObject input = new JSONObject().fluentPut("data",
            new JSONObject().fluentPut("goodsName", "阿莫西林胶囊"));
        MappingResult result = engine.execute(input, 1L, "search");
        assertFalse(result.hasRequiredFieldFailures());
        assertEquals("阿莫西林胶囊", result.getFields().get("product_name"));
    }

    // Test 2: number transform with Chinese suffix stripping
    @Test void execute_numberTransform_withChineseSuffix() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("price_current", "[\"data.price\"]", "number", "{\"scale\":2,\"strip\":\"¥$￥元,，\"}", 0, null, null)
        ));
        JSONObject input = new JSONObject().fluentPut("data",
            new JSONObject().fluentPut("price", "￥13.50元"));
        MappingResult result = engine.execute(input, 1L, "search");
        assertEquals(new java.math.BigDecimal("13.50"), result.getFields().get("price_current"));
    }

    // Test 3: boolean transform (all true values)
    @Test void execute_booleanTransform_trueValues() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("is_prescription_drug", "[\"data.rxFlag\"]", "boolean", null, 0, null, null)
        ));
        JSONObject input = new JSONObject().fluentPut("data", new JSONObject().fluentPut("rxFlag", "是"));
        MappingResult result = engine.execute(input, 1L, "detail");
        assertEquals(1, result.getFields().get("is_prescription_drug"));
    }

    // Test 4: missing required field
    @Test void execute_missingRequiredField_returnsFailure() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("common_name", "[\"data.nonExistent\"]", "none", null, 1, null, null)
        ));
        JSONObject input = new JSONObject().fluentPut("data", new JSONObject());
        MappingResult result = engine.execute(input, 1L, "search");
        assertTrue(result.hasRequiredFieldFailures());
        assertTrue(result.getRequiredFieldFailures().contains("common_name"));
    }

    // Test 5: validation range violation
    @Test void execute_validationRange_violation() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("stock_quantity", "[\"data.stock\"]", "number", "{\"scale\":0}", 0, "{\"min\":0,\"max\":99999}", null)
        ));
        JSONObject input = new JSONObject().fluentPut("data", new JSONObject().fluentPut("stock", -5));
        MappingResult result = engine.execute(input, 1L, "search");
        assertFalse(result.getValidationErrors().isEmpty());
    }

    // Test 6: executeBatch with multiple items
    @Test void executeBatch_multipleItems() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("common_name", "[\"name\"]", "none", null, 0, null, null)
        ));
        JSONArray items = new JSONArray()
            .fluentAdd(new JSONObject().fluentPut("name", "A"))
            .fluentAdd(new JSONObject().fluentPut("name", "B"));
        java.util.List<MappingResult> results = engine.executeBatch(
            items.toJavaList(Object.class), 1L, "search");
        assertEquals(2, results.size());
        assertEquals("A", results.get(0).getFields().get("common_name"));
    }

    // Test 7: value mapping
    @Test void execute_valueMapping_lookupReplacesValue() {
        when(ruleMapper.selectByPlatformAndApi(any(), any())).thenReturn(java.util.List.of(
            rule("product_status", "[\"data.status\"]", "none", null, 0, null, "{\"0\":\"下架\",\"1\":\"上架\"}")
        ));
        JSONObject input = new JSONObject().fluentPut("data", new JSONObject().fluentPut("status", "1"));
        MappingResult result = engine.execute(input, 1L, "search");
        assertEquals("上架", result.getFields().get("product_status"));
    }

    // Helper
    private YyFieldMappingRule rule(String standardField, String sourcePaths,
        String transformType, String transformConfig, int isRequired,
        String validation, String valueMap) {
        YyFieldMappingRule r = new YyFieldMappingRule();
        r.setStandardField(standardField);
        r.setSourcePaths(sourcePaths);
        r.setTransformType(transformType);
        r.setTransformConfig(transformConfig);
        r.setIsRequired(isRequired);
        r.setValidation(validation);
        r.setValueMap(valueMap);
        r.setSortOrder(0);
        r.setIsEnabled(1);
        return r;
    }
}
