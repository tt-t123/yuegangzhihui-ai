package com.yuegang.zhihui.system.api;

public record SystemSettingView(String key, String value, String valueType, boolean secret, long version) {
} // 记录类定义结束
