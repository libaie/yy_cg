package com.ruoyi.yy.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI数据融合工具 — 多平台数据归一化（纯内存操作，不写DB）
 * <p>
 * 功能：
 * 1. 规格归一化（全角→半角、分隔符统一、排序）
 * 2. 运费归一化（计算 effectivePrice = price + freight）
 * 3. 包装换算（提取最小单位价格）
 * 4. 多平台去重合并（同规格→合并，不同规格→分列）
 */
public class DataFusionUtil {

    // ---- 规格归一化 ----

    /** 规格归一化：统一全角、分隔符、排序 */
    public static String normalizeSpec(String rawSpec) {
        if (rawSpec == null || rawSpec.isBlank()) return "";
        String s = rawSpec.trim();
        // 全角→半角
        s = s.replace('（', '(').replace('）', ')')
             .replace('×', '*').replace('X', '*').replace('x', '*')
             .replace('＋', '+').replace('－', '-')
             .replace('／', '/').replace('／', '/');
        // 统一 * 两侧去空格
        s = s.replaceAll("\\s*\\*\\s*", "*");
        // 统一 / 两侧去空格
        s = s.replaceAll("\\s*/\\s*", "/");
        // 将 "N袋/M盒" 等数量词标准化
        s = s.replaceAll("([0-9]+)袋", "$1袋");
        s = s.replaceAll("([0-9]+)盒", "$1盒");
        return s;
    }

    /** 比较两个规格是否等价 */
    public static boolean specsMatch(String a, String b) {
        return normalizeSpec(a).equals(normalizeSpec(b));
    }

    // ---- 包装换算 ----

    private static final Pattern PACK_QTY = Pattern.compile("(\\d+)\\s*[袋盒支瓶片粒]");
    private static final Pattern PACK_G = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*g");

    /** 从规格中提取包装数量（袋/盒/支），返回估算的总单位数 */
    public static int extractPackQuantity(String spec) {
        if (spec == null) return 1;
        Matcher m = PACK_QTY.matcher(spec);
        int total = 1;
        while (m.find()) total *= Integer.parseInt(m.group(1));
        return Math.max(total, 1);
    }

    /** 计算每单位价格 */
    public static BigDecimal getUnitPrice(BigDecimal price, String spec) {
        if (price == null) return BigDecimal.ZERO;
        int qty = extractPackQuantity(spec);
        return qty > 1 ? price.divide(BigDecimal.valueOf(qty), 4, RoundingMode.HALF_UP) : price;
    }

    // ---- 运费归一化 ----

    /** 计算有效价格 = 单价 + 运费 */
    public static BigDecimal getEffectivePrice(BigDecimal price, BigDecimal freight) {
        BigDecimal p = price != null ? price : BigDecimal.ZERO;
        BigDecimal f = freight != null ? freight : BigDecimal.ZERO;
        return p.add(f);
    }

    // ---- 多平台融合 ----

    /** 融合后的单条结果 */
    public static class FusedItem {
        public String commonName;
        public String normalizedSpec;
        public String manufacturer;
        public BigDecimal minPrice;
        public BigDecimal minEffectivePrice;
        public BigDecimal avgPrice;
        public String bestPlatform;
        public int platformCount;
        public Map<String, BigDecimal> platformPrices = new LinkedHashMap<>();
        public List<String> warnings = new ArrayList<>();
    }

    /**
     * 多平台数据融合
     * @param items 原始多平台价格条目
     * @param priceGetter 从 item 获取价格的函数
     * @param freightGetter 从 item 获取运费的函数
     * @param specGetter 从 item 获取规格的函数
     * @param nameGetter 从 item 获取品名的函数
     * @param platformGetter 从 item 获取平台的函数
     * @return 去重合并后的列表
     */
    public static <T> List<FusedItem> fuse(
            List<T> items,
            java.util.function.Function<T, BigDecimal> priceGetter,
            java.util.function.Function<T, BigDecimal> freightGetter,
            java.util.function.Function<T, String> specGetter,
            java.util.function.Function<T, String> nameGetter,
            java.util.function.Function<T, String> platformGetter
    ) {
        // 按 (品名 + 归一化规格) 分组
        Map<String, List<T>> groups = new LinkedHashMap<>();
        for (T item : items) {
            String key = nameGetter.apply(item) + "||" + normalizeSpec(specGetter.apply(item));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<FusedItem> result = new ArrayList<>();
        for (Map.Entry<String, List<T>> e : groups.entrySet()) {
            List<T> group = e.getValue();
            FusedItem fused = new FusedItem();
            fused.commonName = nameGetter.apply(group.get(0));
            fused.normalizedSpec = normalizeSpec(specGetter.apply(group.get(0)));

            BigDecimal minPrice = null, minEffPrice = null, sum = BigDecimal.ZERO;
            String bestPlat = "";
            Set<String> seenPlatforms = new LinkedHashSet<>();

            for (T item : group) {
                BigDecimal price = priceGetter.apply(item);
                BigDecimal freight = freightGetter.apply(item);
                BigDecimal eff = getEffectivePrice(price, freight);
                String plat = platformGetter.apply(item);

                if (minPrice == null || price.compareTo(minPrice) < 0) minPrice = price;
                if (minEffPrice == null || eff.compareTo(minEffPrice) < 0) { minEffPrice = eff; bestPlat = plat; }
                sum = sum.add(price);
                seenPlatforms.add(plat);
                fused.platformPrices.put(plat, price);
            }

            fused.minPrice = minPrice;
            fused.minEffectivePrice = minEffPrice;
            fused.bestPlatform = bestPlat;
            fused.platformCount = seenPlatforms.size();
            fused.avgPrice = sum.divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);

            // 警告：规格不一致
            if (group.stream().map(i -> normalizeSpec(specGetter.apply(i))).distinct().count() > 1)
                fused.warnings.add("不同平台规格描述不一致，可能存在包装差异");

            result.add(fused);
        }
        return result;
    }

    /** 生成 AI 友好的融合报告文本 */
    public static <T> String buildFusionReport(List<FusedItem> fused, int totalRawItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 数据融合报告\n");
        sb.append("原始采集条目: ").append(totalRawItems).append(" → 融合后: ").append(fused.size()).append(" 个独立药品\n\n");

        for (FusedItem f : fused) {
            sb.append("### ").append(f.commonName).append("\n");
            sb.append("- 规格: ").append(f.normalizedSpec).append("\n");
            sb.append("- 覆盖平台: ").append(f.platformCount).append(" 个\n");
            sb.append("- 最低单价: ¥").append(f.minPrice).append(" (").append(f.bestPlatform).append(")\n");
            sb.append("- 最低含运费价: ¥").append(f.minEffectivePrice).append("\n");
            sb.append("- 各平台价格: ");
            f.platformPrices.forEach((plat, pr) -> sb.append(plat).append(" ¥").append(pr).append("  "));
            sb.append("\n");
            if (!f.warnings.isEmpty()) {
                for (String w : f.warnings) sb.append("  ⚠ ").append(w).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
