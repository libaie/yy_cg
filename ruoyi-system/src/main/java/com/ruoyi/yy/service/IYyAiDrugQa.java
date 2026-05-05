package com.ruoyi.yy.service;

import java.util.Map;

public interface IYyAiDrugQa {
    Map<String, Object> ask(String question, String drugName);
}
