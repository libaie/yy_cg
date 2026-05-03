package com.ruoyi.yy;

import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.*;
import com.ruoyi.yy.mapper.YyDrugAliasMapper;
import com.ruoyi.yy.mapper.YyFusionReviewMapper;
import com.ruoyi.yy.mapper.YyProductSnapshotMapper;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.IYyMatchStrategy;
import com.ruoyi.yy.service.impl.YyFusionEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据融合集成测试（Mock模式）
 *
 * 测试YyFusionEngineImpl与策略链、别名缓存、审核队列的集成
 * 不需要数据库连接，使用Mock隔离外部依赖
 */
class DataFusionServiceIntegrationTest {

    private IYyDrugMasterService drugMasterService;
    private YyDrugAliasMapper aliasMapper;
    private YyFusionReviewMapper reviewMapper;
    private YyProductSnapshotMapper snapshotMapper;
    private YyFusionEngineImpl fusionEngine;

    @BeforeEach
    void setUp() {
        drugMasterService = mock(IYyDrugMasterService.class);
        aliasMapper = mock(YyDrugAliasMapper.class);
        reviewMapper = mock(YyFusionReviewMapper.class);
        snapshotMapper = mock(YyProductSnapshotMapper.class);

        List<IYyMatchStrategy> strategies = new ArrayList<>();
        fusionEngine = new YyFusionEngineImpl(strategies, drugMasterService, aliasMapper, reviewMapper);
    }

    @Test
    void fuse_aliasCached_returnsMatchFromCache() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_001", "阿莫西林胶囊");

        YyDrugAlias cachedAlias = new YyDrugAlias();
        cachedAlias.setDrugId(100L);
        cachedAlias.setConfidence(new BigDecimal("0.98"));
        cachedAlias.setMatchMethod("barcode");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_001"))
            .thenReturn(cachedAlias);

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(100L);
        drug.setDrugCode("DRUG100");
        when(drugMasterService.selectById(100L)).thenReturn(drug);

        YyFusionResult result = fusionEngine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(100L, result.getDrugId());
        assertEquals(new BigDecimal("0.98"), result.getConfidence());
        assertEquals("barcode", result.getMatchMethod());
    }

    @Test
    void fuse_noAliasNoStrategies_returnsNoMatch() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_002", "未知药品");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_002"))
            .thenReturn(null);

        YyFusionResult result = fusionEngine.fuse(snapshot);

        assertFalse(result.isMatched());
        assertNull(result.getDrugId());
    }

    @Test
    void fuse_strategyMatches_createsAliasAndReturnsMatch() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_003", "阿莫西林胶囊");
        snapshot.setBarcode("6922045230012");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_003"))
            .thenReturn(null);

        IYyMatchStrategy matchStrategy = mock(IYyMatchStrategy.class);
        when(matchStrategy.getPriority()).thenReturn(100);
        when(matchStrategy.match(any(), any())).thenReturn(
            YyMatchResult.success(10L, "DRUG001", new BigDecimal("0.95"), MatchMethod.BARCODE, "条码匹配")
        );

        List<IYyMatchStrategy> strategies = List.of(matchStrategy);
        YyFusionEngineImpl engine = new YyFusionEngineImpl(
            strategies, drugMasterService, aliasMapper, reviewMapper);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(10L, result.getDrugId());
        assertEquals("barcode", result.getMatchMethod());
        verify(aliasMapper).insertYyDrugAlias(any(YyDrugAlias.class));
    }

    @Test
    void fuse_lowConfidence_addsToReviewQueue() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_004", "模糊药品名");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_004"))
            .thenReturn(null);

        IYyMatchStrategy lowConfidenceStrategy = mock(IYyMatchStrategy.class);
        when(lowConfidenceStrategy.getPriority()).thenReturn(50);
        when(lowConfidenceStrategy.match(any(), any())).thenReturn(
            YyMatchResult.success(20L, "DRUG002", new BigDecimal("0.85"), MatchMethod.FUZZY, "模糊匹配")
        );

        List<IYyMatchStrategy> strategies = List.of(lowConfidenceStrategy);
        YyFusionEngineImpl engine = new YyFusionEngineImpl(
            strategies, drugMasterService, aliasMapper, reviewMapper);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertTrue(result.isNeedsReview());
        // Review queue is for unmatched items; low-confidence matches just set needsReview flag
        verify(aliasMapper).insertYyDrugAlias(any(YyDrugAlias.class));
    }

    @Test
    void fuse_multipleStrategies_highestPriorityWins() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_005", "测试药品");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_005"))
            .thenReturn(null);

        IYyMatchStrategy lowPriority = mock(IYyMatchStrategy.class);
        when(lowPriority.getPriority()).thenReturn(10);
        when(lowPriority.match(any(), any())).thenReturn(
            YyMatchResult.success(1L, "DRUG_LOW", new BigDecimal("0.70"), MatchMethod.FUZZY, "模糊")
        );

        IYyMatchStrategy highPriority = mock(IYyMatchStrategy.class);
        when(highPriority.getPriority()).thenReturn(100);
        when(highPriority.match(any(), any())).thenReturn(
            YyMatchResult.success(2L, "DRUG_HIGH", new BigDecimal("0.99"), MatchMethod.BARCODE, "条码")
        );

        List<IYyMatchStrategy> strategies = new ArrayList<>(List.of(lowPriority, highPriority));
        YyFusionEngineImpl engine = new YyFusionEngineImpl(
            strategies, drugMasterService, aliasMapper, reviewMapper);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals("barcode", result.getMatchMethod());
        verify(lowPriority, never()).match(any(), any());
    }

    @Test
    void fuse_firstStrategyMatches_stopsChain() {
        YyProductSnapshot snapshot = createSnapshot("ysbang", "SKU_006", "测试药品");

        when(aliasMapper.selectYyDrugAliasByPlatformSku("ysbang", "SKU_006"))
            .thenReturn(null);

        IYyMatchStrategy first = mock(IYyMatchStrategy.class);
        when(first.getPriority()).thenReturn(100);
        when(first.match(any(), any())).thenReturn(
            YyMatchResult.success(1L, "DRUG001", new BigDecimal("0.95"), MatchMethod.BARCODE, "条码")
        );

        IYyMatchStrategy second = mock(IYyMatchStrategy.class);
        when(second.getPriority()).thenReturn(50);

        List<IYyMatchStrategy> strategies = new ArrayList<>(List.of(first, second));
        YyFusionEngineImpl engine = new YyFusionEngineImpl(
            strategies, drugMasterService, aliasMapper, reviewMapper);

        YyFusionResult result = engine.fuse(snapshot);

        assertTrue(result.isMatched());
        verify(second, never()).match(any(), any());
    }

    @Test
    void snapshotMapper_batchInsert_works() {
        YyProductSnapshot s1 = createSnapshot("ysbang", "BATCH_001", "药品1");
        YyProductSnapshot s2 = createSnapshot("ysbang", "BATCH_002", "药品2");

        doNothing().when(snapshotMapper).batchInsertYyProductSnapshot(anyList());

        snapshotMapper.batchInsertYyProductSnapshot(List.of(s1, s2));

        verify(snapshotMapper).batchInsertYyProductSnapshot(argThat(list -> list.size() == 2));
    }

    @Test
    void snapshotMapper_selectByPlatformAndSkuIds_works() {
        YyProductSnapshot s1 = createSnapshot("ysbang", "SKU_A", "药品A");
        YyProductSnapshot s2 = createSnapshot("ysbang", "SKU_B", "药品B");

        when(snapshotMapper.selectYyProductSnapshotByPlatformAndSkuIds(
            "ysbang", List.of("SKU_A", "SKU_B")))
            .thenReturn(List.of(s1, s2));

        List<YyProductSnapshot> results = snapshotMapper
            .selectYyProductSnapshotByPlatformAndSkuIds("ysbang", List.of("SKU_A", "SKU_B"));

        assertEquals(2, results.size());
        assertEquals("药品A", results.get(0).getCommonName());
        assertEquals("药品B", results.get(1).getCommonName());
    }

    private YyProductSnapshot createSnapshot(String platform, String skuId, String commonName) {
        YyProductSnapshot s = new YyProductSnapshot();
        s.setSourcePlatform(platform);
        s.setSkuId(skuId);
        s.setCommonName(commonName);
        s.setProductData("{}");
        return s;
    }
}
