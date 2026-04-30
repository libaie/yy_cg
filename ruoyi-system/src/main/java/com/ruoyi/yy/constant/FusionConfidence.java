package com.ruoyi.yy.constant;

import java.math.BigDecimal;

/**
 * 融合匹配置信度常量
 */
public final class FusionConfidence {

    private FusionConfidence() {}

    /** 条码精确匹配置信度 */
    public static final BigDecimal BARCODE = new BigDecimal("1.00");

    /** 批准文号精确匹配置信度 */
    public static final BigDecimal APPROVAL_NUMBER = new BigDecimal("0.98");

    /** 模糊匹配置信度下限 */
    public static final BigDecimal FUZZY_MIN = new BigDecimal("0.70");

    /** 模糊匹配置信度上限 */
    public static final BigDecimal FUZZY_MAX = new BigDecimal("0.95");

    /** AI匹配置信度下限 */
    public static final BigDecimal AI_MIN = new BigDecimal("0.50");

    /** AI匹配置信度上限 */
    public static final BigDecimal AI_MAX = new BigDecimal("0.99");

    /** 自动接受阈值：>=此值直接接受 */
    public static final BigDecimal AUTO_ACCEPT = new BigDecimal("0.95");

    /** 待复核阈值：>=此值自动接受但标记待复核 */
    public static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.80");

    /**
     * 判断是否自动接受
     */
    public static boolean isAutoAccept(BigDecimal confidence) {
        return confidence.compareTo(AUTO_ACCEPT) >= 0;
    }

    /**
     * 判断是否需要人工审核
     */
    public static boolean needsReview(BigDecimal confidence) {
        return confidence.compareTo(REVIEW_THRESHOLD) >= 0
            && confidence.compareTo(AUTO_ACCEPT) < 0;
    }

    /**
     * 判断是否拒绝（进入审核队列）
     */
    public static boolean isRejected(BigDecimal confidence) {
        return confidence.compareTo(REVIEW_THRESHOLD) < 0;
    }
}
