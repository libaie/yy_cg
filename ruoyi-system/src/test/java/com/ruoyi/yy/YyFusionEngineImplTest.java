package com.ruoyi.yy;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.domain.*;
import com.ruoyi.yy.mapper.YyDrugAliasMapper;
import com.ruoyi.yy.mapper.YyFusionReviewMapper;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.IYyMatchStrategy;
import com.ruoyi.yy.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyFusionEngineImplTest {

    private IYyDrugMasterService drugMasterService;
    private YyDrugAliasMapper aliasMapper;
    private YyFusionReviewMapper reviewMapper;
    private YyFusionEngineImpl engine;

    @BeforeEach
    void setUp() {
        drugMasterService = mock(IYyDrugMasterService.class);
        aliasMapper = mock(YyDrugAliasMapper.class);
        reviewMapper = mock(YyFusionReviewMapper.class);

        YyBarcodeMatchStrategy barcodeStrategy = new YyBarcodeMatchStrategy(drugMasterService);
        YyApprovalNumberMatchStrategy approvalStrategy = new YyApprovalNumberMatchStrategy(drugMasterService);
        YyFuzzyMatchStrategy fuzzyStrategy = new YyFuzzyMatchStrategy(drugMasterService);

        List<IYyMatchStrategy> strategies = new ArrayList<>();
        strategies.add(barcodeStrategy);
        strategies.add(approvalStrategy);
        strategies.add(fuzzyStrategy);

        engine = new YyFusionEngineImpl(strategies, drugMasterService, aliasMapper, reviewMapper);
    }

    @Test
    void fuse_aliasCacheHit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU001");

        YyDrugAlias alias = new YyDrugAlias();
        alias.setDrugId(1L);
        alias.setConfidence(new BigDecimal("1.00"));
        alias.setMatchMethod("barcode");
        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU001")).thenReturn(alias);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);
        drug.setDrugCode("DRUG001");
        when(drugMasterService.selectById(1L)).thenReturn(drug);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(1L, result.getDrugId());
        verify(aliasMapper).selectYyDrugAliasByPlatformSku("ysbang", "SKU001");
        verify(drugMasterService, never()).selectByBarcode(any());
    }

    @Test
    void fuse_barcodeMatch() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU002");
        snapshot.setBarcode("6922710600012");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU002")).thenReturn(null);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterService.selectByBarcode("6922710600012")).thenReturn(drug);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals(FusionConfidence.BARCODE, result.getConfidence());
        assertFalse(result.isNeedsReview());

        verify(aliasMapper).insertYyDrugAlias(any(YyDrugAlias.class));
    }

    @Test
    void fuse_noMatch_goesToReview() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU003");
        snapshot.setBarcode(null);
        snapshot.setApprovalNumber(null);
        snapshot.setCommonName("未知药品");
        snapshot.setSpecification("1g");
        snapshot.setManufacturer("未知厂家");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU003")).thenReturn(null);
        when(drugMasterService.selectCandidates("未知药品", null)).thenReturn(new ArrayList<>());

        YyFusionResult result = engine.fuse(snapshot);

        assertFalse(result.isMatched());

        verify(reviewMapper).insertYyFusionReview(any(YyFusionReview.class));
    }

    @Test
    void fuse_reviewThreshold_markedForReview() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setSourcePlatform("ysbang");
        snapshot.setSkuId("SKU004");
        snapshot.setBarcode(null);
        snapshot.setApprovalNumber(null);
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s");
        snapshot.setManufacturer("北京同仁堂股份有限公司");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU004")).thenReturn(null);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(4L);
        drug.setDrugCode("DRUG004");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s");
        drug.setManufacturer("同仁堂");

        when(drugMasterService.selectCandidates("阿莫西林胶囊", null))
            .thenReturn(Arrays.asList(drug));

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(4L, result.getDrugId());
        assertTrue(result.isNeedsReview());
    }
}
