package com.yuegang.zhihui.user.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.user.application.AddressService;
import com.yuegang.zhihui.user.security.TrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 标识当前类是一个控制器类
@RequestMapping("/api/v1/users/me/addresses") // 声明为 REST 控制器并设置基础映射路径
public final class AddressController { // 定义地址控制器类
    private final AddressService service;
    private final TrustedUserContextResolver users; // 声明业务服务和用户解析器成员变量

    public AddressController(AddressService service, TrustedUserContextResolver users) { // 构造函数注入依赖
        this.service = service;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<List<AddressView>> list(HttpServletRequest request) { // 获取当前用户的地址列表
        return ok(service.list(user(request)), request);
    }

    @PostMapping
    public ApiResponse<AddressView> create(@Valid @RequestBody CreateAddressRequest body, HttpServletRequest request) { // 创建新地址，包含参数校验
        return ok(service.create(user(request), body), request);
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressView> update(@PathVariable String id, @Valid @RequestBody UpdateAddressRequest body, HttpServletRequest request) { // 更新地址，包含参数校验
        return ok(service.update(user(request),id,body), request);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AddressOperationResponse> delete(@PathVariable String id, @RequestParam long version,HttpServletRequest request) { // 删除地址接口，携带乐观锁版本号
        return ok(service.delete(user(request),id,version), request);
    }

    @PutMapping("/{id}/default")
    public ApiResponse<AddressView> makeDefault(@PathVariable String id, @RequestParam long version, HttpServletRequest request) { // 将指定地址设置为默认地址
        return ok(service.makeDefault(user(request),id,version),request);
    }

    private String user(HttpServletRequest request) { // 私有辅助方法：从请求中解析并获取当前登录用户 ID
        return users.resolve(request).userId();
    }

    private static <T> ApiResponse<T> ok(T data,HttpServletRequest request) { // 私有辅助方法：封装成功的 API 并携带 Trace ID
        return ApiResponse.success(data, TraceIdResolver.resolve(request));
    }
}
