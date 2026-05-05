package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.service.IYyAiAdvisor;
import com.ruoyi.yy.service.IYyAiGateway;
import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPurchaseAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YyAiAdvisorImpl implements IYyAiAdvisor {

    private static final Logger log = LoggerFactory.getLogger(YyAiAdvisorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是资深医药采购策略顾问，拥有药学、流行病学、商业渠道和市场营销的复合背景。\n" +
        "请对用户提交的采购清单进行多维度分析，并返回结构化JSON。\n" +
        "\n" +
        "## 分析维度\n" +
        "1. 药品本体与基础属性维度（定性基底）：\n" +
        "   - 分析药品剂型、规格、储存条件、效期等对采购决策的影响\n" +
        "   - 处方药/OTC属性对渠道选择的约束\n" +
        "   - 建议最小起订量与安全库存\n" +
        "\n" +
        "2. 外部时序与流行病学维度（动态增量）：\n" +
        "   - 结合当前季节/流行病趋势判断需求走势\n" +
        "   - 药品生命周期阶段（新药导入期/成熟期/衰退期）对定价策略的影响\n" +
        "   - 建议备货节奏与时间窗口\n" +
        "\n" +
        "3. 商业价值与渠道博弈维度（终端推力）：\n" +
        "   - 各平台价格差异背后的渠道策略分析\n" +
        "   - 平台补贴、返利、账期等隐性成本的估算\n" +
        "   - 多平台分散采购 vs 集中采购的利弊权衡\n" +
        "\n" +
        "4. 舆情与社媒热度维度（消费者心智）：\n" +
        "   - 药品在医生/患者群体中的认知度与口碑倾向\n" +
        "   - 是否存在负面舆情或替代品热度上升的风险\n" +
        "   - 品牌药vs仿制药的消费者接受度差异\n" +
        "\n" +
        "5. 竞品环境与替代性维度（生存空间）：\n" +
        "   - 同通用名不同厂商的竞争格局\n" +
        "   - 可替代药品/新药上市威胁评估\n" +
        "   - 集采政策对价格体系的潜在冲击\n" +
        "\n" +
        "## 返回JSON格式\n" +
        "{\n" +
        "  \"summary\": \"一句话核心建议\",\n" +
        "  \"bestPlatform\": \"最优采购平台\",\n" +
        "  \"bestPrice\": 最优单价,\n" +
        "  \"totalSaving\": 预估总节省,\n" +
        "  \"dimensions\": {\n" +
        "    \"drugAttributes\": {\"level\": \"high|medium|low\", \"analysis\": \"本体属性分析\", \"actions\": [\"行动建议\"]},\n" +
        "    \"epidemiology\":    {\"level\": \"high|medium|low\", \"analysis\": \"流行病学趋势分析\", \"actions\": []},\n" +
        "    \"channelStrategy\": {\"level\": \"high|medium|low\", \"analysis\": \"渠道博弈分析\", \"actions\": []},\n" +
        "    \"publicOpinion\":   {\"level\": \"high|medium|low\", \"analysis\": \"舆情热度分析\", \"actions\": []},\n" +
        "    \"competition\":     {\"level\": \"high|medium|low\", \"analysis\": \"竞品替代分析\", \"actions\": []}\n" +
        "  },\n" +
        "  \"riskWarnings\": [\"风险提示\"],\n" +
        "  \"tips\": [\"具体采购建议\"]\n" +
        "}\n" +
        "\n" +
        "注意：\n" +
        "- level取值：high=该维度对本次采购影响重大，medium=中等影响，low=影响较小\n" +
        "- 每个维度必须包含analysis（分析）和actions（可执行建议）\n" +
        "- 基于实际数据推理，不确定的信息请标注\"据推测\"或\"建议进一步调研\"\n" +
        "- 所有金额使用人民币元为单位";

    @Autowired
    private IYyAiGateway aiGateway;

    /**
     * 获取采购建议
     */
    public YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices) {
        return getAdvice(drugName, prices, null);
    }

    public YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices, String model) {
        if (prices == null || prices.isEmpty()) {
            YyPurchaseAdvice empty = new YyPurchaseAdvice();
            empty.setSummary("暂无该药品的比价数据");
            empty.setTips(new ArrayList<>());
            return empty;
        }

        String userPrompt = buildPrompt(drugName, prices);

        YyAiRequest request = new YyAiRequest();
        request.setScene("advisor");
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setUserPrompt(userPrompt);
        request.setModel(model != null ? model : "deepseek-chat");
        request.setTemperature(0.3);
        request.setMaxTokens(2000);

        YyAiResponse response = aiGateway.call(request);

        if (!response.isSuccess()) {
            log.warn("AI advisor failed: {}", response.getErrorMessage());
            return buildFallbackAdvice(prices);
        }

        return parseResponse(response.getContent(), prices);
    }

    @Autowired(required = false)
    private com.ruoyi.yy.mapper.YyPriceComparisonMapper priceComparisonMapper;

    private String buildPrompt(String drugName, List<YyPriceComparison> prices) {
        // 数据融合
        List<DataFusionUtil.FusedItem> fused = DataFusionUtil.fuse(
            prices,
            YyPriceComparison::getCurrentPrice,
            p -> p.getFreightAmount() != null ? p.getFreightAmount() : BigDecimal.ZERO,
            p -> p.getSpecification() != null ? p.getSpecification() : "",
            p -> drugName,
            YyPriceComparison::getSourcePlatform
        );

        // 尝试获取历史价格趋势
        Map<String, List<Map<String, Object>>> priceHistory = new LinkedHashMap<>();
        if (priceComparisonMapper != null) {
            for (YyPriceComparison p : prices) {
                try {
                    List<Map<String, Object>> trend = priceComparisonMapper.selectPriceTrend(
                        p.getSkuId(), p.getSourcePlatform(), 30);
                    if (trend != null && !trend.isEmpty())
                        priceHistory.put(p.getSourcePlatform(), trend);
                } catch (Exception ignored) {}
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(DataFusionUtil.buildFusionReport(fused, prices.size()));

        // 历史价格趋势
        if (!priceHistory.isEmpty()) {
            sb.append("## 近30天价格趋势\n");
            priceHistory.forEach((platform, trend) -> {
                sb.append("- ").append(platform).append(": ");
                List<BigDecimal> hPrices = new ArrayList<>();
                for (Map<String, Object> t : trend) {
                    Object ap = t.get("activityPrice");
                    if (ap instanceof Number) hPrices.add(BigDecimal.valueOf(((Number) ap).doubleValue()));
                }
                if (hPrices.size() >= 2) {
                    BigDecimal first = hPrices.get(0), last = hPrices.get(hPrices.size() - 1);
                    BigDecimal change = last.subtract(first);
                    String dir = change.compareTo(BigDecimal.ZERO) >= 0 ? "↑" : "↓";
                    sb.append(first).append(" → ").append(last)
                      .append(" (").append(dir).append("¥").append(change.abs()).append(")\n");
                } else {
                    sb.append("数据不足\n");
                }
            });
            sb.append("\n");
        }

        // 每个融合项的单价分析
        sb.append("## 单价分析（包装换算后）\n");
        for (DataFusionUtil.FusedItem f : fused) {
            int qty = DataFusionUtil.extractPackQuantity(f.normalizedSpec);
            if (qty > 1 && f.minPrice != null) {
                BigDecimal unitPrice = DataFusionUtil.getUnitPrice(f.minPrice, f.normalizedSpec);
                sb.append("- ").append(f.commonName).append(" (").append(f.normalizedSpec).append(")")
                  .append(": 整包 ¥").append(f.minPrice)
                  .append(" / 每单位 ¥").append(unitPrice).append("\n");
            }
        }
        sb.append("\n");

        sb.append("## 分析要求\n");
        sb.append("请按5个维度全面分析，重点关注：\n");
        sb.append("1. 不同平台的剂型/规格差异是否影响临床使用\n");
        sb.append("2. 当前季节和流行病趋势对需求的影响\n");
        sb.append("3. 隐性成本（账期、返利、配送时效）与显性价格的权衡\n");
        sb.append("4. 用户口碑和品牌溢价是否值得支付\n");
        sb.append("5. 是否有被替代或集采降价的风险\n");
        sb.append("\n返回完整的5维度JSON分析结果。");
        return sb.toString();
    }

    private YyPurchaseAdvice parseResponse(String content, List<YyPriceComparison> prices) {
        try {
            JsonNode root = JSON.readTree(content);
            YyPurchaseAdvice advice = new YyPurchaseAdvice();
            advice.setSummary(root.path("summary").asText("暂无建议"));
            advice.setBestPlatform(root.path("bestPlatform").asText());
            advice.setBestPrice(new BigDecimal(root.path("bestPrice").asText("0")));
            advice.setTotalSaving(new BigDecimal(root.path("totalSaving").asText("0")));

            // 5维度分析
            JsonNode dims = root.path("dimensions");
            if (!dims.isMissingNode()) {
                advice.setDimensionAnalysis(JSON.writeValueAsString(dims));
            }

            // 风险提示
            List<String> risks = new ArrayList<>();
            JsonNode riskNode = root.path("riskWarnings");
            if (riskNode.isArray()) {
                for (JsonNode r : riskNode) risks.add(r.asText());
            }
            advice.setRiskWarnings(risks);

            List<String> tips = new ArrayList<>();
            JsonNode tipsNode = root.path("tips");
            if (tipsNode.isArray()) {
                for (JsonNode tip : tipsNode) {
                    tips.add(tip.asText());
                }
            }
            advice.setTips(tips);
            return advice;
        } catch (Exception e) {
            log.error("Failed to parse AI advisor response", e);
            return buildFallbackAdvice(prices);
        }
    }

    /**
     * 降级建议：不调用LLM，直接取最低价
     */
    private YyPurchaseAdvice buildFallbackAdvice(List<YyPriceComparison> prices) {
        YyPriceComparison cheapest = prices.stream()
            .min((a, b) -> {
                BigDecimal priceA = a.getCurrentPrice() != null ? a.getCurrentPrice() : BigDecimal.valueOf(99999);
                BigDecimal priceB = b.getCurrentPrice() != null ? b.getCurrentPrice() : BigDecimal.valueOf(99999);
                return priceA.compareTo(priceB);
            })
            .orElse(null);

        YyPurchaseAdvice advice = new YyPurchaseAdvice();
        if (cheapest != null) {
            advice.setSummary("推荐在 " + cheapest.getSourcePlatform() + " 采购，价格最低");
            advice.setBestPlatform(cheapest.getSourcePlatform());
            advice.setBestPrice(cheapest.getCurrentPrice());
        } else {
            advice.setSummary("暂无价格数据");
        }
        advice.setTips(new ArrayList<>());
        return advice;
    }
}
