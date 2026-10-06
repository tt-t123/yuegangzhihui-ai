package com.yuegang.zhihui.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 资源访问守卫（ResourceAccessGuard）单元测试类 */
class ResourceAccessGuardTest {
    // 定义测试用"跨用户管理地址"特权点常量
    private static final String MANAGE_ANY_ADDRESS = "user:address:manage:any";
    // 实例化待测试的守卫对象
    private final ResourceAccessGuard guard = new ResourceAccessGuard();

    /** 测试方法：应当允许资源拥有访问属于自己的资源 */
    @Test
    void shouldAllowOwnerToAccessOwnResource() {
        // 创建普通客户用户主体
        CurrentUserPrincipal principal = principal("user-1001", Set.of("CUSTOMER"), Set.of());
        // 校验资源所有者访问自身资源不抛出异常
        assertThatCode(() -> guard.requireOwnerOrPermission(
                principal,          // 登录主体
                "user-1001",        // 资源拥有者ID
                MANAGE_ANY_ADDRESS  // 跨所有者特权点
        )).doesNotThrowAnyException();
    }

    /** 测试方法：管理员仅当显式具备越权特权时方可访问 */
    @Test
    void shouldAllowAdministratorOnlyWhenExplicitByPassPermissionIsPersistent() {
        // 构建管理员用户：拥有ADMIN角色，同时显式授予跨所有者特权点
        CurrentUserPrincipal principal = principal(
                "admin-1001",                // 用户ID
                Set.of("ADMIN"),             // 用户角色
                Set.of(MANAGE_ANY_ADDRESS)   // 显式授予跨所有者特权点
        );

        // 执行权限校验：管理员访问不属于自己的资源，因拥有特权点直接放行，无异常
        assertThatCode(() -> guard.requireOwnerOrPermission(
                principal,                  // 当前登录用户主体
                "user-1001",                // 资源归属用户ID
                MANAGE_ANY_ADDRESS          // 所需跨所有者特权权限
        )).doesNotThrowAnyException();
    }

    /** 测试方法：仅有 ADMIN 角色但无显式特权点时不得隐式绕过所有权校验 */
    @Test
    void shouldNotTreatAdministratorRoleAsImplicitOwnershipBypass() {
        // 创建仅有 ADMIN 角色但无特权点的用户
        CurrentUserPrincipal principal = principal("admin-1001", Set.of("ADMIN"), Set.of());
        // 捕获预期业务异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> guard.requireOwnerOrPermission(principal, "user-1001", MANAGE_ANY_ADDRESS),
                "ADMIN role alone must not bypass ownership"
        );
        // 校验错误码固定为 PERMISSION_DENIED
        assertThat(stableErrorCode(exception)).isEqualTo("PERMISSION_DENIED");
    }

    /** 测试方法：拒绝既非拥有者又无特权的用户，并返回稳定的错误码 */
    @Test
    void shouldRejectNonOwnerWithoutPermissionUsingStableBusinessError() {
        CurrentUserPrincipal principal = principal(
                "user-2002",                       // 用户ID
                Set.of("CUSTOMER"),                // 角色
                Set.of("user:address:read")        // 仅有读取自己地址的普通权限
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> guard.requireOwnerOrPermission(principal, "user-1001", MANAGE_ANY_ADDRESS),
                "Cross-owner access must require a explicit permission"
        );

        assertThat(stableErrorCode(exception)).isEqualTo("PERMISSION_DENIED");
    }

    /** 工厂辅助方法：快捷创建用户主体 */
    private CurrentUserPrincipal principal(String userId, Set<String> roles, Set<String> permissions) {
        return new CurrentUserPrincipal(userId, roles, permissions);
    }

    /**
     * BusinessException 的错误载体属于 common-core 契约：测试兼容 code/getCode 与
     * errorCode/getErrorCode 两种只读访问形式，但最终对外稳定值必须是 PERMISSION_DENIED。
     */
    private static String stableErrorCode(BusinessException exception) {
        Object value = invokeFirstNoArg(exception, "code", "errorCode", "getErrorCode");
        if (value instanceof String code) {
            return code;
        }
        Object nested = invokeFirstNoArg(value, "code", "getCode", "name");
        return String.valueOf(nested);
    }

    /** 辅助反射工具：按顺序尝试调用无参 getter 方法 */
    private static Object invokeFirstNoArg(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // 忽略并继续尝试下一个预设方法名
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new AssertionError("Cannot read business error code through " + methodName, exception);
            }
        }
        throw new AssertionError("BusinessException must expose a stable error code");
    }
}
