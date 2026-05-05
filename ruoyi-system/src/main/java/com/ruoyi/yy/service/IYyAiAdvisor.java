package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPurchaseAdvice;
import java.util.List;

public interface IYyAiAdvisor {
    YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices);
    YyPurchaseAdvice getAdvice(String drugName, List<YyPriceComparison> prices, String model);
}
