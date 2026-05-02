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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 模糊匹配策略 — 基于归一化字符串相似度
 *
 * 匹配逻辑：
 * 1. 归一化通用名、规格、厂家名
 * 2. 查询候选药品（通用名前缀匹配）
 * 3. 对每个候选计算三维相似度（通用名、规格、厂家）
 * 4. 加权综合得分，取最高分
 */
@Component
public class YyFuzzyMatchStrategy implements IYyMatchStrategy {

    private static final double WEIGHT_COMMON_NAME = 0.4;
    private static final double WEIGHT_SPECIFICATION = 0.3;
    private static final double WEIGHT_MANUFACTURER = 0.3;
    private static final double MIN_MATCH_SCORE = 0.7;

    // 厂家名后缀
    private static final Pattern MANUFACTURER_SUFFIXES = Pattern.compile(
        "(股份有限公司|有限公司|有限责任公司|集团|药业|制药|医药|股份|Inc\\.?|Ltd\\.?|Co\\.?|Corp\\.?)",
        Pattern.CASE_INSENSITIVE
    );

    // 规格标准化
    private static final Pattern SPEC_X = Pattern.compile("[×xX]");

    private final IYyDrugMasterService drugMasterService;

    @Autowired
    public YyFuzzyMatchStrategy(IYyDrugMasterService drugMasterService) {
        this.drugMasterService = drugMasterService;
    }

    @Override
    public String getName() {
        return "FuzzyMatch";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        if (snapshot == null) {
            return YyMatchResult.failure("Snapshot is null");
        }

        String commonName = snapshot.getCommonName();
        String specification = snapshot.getSpecification();
        String manufacturer = snapshot.getManufacturer();

        if (commonName == null || commonName.trim().isEmpty()) {
            return YyMatchResult.failure("No common name on snapshot");
        }

        // 归一化输入
        String normCommonName = commonName.trim();
        String normSpec = normalizeSpecification(specification);
        String normMfr = normalizeManufacturer(manufacturer);

        // 取规格前缀作为查询条件（如 "0.25g"）
        String specPrefix = extractSpecPrefix(normSpec);

        // 查询候选药品（前缀匹配 → 回退全模糊匹配）
        List<YyDrugMaster> candidateList = candidates;
        if (candidateList == null || candidateList.isEmpty()) {
            candidateList = drugMasterService.selectCandidates(normCommonName, specPrefix);
        }
        if (candidateList == null || candidateList.isEmpty()) {
            candidateList = drugMasterService.selectCandidatesFallback(normCommonName, specPrefix);
        }

        if (candidateList.isEmpty()) {
            return YyMatchResult.failure("No candidates found for: " + normCommonName);
        }

        // 找最佳匹配
        YyDrugMaster bestMatch = null;
        double bestScore = 0;

        for (YyDrugMaster candidate : candidateList) {
            double score = calculateScore(
                normCommonName, normSpec, normMfr,
                candidate.getCommonName(),
                normalizeSpecification(candidate.getSpecification()),
                normalizeManufacturer(candidate.getManufacturer())
            );

            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch == null || bestScore < MIN_MATCH_SCORE) {
            return YyMatchResult.failure("Best score " + bestScore + " below threshold " + MIN_MATCH_SCORE);
        }

        BigDecimal confidence = BigDecimal.valueOf(bestScore)
            .setScale(2, RoundingMode.HALF_UP)
            .min(FusionConfidence.FUZZY_MAX)
            .max(FusionConfidence.FUZZY_MIN);

        return YyMatchResult.success(
            bestMatch.getId(),
            bestMatch.getDrugCode(),
            confidence,
            MatchMethod.FUZZY,
            "Fuzzy match score=" + bestScore + " against drug_id=" + bestMatch.getId()
        );
    }

    /**
     * 计算三维加权相似度
     */
    double calculateScore(String srcName, String srcSpec, String srcMfr,
                          String tgtName, String tgtSpec, String tgtMfr) {
        double nameSim = levenshteinSimilarity(srcName, tgtName);
        double specSim = levenshteinSimilarity(srcSpec, tgtSpec);
        double mfrSim = levenshteinSimilarity(srcMfr, tgtMfr);

        return nameSim * WEIGHT_COMMON_NAME
             + specSim * WEIGHT_SPECIFICATION
             + mfrSim * WEIGHT_MANUFACTURER;
    }

    /**
     * 归一化厂家名：去除公司后缀
     */
    public String normalizeManufacturer(String manufacturer) {
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            return "";
        }
        String result = manufacturer.trim();
        result = MANUFACTURER_SUFFIXES.matcher(result).replaceAll("");
        result = result.replaceAll("[\\s()（）]+", "");
        return result;
    }

    /**
     * 归一化规格：统一乘号和单位
     */
    public String normalizeSpecification(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            return "";
        }
        String result = specification.trim();
        result = SPEC_X.matcher(result).replaceAll("*");
        result = result.replace("片", "s").replace("粒", "s").replace("颗", "s");
        result = result.replace("支", "b").replace("瓶", "b").replace("袋", "p");
        result = result.replaceAll("\\s+", "");
        return result;
    }

    /**
     * 提取规格前缀（数字+单位部分）
     */
    private String extractSpecPrefix(String normalizedSpec) {
        if (normalizedSpec == null || normalizedSpec.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher m = Pattern.compile("^(\\d+\\.?\\d*[a-zA-Z]+)").matcher(normalizedSpec);
        if (m.find()) {
            return m.group(1);
        }
        return normalizedSpec;
    }

    /**
     * Levenshtein相似度（0.0 - 1.0）
     */
    double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int len1 = s1.length();
        int len2 = s2.length();
        int maxLen = Math.max(len1, len2);

        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return 1.0 - (double) dp[len1][len2] / maxLen;
    }
}
