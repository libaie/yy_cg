package com.ruoyi.yy;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.impl.YyFuzzyMatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyFuzzyMatchStrategyTest {

    private IYyDrugMasterService drugMasterService;
    private YyFuzzyMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterService = mock(IYyDrugMasterService.class);
        strategy = new YyFuzzyMatchStrategy(drugMasterService);
    }

    @Test
    void name() {
        assertEquals("FuzzyMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(50, strategy.getPriority());
    }

    @Test
    void exactMatch_highConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s*2b");
        snapshot.setManufacturer("联邦制药");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(3L);
        drug.setDrugCode("DRUG003");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s*2b");
        drug.setManufacturer("联邦制药");

        when(drugMasterService.selectCandidates("阿莫西林胶囊", "0.25g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(3L, result.getDrugId());
        assertEquals(MatchMethod.FUZZY, result.getMatchMethod());
        assertTrue(result.getConfidence().compareTo(FusionConfidence.FUZZY_MIN) >= 0);
    }

    @Test
    void similarManufacturer_highConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.25g*12s");
        snapshot.setManufacturer("北京同仁堂股份有限公司");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(4L);
        drug.setDrugCode("DRUG004");
        drug.setCommonName("阿莫西林胶囊");
        drug.setSpecification("0.25g*12s");
        drug.setManufacturer("同仁堂");

        when(drugMasterService.selectCandidates("阿莫西林胶囊", "0.25g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertTrue(result.getConfidence().compareTo(new BigDecimal("0.80")) >= 0);
    }

    @Test
    void noCandidates_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("未知药品");
        snapshot.setSpecification("1g");
        snapshot.setManufacturer("未知厂家");

        when(drugMasterService.selectCandidates("未知药品", "1g"))
            .thenReturn(new ArrayList<>());
        when(drugMasterService.selectCandidatesFallback("未知药品", "1g"))
            .thenReturn(new ArrayList<>());

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void differentDrug_lowConfidence() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setSpecification("0.5g*24s");
        snapshot.setManufacturer("联邦制药");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(5L);
        drug.setDrugCode("DRUG005");
        drug.setCommonName("布洛芬缓释胶囊");
        drug.setSpecification("0.3g*20s");
        drug.setManufacturer("中美史克");

        when(drugMasterService.selectCandidates("阿莫西林胶囊", "0.5g"))
            .thenReturn(Arrays.asList(drug));

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void normalizeManufacturer_removesSuffixes() {
        assertEquals("北京同仁堂", strategy.normalizeManufacturer("北京同仁堂股份有限公司"));
        assertEquals("联邦", strategy.normalizeManufacturer("联邦制药集团有限公司"));
        assertEquals("华润三九", strategy.normalizeManufacturer("华润三九医药股份有限公司"));
        assertEquals("Pfizer", strategy.normalizeManufacturer("Pfizer Inc."));
    }

    @Test
    void normalizeSpecification_standardizes() {
        assertEquals("0.25g*12s", strategy.normalizeSpecification("0.25g×12片"));
        assertEquals("0.25g*12s", strategy.normalizeSpecification("0.25gX12片"));
        assertEquals("10ml*6b", strategy.normalizeSpecification("10ml×6支"));
    }

    @Test
    void nullSnapshot_miss() {
        YyMatchResult result = strategy.match(null, new ArrayList<>());
        assertFalse(result.isMatched());
    }
}
