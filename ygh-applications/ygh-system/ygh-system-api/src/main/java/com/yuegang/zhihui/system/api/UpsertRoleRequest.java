package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * 新增或更新角色请求 DTO
 */
public record UpsertRoleRequest(
        @NotBlank
        @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}")
        String code,                                          // 角色编码
        @NotBlank
        @Size(max = 100)
        String name,                                          // 角色名称：非空且限100位
        @NotNull
        Set<@Pattern(regexp = "[A-Za-z][A-Za-z0-9:_-]{0,127}") String> permissions,  // 绑定权限编码集合
        boolean enabled,                                      // 是否启用
        @PositiveOrZero
        long version                                          // 版本号校验
) {
}

