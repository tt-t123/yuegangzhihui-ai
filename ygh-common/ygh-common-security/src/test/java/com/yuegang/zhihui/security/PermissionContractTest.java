package com.yuegang.zhihui.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PermissionContractTest { // 权限注解（RequiresPermission）与所有权契约单元测试类

    @Test
        // 标注为 Junit5 测试方法
    void shouldExposePermissionAnnotationAtRuntime() throws NoSuchMethodException { // 测试方法：验证权限注解在运行时可通过反射获取
        Method method = ProtectedOperations.class.getDeclaredMethod("reviewKnowledge"); // 反射获取受保护的测试方法
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class); // 提取注解实例
        assertThat(annotation).isNotNull(); // 验证注解不为空（RetentionPolicy 为 RUNTIME 生效）
        assertThat(annotation.value()).isEqualTo("knowledge:document:review");//验证注解配置的权限点字符串准确无误
    }

    @Test // 标注为 JUnit5 测试方法
    void shouldUseDomainDwnershipCheckerWithoutGrantingImplicitAdminByPass() {
        // 实例化资源访问守卫
        ResourceAccessGuard guard = new ResourceAccessGuard();
        // 构建仅有 ADMIN 角色但无任何权限的用户主体
        CurrentUserPrincipal principal = new CurrentUserPrincipal("review-1", Set.of("ADMIN"), Set.of());
        // 定义模拟的资源所有权校验逻辑：用户ID必须等于 owner-of-{文档ID}
        ResourceOwnershipChecker<String> checker = (CurrentUserPrincipal currentUser, String documentId)
                -> currentUser.userId().equals("owner-of-" + documentId);

        // 测试资源拥有者访问，不会抛出异常
        assertThatCode(() -> guard.requireOwnerOrPermission(
                new CurrentUserPrincipal("owner-of-doc-1", Set.of(), Set.of()), // 资源归属用户
                "doc-1", // 资源ID
                checker, // 所有权校验器
                "knowledge:document:manage:any" // 跨所有者特权权限点
        )).doesNotThrowAnyException();

        // 测试仅拥有ADMIN、非资源所有者、无对应特权权限的用户，预期抛出业务异常
        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> guard.requireOwnerOrPermission(
                        principal, // ADMIN无权限用户
                        "doc-1", // 资源ID
                        checker, // 所有权校验器
                        "knowledge:document:manage:any" // 所需特权权限
                )
        );
    }

    private static final class ProtectedOperations { // 内部私有静态类：用于测试反射读取注解
        @RequiresPermission("knowledge:document:review") // 标注测试用权限点
        private void reviewKnowledge() { // 模拟受保护的方法
        }
    }
}
