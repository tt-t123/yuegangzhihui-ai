# 修复 TrustedUserContextFilter 的 encodeAuthorities 方法无法解析问题

## 摘要

[TrustedUserContextFilter.java](file:///c:/Users/唐国几/IdeaProjects/yuegang-zhihui-ai1/ygh-platform/ygh-gateway/src/main/java/com/yuegang/zhihui/gateway/TrustedUserContextFilter.java) 中存在方法名不一致和参数签名不匹配问题，导致 4 处 `encodeAuthorities` 调用无法解析。同时文件中残留了两个冗余方法（`split` 和 `encodedAuthorities`），与已有的 `validateAuthorities` 功能重叠，需一并清理。

## 现状分析

### 调用方与定义方不匹配

**调用点（4 处错误）**：
- 第 56 行：`encodeAuthorities(principal.roles(), "roles")` —— 2 参数，期望返回 `List<String>`（赋值给 `List<String> roles`）
- 第 57 行：`encodeAuthorities(principal.permissions(), "permissions")` —— 2 参数，期望返回 `List<String>`
- 第 78 行：`encodeAuthorities(roles)` —— 1 参数，期望返回 `String`（赋值给 `String encodedRoles`）
- 第 80 行：`encodeAuthorities(permissions)` —— 1 参数，期望返回 `String`

**定义点（已有方法）**：
- 第 114 行：`validateAuthorities(Collection<String> values, String name)` 返回 `List<String>` —— 已做校验、去重、长度检查
- 第 135 行：`encodedAuthorities(Set<String> authorities, String fieldName)` 返回 `String` —— 名字带 `d`（过去式），与调用方名字不一致；且功能与 `validateAuthorities` 重叠
- 第 90 行：`split(String encoded)` 返回 `List<String>` —— 未被任何地方调用，冗余

### 类型链路确认

- [CurrentUserPrincipal.java](file:///c:/Users/唐国几/IdeaProjects/yuegang-zhihui-ai1/ygh-platform/ygh-gateway/src/main/java/com/yuegang/zhihui/gateway/CurrentUserPrincipal.java#L8-L9)：`roles()` / `permissions()` 返回 `Set<String>`
- [InternalUserContextSignature.Metadata](file:///c:/Users/唐国几/IdeaProjects/yuegang-zhihui-ai1/ygh-common/ygh-common-security/src/main/java/com/yuegang/zhihui/common/core/security/InternalUserContextSignature.java#L65-L67)：构造器需要 `List<String>` 类型的 roles / permissions（紧凑构造器会做排序去重校验）

### 正确逻辑流

1. 从 principal 获取 `Set<String>`（roles / permissions）
2. 校验并转为 `List<String>`（用已有 `validateAuthorities`，返回排序去重后的不可变 List）
3. 将 `List<String>` 传给 `InternalUserContextSignature.Metadata` 签名
4. 将 `List<String>` 用逗号拼接为 `String`，设置到下游 Header

## 修复方案

### 修改文件

[TrustedUserContextFilter.java](file:///c:/Users/唐国几/IdeaProjects/yuegang-zhihui-ai1/ygh-platform/ygh-gateway/src/main/java/com/yuegang/zhihui/gateway/TrustedUserContextFilter.java)

#### 改动 1：第 56、57 行 —— 调用改为 `validateAuthorities`

将 2 参数的 `encodeAuthorities` 调用改为已有的 `validateAuthorities`，返回 `List<String>`：

```java
// 修改前（第 56 行）
List<String> roles = principal == null ? List.of() : encodeAuthorities(principal.roles(), "roles");
// 修改后
List<String> roles = principal == null ? List.of() : validateAuthorities(principal.roles(), "roles");
```

```java
// 修改前（第 57 行）
List<String> permissions = principal == null ? List.of() : encodeAuthorities(principal.permissions(), "permissions");
// 修改后
List<String> permissions = principal == null ? List.of() : validateAuthorities(principal.permissions(), "permissions");
```

#### 改动 2：新增 `encodeAuthorities(List<String>)` 单参数方法

在第 132 行（`validateAuthorities` 方法之后）新增一个简单的编码方法，将 List 拼接为逗号分隔字符串：

```java
/** 将权限/角色列表用逗号拼接为 Header 值 */
private static String encodeAuthorities(List<String> values) {
    return values.isEmpty() ? "" : String.join(",", values);
}
```

#### 改动 3：删除冗余的 `split` 方法（第 89-92 行）

`split(String)` 方法未被任何地方调用，且与正确的数据流（Set → validateAuthorities → List）无关，删除：

```java
// 删除以下代码（第 89-92 行）
/** 辅助方法：将逗号分隔的字符串拆分为列表 */
private static List<String> split(String encoded) {
    return encoded.isEmpty() ? List.of() : List.of(encoded.split(",", -1)); // 为空返回空列表，否则拆分
}
```

#### 改动 4：删除冗余的 `encodedAuthorities` 方法（第 134-151 行）

`encodedAuthorities(Set<String>, String)` 与 `validateAuthorities` 功能重叠，且名字带 `d` 导致调用方不匹配，删除：

```java
// 删除以下代码（第 134-151 行）
/** 将权限/角色集合编码为排序后的安全字符串 */
private static String encodedAuthorities(Set<String> authorities, String fieldName) {
    // ... 整个方法体
}
```

#### 改动 5：清理未使用的 import（第 20 行）

删除冗余方法后，`java.util.stream.Collectors` 不再被使用，删除该 import：

```java
// 删除（第 20 行）
import java.util.stream.Collectors;
```

## 假设与决策

- **决策 1**：保留 `validateAuthorities` 作为唯一的校验方法（它已实现去重、排序、安全正则校验、数量与长度限制），删除功能重叠的 `encodedAuthorities`。
- **决策 2**：新增 `encodeAuthorities(List<String>)` 单参数方法仅做简单 `String.join`，因为校验逻辑已在 `validateAuthorities` 中完成，编码步骤无需重复校验。
- **决策 3**：不修改 `validateAuthorities` 的签名，它接受 `Collection<String>` 已兼容 `Set<String>`（principal 返回类型）和 `List<String>`。
- **假设**：`InternalUserContextSignature.Metadata` 的紧凑构造器会再次对 roles/permissions 做排序去重校验，因此 `validateAuthorities` 的校验是幂等的、安全的。

## 验证步骤

1. 修改后检查文件中所有 `encodeAuthorities` 调用是否都有对应定义：
   - 第 56、57 行：改为 `validateAuthorities`（已存在）
   - 第 78、80 行：调用 `encodeAuthorities(List<String>)`（新增）
2. 确认 `split` 和 `encodedAuthorities(Set, String)` 已删除
3. 确认 `java.util.stream.Collectors` import 已删除
4. 在 IDEA 中重新编译该文件，确认 4 个 "无法解析方法" 错误全部消除
