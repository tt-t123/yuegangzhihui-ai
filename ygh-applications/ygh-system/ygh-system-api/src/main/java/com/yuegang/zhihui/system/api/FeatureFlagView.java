package com.yuegang.zhihui.system.api;

/**
 * 特性开关（功能开关）视图对象
 */
public record FeatureFlagView(
        String key,
        boolean enabled,
        int rolloutPercent,
        String rulesJson,
        long version
) {
}
