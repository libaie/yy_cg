package com.ruoyi.yy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台适配器注册中心 — 启动时加载所有适配器，运行时按platformCode获取
 */
@Component
public class YyPlatformAdapterRegistry {

    private final Map<String, IYyPlatformAdapter> adapters = new HashMap<>();
    private IYyPlatformAdapter defaultAdapter;

    @Autowired
    public YyPlatformAdapterRegistry(List<IYyPlatformAdapter> adapterList) {
        for (IYyPlatformAdapter adapter : adapterList) {
            adapters.put(adapter.getPlatformCode(), adapter);
            if ("*".equals(adapter.getPlatformCode())) {
                defaultAdapter = adapter;
            }
        }
    }

    /**
     * 获取指定平台的适配器，未找到则返回通用适配器
     */
    public IYyPlatformAdapter getAdapter(String platformCode) {
        IYyPlatformAdapter adapter = adapters.get(platformCode);
        if (adapter == null) {
            adapter = defaultAdapter;
        }
        return adapter;
    }
}
