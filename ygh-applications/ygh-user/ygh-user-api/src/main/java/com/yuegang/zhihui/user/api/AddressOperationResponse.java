package com.yuegang.zhihui.user.api;

/**
 * 地址操作响应 DTO
 */
public class AddressOperationResponse {
    private final boolean success;

    /**
     * 构造函数：传入操作是否成功标识
     */
    public AddressOperationResponse(boolean success) {
        this.success = success;
    }

    /**
     * 获取操作是否成功
     */
    public boolean success() {
        return success;
    }
}
