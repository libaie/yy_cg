package com.ruoyi.yy;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyFieldMappingMapper;
import com.ruoyi.yy.mapper.YyPlatformMapper;
import com.ruoyi.yy.service.impl.YyConfigurablePlatformAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyConfigurablePlatformAdapterTest {

    private YyFieldMappingMapper fieldMappingMapper;
    private YyPlatformMapper platformMapper;
    private YyConfigurablePlatformAdapter adapter;

    @BeforeEach
    void setUp() {
        fieldMappingMapper = mock(YyFieldMappingMapper.class);
        platformMapper = mock(YyPlatformMapper.class);
        adapter = new YyConfigurablePlatformAdapter();
        setField(adapter, "fieldMappingMapper", fieldMappingMapper);
        setField(adapter, "platformMapper", platformMapper);
    }

    @Test
    void getPlatformCode_returnsWildcard() {
        assertEquals("*", adapter.getPlatformCode());
    }

    @Test
    void decrypt_noEncryption_returnsInput() {
        String result = adapter.decrypt("plaintext", "key", 0);
        assertEquals("plaintext", result);
    }

    @Test
    void decrypt_aesEncryption_decryptsCorrectly() {
        String key = "1234567890abcdef";
        String plaintext = "hello world";
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key.getBytes(), "AES"));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            String encoded = java.util.Base64.getEncoder().encodeToString(encrypted);

            String result = adapter.decrypt(encoded, key, 1);
            assertEquals(plaintext, result);
        } catch (Exception e) {
            fail("AES test setup failed: " + e.getMessage());
        }
    }

    @Test
    void decrypt_aesFailure_throwsException() {
        assertThrows(RuntimeException.class, () -> {
            adapter.decrypt("invalid-base64", "key", 1);
        });
    }

    @Test
    void extractProductArray_withEntryPath() {
        String json = "{\"data\":{\"items\":[{\"id\":1},{\"id\":2}]}}";
        JSONArray result = adapter.extractProductArray(json, "data.items");
        assertEquals(2, result.size());
    }

    @Test
    void extractProductArray_withoutEntryPath() {
        String json = "[{\"id\":1},{\"id\":2}]";
        JSONArray result = adapter.extractProductArray(json, null);
        assertEquals(2, result.size());
    }

    @Test
    void normalizeProduct_appliesFieldMappings() {
        YyPlatform platform = new YyPlatform();
        platform.setPId(1L);
        when(platformMapper.selectYyPlatformByCode("test")).thenReturn(platform);

        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(Map.of("standard_field", "common_name", "platform_field", "goods_name"));
        mappings.add(Map.of("standard_field", "price_current", "platform_field", "sale_price"));
        when(fieldMappingMapper.selectMappingsByPlatformId(1L)).thenReturn(mappings);

        JSONObject rawItem = new JSONObject();
        rawItem.put("goods_name", "阿莫西林胶囊");
        rawItem.put("sale_price", "25.50");

        YyProductSnapshot snapshot = adapter.normalizeProduct(rawItem, "test", "search");

        assertEquals("阿莫西林胶囊", snapshot.getCommonName());
        assertEquals(new java.math.BigDecimal("25.50"), snapshot.getPriceCurrent());
        assertEquals("test", snapshot.getSourcePlatform());
    }

    @Test
    void normalizeProduct_platformNotFound() {
        when(platformMapper.selectYyPlatformByCode("unknown")).thenReturn(null);

        JSONObject rawItem = new JSONObject();
        rawItem.put("goods_name", "阿莫西林胶囊");

        YyProductSnapshot snapshot = adapter.normalizeProduct(rawItem, "unknown", "search");

        assertEquals("unknown", snapshot.getSourcePlatform());
        assertNull(snapshot.getCommonName());
    }

    @Test
    void normalizeProduct_missingField_skipped() {
        YyPlatform platform = new YyPlatform();
        platform.setPId(1L);
        when(platformMapper.selectYyPlatformByCode("test")).thenReturn(platform);

        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(Map.of("standard_field", "common_name", "platform_field", "goods_name"));
        when(fieldMappingMapper.selectMappingsByPlatformId(1L)).thenReturn(mappings);

        JSONObject rawItem = new JSONObject();
        // goods_name not present

        YyProductSnapshot snapshot = adapter.normalizeProduct(rawItem, "test", "search");
        assertNull(snapshot.getCommonName());
    }

    @Test
    void normalizeProduct_invalidPrice_logged() {
        YyPlatform platform = new YyPlatform();
        platform.setPId(1L);
        when(platformMapper.selectYyPlatformByCode("test")).thenReturn(platform);

        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(Map.of("standard_field", "price_current", "platform_field", "sale_price"));
        when(fieldMappingMapper.selectMappingsByPlatformId(1L)).thenReturn(mappings);

        JSONObject rawItem = new JSONObject();
        rawItem.put("sale_price", "not_a_number");

        YyProductSnapshot snapshot = adapter.normalizeProduct(rawItem, "test", "search");
        assertNull(snapshot.getPriceCurrent());
    }

    @Test
    void buildSearchKeywords_removesBrandName() {
        List<String> keywords = adapter.buildSearchKeywords("阿莫西林胶囊（华北制药）");
        assertTrue(keywords.size() >= 2);
        assertEquals("阿莫西林胶囊（华北制药）", keywords.get(0));
        assertEquals("阿莫西林胶囊", keywords.get(1));
    }

    @Test
    void buildSearchKeywords_nullInput_returnsEmpty() {
        List<String> keywords = adapter.buildSearchKeywords(null);
        assertTrue(keywords.isEmpty());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
