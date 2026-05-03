package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public final class YyAiResponseParser {

    private YyAiResponseParser() {}

    public static List<String> parseArray(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        JsonNode node = root.path(field);
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    public static Map<String, Object> fallbackMap(String defaultAnswer) {
        Map<String, Object> result = new HashMap<>();
        result.put("answer", defaultAnswer);
        result.put("sources", Collections.emptyList());
        result.put("warnings", Collections.emptyList());
        return result;
    }
}
