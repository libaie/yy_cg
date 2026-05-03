package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyCleanResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.service.impl.YyAiDataCleanerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyAiDataCleanerImplTest {

    private IYyAiGateway gateway;
    private YyProductSnapshotMapper snapshotMapper;
    private YyAiDataCleanerImpl cleaner;

    @BeforeEach
    void setUp() {
        gateway = mock(IYyAiGateway.class);
        snapshotMapper = mock(YyProductSnapshotMapper.class);
        cleaner = new YyAiDataCleanerImpl();
        ReflectionTestUtils.setField(cleaner, "aiGateway", gateway);
        ReflectionTestUtils.setField(cleaner, "snapshotMapper", snapshotMapper);
    }

    @Test
    void clean_emptyList_returnsEmptyResult() {
        YyCleanResult result = cleaner.cleanProductData(Collections.emptyList());
        assertNotNull(result);
        assertEquals(0, result.getTotalProcessed());
        assertEquals(0, result.getNeedsReview());
    }

    @Test
    void clean_validResponse_parsesSuggestions() {
        YyProductSnapshot s = new YyProductSnapshot();
        s.setId(1L);
        s.setCommonName("阿莫西林胶囊");
        s.setManufacturer("华北制药");
        s.setSpecification("0.25g*24粒");

        String aiResponse = "[{\"snapshotId\":1,\"field\":\"manufacturer\"," +
            "\"original\":\"华北制药\",\"suggested\":\"华北制药股份有限公司\"," +
            "\"confidence\":0.95,\"reason\":\"厂家名不完整\"}]";

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok(aiResponse, "qwen-turbo", 100, 50, 200)
        );

        YyCleanResult result = cleaner.cleanProductData(List.of(s));

        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
        assertEquals(1, result.getNeedsReview());
        assertNotNull(result.getSuggestions());
        assertEquals(1, result.getSuggestions().size());
        assertEquals("manufacturer", result.getSuggestions().get(0).get("field"));
        assertEquals("华北制药股份有限公司", result.getSuggestions().get(0).get("suggested"));
    }

    @Test
    void clean_gatewayFailure_returnsEmptySuggestions() {
        YyProductSnapshot s = new YyProductSnapshot();
        s.setId(1L);
        s.setCommonName("阿莫西林胶囊");
        s.setManufacturer("华北制药");

        when(gateway.call(any(YyAiRequest.class))).thenReturn(YyAiResponse.fail("timeout"));

        YyCleanResult result = cleaner.cleanProductData(List.of(s));

        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
        assertEquals(0, result.getNeedsReview());
        assertNotNull(result.getSuggestions());
        assertTrue(result.getSuggestions().isEmpty());
    }

    @Test
    void clean_invalidJson_returnsEmptySuggestions() {
        YyProductSnapshot s = new YyProductSnapshot();
        s.setId(1L);
        s.setCommonName("阿莫西林胶囊");

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("not valid json", "qwen-turbo", 100, 50, 200)
        );

        YyCleanResult result = cleaner.cleanProductData(List.of(s));

        assertNotNull(result);
        assertTrue(result.getSuggestions().isEmpty());
    }

    @Test
    void clean_multipleItems_batches20() {
        List<YyProductSnapshot> snapshots = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            YyProductSnapshot s = new YyProductSnapshot();
            s.setId((long) i);
            s.setCommonName("药品" + i);
            snapshots.add(s);
        }

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("[]", "qwen-turbo", 100, 50, 200)
        );

        YyCleanResult result = cleaner.cleanProductData(snapshots);

        assertEquals(25, result.getTotalProcessed());
        verify(gateway, times(2)).call(any(YyAiRequest.class));
    }

    @Test
    void clean_nullFields_handledGracefully() {
        YyProductSnapshot s = new YyProductSnapshot();
        s.setId(1L);
        s.setCommonName(null);
        s.setManufacturer(null);
        s.setSpecification(null);

        when(gateway.call(any(YyAiRequest.class))).thenReturn(
            YyAiResponse.ok("[]", "qwen-turbo", 100, 50, 200)
        );

        YyCleanResult result = cleaner.cleanProductData(List.of(s));

        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
    }
}
