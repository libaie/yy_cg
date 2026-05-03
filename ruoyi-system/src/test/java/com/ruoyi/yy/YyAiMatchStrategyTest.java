package com.ruoyi.yy;

import com.ruoyi.yy.service.impl.YyAiMatchStrategy;
import com.ruoyi.yy.service.impl.YyMockAiGateway;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YyAiMatchStrategyTest {

    @Test
    void name() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());
        assertEquals("AiMatch", strategy.getName());
    }

    @Test
    void priority() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());
        assertEquals(10, strategy.getPriority());
    }

    @Test
    void match_success() {
        YyMockAiGateway gateway = new YyMockAiGateway(
            "{\"matched\": true, \"drug_id\": \"5\", \"confidence\": 0.92, \"reason\": \"Same drug\"}"
        );
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");
        snapshot.setManufacturer("联邦制药");
        snapshot.setSpecification("0.25g*12s");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(5L);
        drug.setDrugCode("DRUG005");
        drug.setCommonName("阿莫西林胶囊");

        List<YyDrugMaster> candidates = Arrays.asList(drug);
        YyMatchResult result = strategy.match(snapshot, candidates);

        assertTrue(result.isMatched());
        assertEquals(5L, result.getDrugId());
        assertEquals(MatchMethod.AI, result.getMatchMethod());
    }

    @Test
    void match_noMatch() {
        YyMockAiGateway gateway = new YyMockAiGateway(
            "{\"matched\": false, \"confidence\": 0.3, \"reason\": \"Different drugs\"}"
        );
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("未知药品");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);
        drug.setCommonName("其他药品");

        YyMatchResult result = strategy.match(snapshot, Arrays.asList(drug));

        assertFalse(result.isMatched());
    }

    @Test
    void match_gatewayFailure() {
        YyMockAiGateway gateway = new YyMockAiGateway();
        gateway.setShouldFail(true);
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);

        YyMatchResult result = strategy.match(snapshot, Arrays.asList(drug));

        assertFalse(result.isMatched());
    }

    @Test
    void match_noCandidates() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyMatchResult result = strategy.match(snapshot, Arrays.asList());

        assertFalse(result.isMatched());
    }

    @Test
    void match_nullSnapshot() {
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(new YyMockAiGateway());
        YyMatchResult result = strategy.match(null, Arrays.asList());
        assertFalse(result.isMatched());
    }

    @Test
    void match_invalidJsonResponse() {
        YyMockAiGateway gateway = new YyMockAiGateway("not valid json");
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);

        YyMatchResult result = strategy.match(snapshot, Arrays.asList(drug));
        assertFalse(result.isMatched());
    }

    @Test
    void match_drugIdNotInCandidates() {
        YyMockAiGateway gateway = new YyMockAiGateway(
            "{\"matched\": true, \"drug_id\": \"999\", \"confidence\": 0.9, \"reason\": \"test\"}"
        );
        YyAiMatchStrategy strategy = new YyAiMatchStrategy(gateway);

        YyProductSnapshot snapshot = new YyProductSnapshot();
        snapshot.setCommonName("阿莫西林胶囊");

        YyDrugMaster drug = new YyDrugMaster();
        drug.setId(1L);

        YyMatchResult result = strategy.match(snapshot, Arrays.asList(drug));
        assertFalse(result.isMatched());
    }
}
