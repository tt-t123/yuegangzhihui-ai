package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest( // 定义公共记录类：创建部门请求 DTO no usages
                                       String parentId, // 属性：父级部门ID（可为空，表示顶级部门） no usages
                                       @NotBlank @Size(max = 32) String code, // 校验：部门编码不能为空且最大32字符 no usages
                                       @NotBlank @Size(max = 100) String name, // 校验：部门名称不能为空且最大100字符 no usages
                                       int sortOrder // 属性：显示排序权重值 no usages
) { } // 类定义结束
