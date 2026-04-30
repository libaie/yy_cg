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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批准文号精确匹配策略 — 优先级第二
 *
 * 批准文号格式多样："国药准字Z11020001"、"Z11020001"、"国药准字H20000001"
 * 归一化规则：去除"国药准字"前缀，保留字母+数字部分
 */
@Component
public class YyApprovalNumberMatchStrategy implements IYyMatchStrategy {

    private static final Pattern APPROVAL_PATTERN =
        Pattern.compile("(?:国药准字)?([A-Za-z]\\d{8})");

    private final IYyDrugMasterService drugMasterService;

    @Autowired
    public YyApprovalNumberMatchStrategy(IYyDrugMasterService drugMasterService) {
        this.drugMasterService = drugMasterService;
    }

    @Override
    public String getName() {
        return "ApprovalNumberMatch";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        String raw = snapshot.getApprovalNumber();
        if (raw == null || raw.trim().isEmpty()) {
            return YyMatchResult.failure("No approval number on snapshot");
        }

        String normalized = normalizeApprovalNumber(raw.trim());
        if (normalized == null) {
            return YyMatchResult.failure("Cannot parse approval number: " + raw);
        }

        YyDrugMaster drug = drugMasterService.selectByApprovalNumber(normalized);
        if (drug == null) {
            return YyMatchResult.failure("No drug master with approval number: " + normalized);
        }

        return YyMatchResult.success(
            drug.getId(),
            drug.getDrugCode(),
            FusionConfidence.APPROVAL_NUMBER,
            MatchMethod.APPROVAL,
            "Exact approval number match: " + normalized
        );
    }

    /**
     * 归一化批准文号：提取字母+8位数字部分
     * "国药准字Z11020001" → "Z11020001"
     * "Z11020001" → "Z11020001"
     */
    static String normalizeApprovalNumber(String raw) {
        Matcher m = APPROVAL_PATTERN.matcher(raw);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }
}
