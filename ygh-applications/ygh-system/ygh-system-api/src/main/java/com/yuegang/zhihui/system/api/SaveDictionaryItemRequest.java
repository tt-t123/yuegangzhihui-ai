package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 保存/更新字典项请求 DTO
 */
public record SaveDictionaryItemRequest(@NotBlank @Size(max = 64) String key, @NotBlank @Size(max = 500) String value,
                                        int sortOrder, boolean enabled, @PositiveOrZero long version) { // 排序值、启用状态、版本号校验 no usages
} // 记录类定义结束
