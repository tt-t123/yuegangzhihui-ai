package com.yuegang.zhihui.system.api;

import java.util.Set;

/**
 * 角色视图对象
 */
public record RoleView(
        String id,
        String code,
        String name,
        boolean enabled,
        long version,
        Set<String> permissions
) {
    /**
     * 辅助构造函数：不带权限列表的初始化
     */
    public RoleView(String id, String code, String name, boolean enabled, long version) {
        this(id, code, name, enabled, version, Set.of());
    }

    /**
     * 紧凑构造函数：防御性拷贝
     */
    public RoleView {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}

