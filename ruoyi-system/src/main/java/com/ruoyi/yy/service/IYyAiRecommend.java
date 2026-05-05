package com.ruoyi.yy.service;

import java.util.Map;

public interface IYyAiRecommend {
    Map<String, Object> recommend(String category, int limit);
}
