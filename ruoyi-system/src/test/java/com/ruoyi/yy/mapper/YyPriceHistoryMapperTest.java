package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPriceHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 价格历史 Mapper 测试
 */
@SpringBootTest
@Transactional
class YyPriceHistoryMapperTest {

    @Autowired
    private YyPriceHistoryMapper mapper;

    @Test
    void testInsertAndQuery() {
        YyPriceHistory h = new YyPriceHistory();
        h.setSourcePlatform("ysbang");
        h.setSkuId("SKU-TEST-001");
        h.setProductName("阿莫西林胶囊");
        h.setSpecification("0.5g×30粒");
        h.setManufacturer("华北制药");
        h.setPriceCurrent(new BigDecimal("12.50"));
        h.setPriceRetail(new BigDecimal("15.00"));
        h.setPriceAssemble(new BigDecimal("11.00"));
        h.setStockQuantity(500);
        h.setFreightAmount(new BigDecimal("8.00"));
        h.setShopName("华北制药官方旗舰店");
        h.setCollectedAt(LocalDateTime.now());

        int rows = mapper.insert(h);
        assertEquals(1, rows);
        assertNotNull(h.getId());

        List<YyPriceHistory> results = mapper.selectByPlatformAndSku(
            "ysbang", "SKU-TEST-001",
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now().plusDays(1)
        );
        assertFalse(results.isEmpty());
        assertEquals("阿莫西林胶囊", results.get(0).getProductName());
        assertEquals(new BigDecimal("12.50"), results.get(0).getPriceCurrent());
    }
}
