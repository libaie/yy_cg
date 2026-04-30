package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.domain.YyMatchResult;
import com.ruoyi.yy.domain.YyProductSnapshot;
import java.util.List;

/**
 * 匹配策略接口 — 策略链中的每一环
 */
public interface IYyMatchStrategy {

    /**
     * 策略名称
     */
    String getName();

    /**
     * 优先级，越高越先执行
     */
    int getPriority();

    /**
     * 尝试将商品快照匹配到药品主数据
     *
     * @param snapshot   待匹配的商品快照
     * @param candidates 候选药品列表（可能为空，策略自行查询亦可）
     * @return 匹配结果，matched=false表示本策略无法匹配
     */
    YyMatchResult match(YyProductSnapshot snapshot, List<YyDrugMaster> candidates);
}
