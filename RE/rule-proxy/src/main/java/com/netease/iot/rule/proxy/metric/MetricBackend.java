package com.netease.iot.rule.proxy.metric;

import java.util.List;
import java.util.Map;

public interface MetricBackend {
    List<List<Map.Entry<String, String>>> query(List<String> keys, String columnPrefix);
}
