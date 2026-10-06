package com.yuegang.zhihui.system.api;

import java.util.List;

/**
 * 数据字典管理视图（用于后台管理界面）
 */
public record DictionaryAdminView(
        String code,
        String name,
        boolean enabled,
        long version,
        List<Item> items
) {
    /**
     * 内部记录类：字典项明细
     */
    public record Item(
            String key,
            String value,
            int sortOrder,
            boolean enabled,
            long version
    ) {
    }
}

