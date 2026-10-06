package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest( // 定义公共记录类：创建岗位请求 DTO no usages
                                     @NotBlank @Size(max = 32) String code, // 校验：岗位编码不能为空且最大32字符 no usages
                                     @NotBlank @Size(max = 100) String name, // 校验：岗位名称不能为空且最大100字符 no usages
                                     @Size(max = 500) String description // 校验：岗位描述信息最大长度500字符 no usages
) { } // 类定义结束
