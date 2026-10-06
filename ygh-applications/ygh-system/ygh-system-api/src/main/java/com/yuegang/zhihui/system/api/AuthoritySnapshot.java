package com.yuegang.zhihui.system.api;

import java.util.Set;

/**
 * 权限快照对象
 */
public record AuthoritySnapshot(
        String userId,
        Set<String> roles,
        Set<String> permissions,
        long version
) {
    // 紧凑构造函数，用于初始化数据拷贝
    public AuthoritySnapshot {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}

