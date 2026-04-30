package com.ruoyi.yy.constant;

/**
 * 匹配方式枚举
 */
public enum MatchMethod {

    MANUAL("manual", "人工匹配"),
    BARCODE("barcode", "条码匹配"),
    APPROVAL("approval", "批准文号匹配"),
    FUZZY("fuzzy", "模糊匹配"),
    AI("ai", "AI匹配");

    private final String code;
    private final String description;

    MatchMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchMethod fromCode(String code) {
        for (MatchMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown MatchMethod code: " + code);
    }
}
