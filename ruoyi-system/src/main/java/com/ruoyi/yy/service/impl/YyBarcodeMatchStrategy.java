package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.constant.MatchMethod;
import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.IYyMatchStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 条码（69码）精确匹配策略 — 优先级最高
 */
@Component
public class YyBarcodeMatchStrategy implements IYyMatchStrategy {

    private final IYyDrugMasterService drugMasterService;

    @Autowired
    public YyBarcodeMatchStrategy(IYyDrugMasterService drugMasterService) {
        this.drugMasterService = drugMasterService;
    }

    @Override
    public String getName() {
        return "BarcodeMatch";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        if (snapshot == null) {
            return YyMatchResult.failure("Snapshot is null");
        }
        String barcode = snapshot.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            return YyMatchResult.failure("No barcode on snapshot");
        }

        YyDrugMaster drug = drugMasterService.selectByBarcode(barcode.trim());
        if (drug == null) {
            return YyMatchResult.failure("No drug master with barcode: " + barcode);
        }

        return YyMatchResult.success(
            drug.getId(),
            drug.getDrugCode(),
            FusionConfidence.BARCODE,
            MatchMethod.BARCODE,
            "Exact barcode match: " + barcode
        );
    }
}
