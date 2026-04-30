package com.ruoyi.yy;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.impl.YyApprovalNumberMatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyApprovalNumberMatchStrategyTest {

    private IYyDrugMasterService drugMasterService;
    private YyApprovalNumberMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        drugMasterService = mock(IYyDrugMasterService.class);
        strategy = new YyApprovalNumberMatchStrategy(drugMasterService);
    }

    @Test
    void name() {
        assertEquals("ApprovalNumberMatch", strategy.getName());
    }

    @Test
    void priority() {
        assertEquals(90, strategy.getPriority());
    }

    @Test
    void match_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("国药准字Z11020001");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterService.selectByApprovalNumber("Z11020001")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals(FusionConfidence.APPROVAL_NUMBER, result.getConfidence());
        assertEquals(MatchMethod.APPROVAL, result.getMatchMethod());
    }

    @Test
    void matchWithoutPrefix_hit() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("Z11020001");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(2L);
        drug.setDrugCode("DRUG002");
        when(drugMasterService.selectByApprovalNumber("Z11020001")).thenReturn(drug);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertTrue(result.isMatched());
        assertEquals(2L, result.getDrugId());
        assertEquals(FusionConfidence.APPROVAL_NUMBER, result.getConfidence());
        assertEquals(MatchMethod.APPROVAL, result.getMatchMethod());
    }

    @Test
    void match_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber("国药准字Z99999999");
        when(drugMasterService.selectByApprovalNumber("Z99999999")).thenReturn(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }

    @Test
    void matchNull_miss() {
        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setApprovalNumber(null);

        YyMatchResult result = strategy.match(snapshot, new ArrayList<>());

        assertFalse(result.isMatched());
    }
}
