package com.ruoyi.yy;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.impl.YyBarcodeMatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyBarcodeMatchStrategyTest {

    private IYyDrugMasterService drugMasterService;
    private YyBarcodeMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterService = mock(IYyDrugMasterService.class);
        strategy = new YyBarcodeMatchStrategy(drugMasterService);
    }

    @Test
    void name() {
        assertEquals("BarcodeMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(100, strategy.getPriority());
    }

    @Test
    void matchWithBarcode_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("6922710600012");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);
        drug.setDrugCode("DRUG001");
        when(drugMasterService.selectByBarcode("6922710600012")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(1L, result.getDrugId());
        assertEquals(FusionConfidence.BARCODE, result.getConfidence());
        assertEquals(MatchMethod.BARCODE, result.getMatchMethod());
    }

    @Test
    void matchWithBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("6922710600012");
        when(drugMasterService.selectByBarcode("6922710600012")).thenReturn(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void matchWithNullBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
        verify(drugMasterService, never()).selectByBarcode(any());
    }

    @Test
    void matchWithEmptyBarcode_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setBarcode("");

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }
}
