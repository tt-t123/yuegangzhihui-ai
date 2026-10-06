package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.system.api.SystemSettingView;
import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.SystemSettingService;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 系统设置控制器
@RestController // 声明为 REST 服务组件 no usages
@RequestMapping("/api/v1/system/settings") // 映射系统全局配置相关的根路径
public class SystemSettingController { // 定义最终类：系统设置控制器
    private final SystemSettingService service; // 全局设置业务处理服务 2 usages
    private final SystemTrustedUserContextResolver users; // 身份解析器 2 usages

    public SystemSettingController(SystemSettingService s, SystemTrustedUserContextResolver u) { // 构造函数 no usages
        service = s; // 初始化业务服务
        users = u; // 初始化身份解析器
    } // 构造方法结束

    @GetMapping
        // 获取当前系统所有全局配置项列表的接口 no usages
    ApiResponse<List<SystemSettingView>> list(HttpServletRequest r) { // 接收请求对象
        admin(r); // 调用权限检查方法
        return ApiResponse.success(service.list(), TraceIdResolver.resolve(r)); // 返回成功的配置列表响应
    }

    @PutMapping("/{key}")
        // 更新或修改当前的全局配置信息列表接口 no usages
    ApiResponse<SystemSettingView> update(@PathVariable String key, @Valid @RequestBody UpdateSystemSettingRequest body, HttpServletRequest r) {
        var p = admin(r); // 执行权限校验并获取操作人主体
        return ApiResponse.success(service.update(key, body, Long.parseLong(p.userId())), TraceIdResolver.resolve(r)); // 记录变更并返回最新的配置
    }

    private CurrentUserPrincipal admin(HttpServletRequest r) { // 私有方法：强制要求管理员权限 2 usages
        var p = users.resolve(r); // 解析来自网关的受信任用户上下文
        if (!p.roles().contains("ADMIN")) throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 如果不包含ADMIN角色则拒绝执行
        return p; // 校验通过返回当前用户信息
    } // 结束

    private static <T> ApiResponse<T> ok(T data, HttpServletRequest request) { // 封装成功的响应结果工具方法 no usages
        return ApiResponse.success(data, TraceIdResolver.resolve(request)); // 生成统一响应格式
    }
}// 结束