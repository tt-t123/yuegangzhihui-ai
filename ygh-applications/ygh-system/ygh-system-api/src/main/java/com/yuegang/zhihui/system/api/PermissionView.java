package com.yuegang.zhihui.system.api;

/**
 * 权限点视图对象
 */
public record PermissionView(
        String id,
        String code,
        String name,
        String resourceType,
        boolean enabled
) {
}
