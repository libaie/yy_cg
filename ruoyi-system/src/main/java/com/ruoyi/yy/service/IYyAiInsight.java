package com.ruoyi.yy.service;

import java.util.Map;

public interface IYyAiInsight {
    Map<String, Object> analyze(String drugName, String pricesJson);
}
