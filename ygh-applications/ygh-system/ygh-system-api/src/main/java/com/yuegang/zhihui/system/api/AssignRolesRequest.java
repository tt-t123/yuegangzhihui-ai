package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 分配角色请求 DTO
 */
public record AssignRolesRequest(
        @NotNull
        @Size(max = 32)
        Set<@Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String> roleCodes, // 角色编码集合。约束：非空，最多32个
        @PositiveOrZero
        long version, // 版本号。约束：必须是正数或零
        String reason // 操作原因说明（如：入职授权）
) {
}

