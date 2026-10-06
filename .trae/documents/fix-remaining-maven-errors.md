# 修复 IDEA 中剩余 48 个 Maven 解析错误

## 问题根因分析

经过探索，确认有 3 个独立的根本原因导致 IDEA 报 48 个错误：

### 根因 A：`${revision}` 占位符无法解析（影响最广）

- `ygh-common/pom.xml` 的 parent version 用 `${revision}`，但其 parent (yuegang-zhihui-ai1) 的 version 是字面量 `1.0.0-SNAPSHOT`
- `ygh-common` 的 7 个子模块的 parent version 也用 `${revision}`
- 项目**没有配置 `flatten-maven-plugin`**，导致 Maven/IDEA 在解析子模块时无法解析 parent 的 `${revision}` 占位符
- 错误现象：`Non-resolvable parent POM for com.yuegang.zhihui:ygh-common:${revision}`
- 连锁影响：整个 ygh-common 模块树无法识别 → ygh-gateway 依赖的 `ygh-common-redis`、`ygh-common-test` 找不到

**项目其他模块（ygh-dependencies、ygh-platform、ygh-deploy、ygh-applications、ygh-testes）全部使用字面量 `1.0.0-SNAPSHOT`，只有 ygh-common 系列用了 `${revision}`，不一致。**

### 根因 B：阿里云镜像连接超时（导致插件解析失败）

- 从本地仓库的 `.lastUpdated` 文件确认：`Connect to maven.aliyun.com:443 [...] failed: Connect timed out`
- IDEA 配置了阿里云镜像（ID: `aliyunmaven`，URL: `https://maven.aliyun.com/repository/public`）
- 本地仓库实际已有插件 jar/pom 文件，但 `.lastUpdated` 失败标记导致 Maven 跳过使用
- 受影响插件：maven-surefire-plugin:3.5.6、jacoco-maven-plugin:0.8.15、cyclonedx-maven-plugin:2.9.1、maven-deploy-plugin:3.1.4、maven-site-plugin:3.12.1、maven-resources-plugin:3.4.0

### 根因 C：IDEA `.idea` 配置残留错误模块名

- `.idea/compiler.xml` 中出现 `ygh-testes (1)`、`ygh-testes (2)` 重复模块（之前 ygh-applications 的 artifactId 错误导致）
- 缺少 `ygh-common-web`、`ygh-common-mybatis`、`ygh-common-mq`（因根因 A 导致无法识别）
- 残留 `untitled` 模块（根 pom 的 modules 列表中没有此模块）

## 修复方案

### 修复 1：统一 parent version 为字面量（解决根因 A）

将 ygh-common 及其 7 个子模块中所有 `${revision}` 改为 `1.0.0-SNAPSHOT`，与项目其他模块保持一致。

涉及文件（共 8 个，9 处修改）：

1. **`ygh-common/pom.xml`**（2 处）
   - 第 6 行：parent version `${revision}` → `1.0.0-SNAPSHOT`
   - 第 17 行：BOM import version `${revision}` → `1.0.0-SNAPSHOT`

2. **`ygh-common/ygh-common-core/pom.xml`**（1 处）
   - 第 6 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

3. **`ygh-common/ygh-common-web/pom.xml`**（1 处）
   - 第 7 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

4. **`ygh-common/ygh-common-security/pom.xml`**（1 处）
   - 第 6 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

5. **`ygh-common/ygh-common-mybatis/pom.xml`**（1 处）
   - 第 7 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

6. **`ygh-common/ygh-common-redis/pom.xml`**（1 处）
   - 第 7 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

7. **`ygh-common/ygh-common-mq/pom.xml`**（1 处）
   - 第 7 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

8. **`ygh-common/ygh-common-test/pom.xml`**（1 处）
   - 第 7 行：parent version `${revision}` → `1.0.0-SNAPSHOT`

### 修复 2：清理 IDEA 残留配置（解决根因 C）

删除整个 `.idea` 目录，让 IDEA 重新生成干净的配置：
- 路径：`c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\.idea`
- 此操作安全：`.idea` 中的配置都会由 IDEA 基于 pom.xml 重新生成
- 用户需要关闭 IDEA → 删除 .idea → 重新打开项目

### 修复 3：处理阿里云镜像网络问题（解决根因 B）

**此问题需要用户介入，无法通过代码修复。** 提供以下建议（择一）：

1. **检查网络**：确认能否访问 `https://maven.aliyun.com/repository/public`（可能是临时网络故障）
2. **清理失败标记**：删除本地仓库中所有 `.lastUpdated` 文件，让 Maven 重新尝试使用已下载的文件
   - 命令：在 `c:\Users\唐国几\.m2\repository` 下递归删除 `*.lastUpdated` 文件
3. **配置 Maven settings.xml**：在 `c:\Users\唐国几\.m2\settings.xml` 中配置可用镜像（阿里云不可用时可选其他镜像）

## 假设与决策

- **决策 1**：选择字面量 `1.0.0-SNAPSHOT` 而非引入 `flatten-maven-plugin`
  - 理由：项目其他模块已统一用字面量；flatten 插件增加复杂度；符合用户"避免冗余"偏好
- **决策 2**：删除整个 `.idea` 而非精准清理 compiler.xml
  - 理由：`.idea` 中可能还有其他残留（workspace.xml 等），整体重置最干净
- **假设**：用户的 JDK 25 和 Maven 3.9.16+ 已正确配置（enforcer 要求），否则构建仍会失败

## 执行步骤

1. 修改 8 个 pom.xml 文件中的 9 处 `${revision}` → `1.0.0-SNAPSHOT`
2. 删除 `c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\.idea` 目录
3. 提示用户：关闭 IDEA → 删除 .idea 后重新打开 → 在 Maven 面板点击刷新 → 如插件仍报错则清理 `.lastUpdated` 文件并检查网络

## 验证步骤

1. 代码修改后，用 Grep 确认项目中不再有 `${revision}` 引用
2. 用户在 IDEA 重新打开项目后，确认：
   - Maven 面板能正确显示所有模块（ygh-common-core、ygh-common-web、ygh-common-security、ygh-common-mybatis、ygh-common-redis、ygh-common-mq、ygh-common-test）
   - 不再有 `Non-resorable parent POM` 错误
   - 不再有 `ygh-common-redis`、`ygh-common-test` 未解析错误
3. 如插件解析错误仍存在，按修复 3 处理网络问题
