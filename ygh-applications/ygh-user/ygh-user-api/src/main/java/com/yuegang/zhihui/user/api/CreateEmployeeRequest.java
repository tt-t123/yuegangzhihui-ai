package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateEmployeeRequest( // no usages
                                     @NotBlank String userId, // 校验：关联的用户账号ID不能为空 no usages
                                     @NotBlank @Size(max = 32) String employeeNo, // 校验：工号不能为空且最大32字符 no usages
                                     String departmentId, // 属性：所属部门ID no usages
                                     Set<String> positionIds, // 属性：关联的岗位ID集合（一个员工可有多个岗位） no usages
                                     LocalDate hiredOn // 属性：入职日志 no usages
) { } // 类定义结束
