package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 更新功能开关请求 DTO
 */
public record UpdateFeatureFlagRequest(
        boolean enabled,                     // 启用状态
        @Min(0) @Max(100)
        int rolloutPercent,                 // 放量比例(限制0-100)
        String rulesJson,                   // 定向规则JSON字符串
        long version                        // 乐观锁版本号
) {
}
