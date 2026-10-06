# 跨境智汇 AI 系统 项目说明

> 项目名称：粤港甄选跨境智汇 AI 企业级分布式系统
> 版本：1.0.0-SNAPSHOT
> 构建工具：Maven 多模块（聚合根 `com.yuegang.zhihui:yuegang-zhihui-ai1`）
> 运行时：Java 25 + Spring Boot 4.0.7 + Spring Cloud 2025.1.2 + Spring Cloud Alibaba 2025.1.0.0

---

## 一、项目可行性分析

### 1.1 业务可行性

本项目定位为面向跨境电商企业的智能化运营平台，覆盖企业核心业务场景：

- **用户与组织管理**：支持客户、员工、部门、岗位的多维度组织建模，RBAC 权限体系满足企业级权限管控需求。
- **商品与库存**：商品分类、品牌、SKU 管理结合库存服务，支撑跨境电商商品运营。
- **订单与支付**：购物车、订单全流程管理，配套虚拟钱包实现企业内部资金流转。
- **AI 智能客服**：基于 RAG（检索增强生成）架构，融合企业知识库与大模型通用知识，提供专业合规的客服问答。
- **知识库与培训**：企业知识沉淀（文档审核、向量检索）与员工培训闭环（课程、测验、学习追踪）。
- **消息通知**：统一通知服务支持多渠道触达。

**结论**：业务场景完整、闭环清晰，具备企业实际落地的业务价值。

### 1.2 技术可行性

| 维度 | 技术选型 | 可行性依据 |
|------|---------|-----------|
| 开发语言 | Java 25（最新 LTS） | 生态成熟，虚拟线程、模式匹配等特性提升并发与代码简洁性 |
| 应用框架 | Spring Boot 4.0.7 + Spring Cloud | 业界主流微服务栈，社区活跃，文档完善 |
| 服务治理 | Spring Cloud Alibaba（Nacos） | 服务注册发现、配置中心一体化，国产化友好 |
| 数据存储 | MySQL 8.4 + Redis 7 | 关系型数据 + 缓存/会话的经典组合 |
| 搜索引擎 | Elasticsearch | 支持全文检索与向量检索，满足 RAG 检索需求 |
| 消息队列 | RocketMQ 5.x（可选） | 高可靠消息中间件，支撑异步解耦 |
| AI 能力 | DeepSeek / 豆包方舟（OpenAI 兼容协议） | 多供应商抽象，支持动态切换，避免供应商锁定 |
| 安全机制 | JWT（RS256）+ HMAC-SHA256 + AES-GCM | 端到端身份认证、内部服务签名、敏感数据加密 |
| 前端 | Vue 3 + Vite + pnpm Monorepo | 前后端分离，admin 与 mall 双端独立部署 |
| 容器化 | Docker + Docker Compose | 支持 VM 部署与企业级服务器部署两种模式 |

**结论**：技术栈均为成熟、主流、有大规模生产验证的方案，技术风险可控。

### 1.3 工程可行性

- **模块化设计**：Maven 多模块清晰划分 `ygh-dependencies`（BOM）、`ygh-common`（公共组件）、`ygh-platform`（基础设施）、`ygh-applications`（业务服务）、`ygh-deploy`（部署前端）、`ygh-testes`（测试体系），职责边界明确。
- **质量保障**：强制 JaCoCo 覆盖率门槛（行覆盖 ≥70%、分支覆盖 ≥60%），Maven Enforcer 校验构建基线，CycloneDX 支持 SBOM 输出。
- **测试体系**：`ygh-testes` 下细分兼容性测试、契约测试、E2E 测试、集成测试、性能测试、安全测试六大类。
- **部署能力**：提供 VM 部署（`constrained-dev`）与企业服务器部署（`enterprise-server`）两套 Compose 编排，适配不同交付环境。
- **可观测性**：内置 Prometheus + Loki + Grafana + Alertmanager 可观测栈，Actuator 暴露 metrics 与健康探测。

**结论**：工程化程度高，具备持续交付与生产运维能力。

---

## 二、项目架构

### 2.1 总体架构

系统采用**微服务 + API 网关**架构，所有外部请求统一经由网关鉴权后路由至后端服务，服务间通过 Nacos 服务发现 + 内部签名机制通信。

```
                         ┌─────────────────────────────────────────┐
                         │            前端（Vue 3 双端）            │
                         │  ygh-web-admin（管理后台）              │
                         │  ygh-web-mall（商城前台）               │
                         └───────────────────┬─────────────────────┘
                                             │ HTTPS
                         ┌───────────────────▼─────────────────────┐
                         │          ygh-gateway (8080)             │
                         │  Spring Cloud Gateway (WebFlux 响应式)  │
                         │  · JWT 会话校验（Redis）                │
                         │  · 路由转发（lb:// 负载均衡）           │
                         │  · CORS / 限流(Sentinel) / 请求体限制   │
                         └───────────────────┬─────────────────────┘
                                             │
        ┌────────────────────────────────────┼────────────────────────────────────┐
        │                                    │                                    │
   ┌────▼────┐  ┌────▼────┐  ┌────▼────┐  ┌──▼───┐  ┌────▼────┐  ┌────▼────┐  ┌──▼─────┐
   │  auth   │  │  user   │  │ system  │  │product│  │inventory│  │  order  │  │ wallet  │
   │  8081   │  │  8082   │  │  8083   │  │ 8084  │  │  8087   │  │  8088   │  │  8091   │
   └────┬────┘  └────┬────┘  └────┬────┘  └──┬───┘  └────┬────┘  └────┬────┘  └────┬────┘
        │            │            │            │           │            │            │
   ┌────▼────┐  ┌────▼────┐  ┌────▼────┐  ┌──▼───┐  ┌────▼────┐  ┌────▼────┐  ┌──▼─────┐
   │knowledge│  │   ai    │  │ search  │  │training│  │notification│  │ admin  │
   │  8085   │  │  8090   │  │  8092   │  │ 8089  │  │  8093   │  │  8094   │
   └─────────┘  └─────────┘  └─────────┘  └───────┘  └─────────┘  └─────────┘
        │            │            │
        │      ┌─────▼─────┐      │
        │      │ DeepSeek  │      │
        │      │ API(外部) │      │
        │      └───────────┘      │
        └──────────────────────────┘
                  RAG 检索链路

   基础设施：MySQL 8.4 · Redis 7 · Nacos · Elasticsearch · RocketMQ(可选)
```

### 2.2 模块划分

```
yuegang-zhihui-ai1 (聚合根)
├── ygh-dependencies          # BOM 依赖版本统一管理
├── ygh-common                # 公共组件层
│   ├── ygh-common-core       #   核心模型：ApiResponse / ErrorCode / BusinessException
│   ├── ygh-common-web        #   Web 支持：全局异常处理 / TraceId / OpenAPI
│   ├── ygh-common-security   #   安全：InternalUserContextSignature / InternalServiceSignature
│   ├── ygh-common-redis      #   Redis 封装：分布式锁 / 会话
│   ├── ygh-common-mybatis    #   MyBatis-Plus 配置
│   └── ygh-common-mq         #   RocketMQ 封装（幂等消费）
├── ygh-platform              # 平台基础设施层
│   ├── ygh-gateway           #   API 网关（WebFlux 响应式）
│   └── ygh-auth-service      #   认证服务（JWT 签发 / JWKS / 验证码）
├── ygh-applications          # 业务服务层（13 个业务服务）
│   ├── ygh-user              #   用户服务（8082）
│   ├── ygh-system            #   系统配置服务（8083）
│   ├── ygh-product           #   商品服务（8084）
│   ├── ygh-knowledge         #   知识库服务（8085）
│   ├── ygh-inventory         #   库存服务（8087）
│   ├── ygh-order             #   订单服务（8088）
│   ├── ygh-training          #   培训服务（8089）
│   ├── ygh-ai                #   AI 智能客服服务（8090）
│   ├── ygh-wallet            #   钱包服务（8091）
│   ├── ygh-search            #   搜索服务（8092）
│   ├── ygh-notification      #   通知服务（8093）
│   ├── ygh-admin             #   后台聚合服务（8094）
│   └── (每个服务拆分 *-api / *-service 子模块)
├── ygh-deploy                # 部署层
│   └── ygh-web               #   前端工程（pnpm monorepo：admin + mall + shared）
└── ygh-testes                # 测试体系（契约/集成/E2E/性能/安全/兼容性）
```

### 2.3 安全架构

系统采用**三层安全防护**：

1. **外部请求认证（网关层）**
   - JWT（RS256 非对称签名）验证用户身份
   - JWKS 端点动态获取公钥，支持密钥轮换
   - Redis 会话状态校验，支持主动注销/踢人

2. **内部服务签名（服务间）**
   - `InternalServiceSignature`：HMAC-SHA256 签名，绑定服务名 + 方法 + 路径 + 时间戳，防篡改防重放（30 秒时间窗口）
   - `InternalUserContextSignature`：网关将已认证用户身份（userId/roles/permissions）签名后透传给下游，下游验签后无需重复解析 JWT

3. **敏感数据加密（存储层）**
   - `SystemSecretCipher`：AES-GCM 加密敏感配置（如 AI 供应商 API Key）
   - 密文 + 随机 Nonce 存储，主密钥通过环境变量注入，不落库

### 2.4 数据架构

- **分库设计**：每个服务独立数据库（`auth_db` / `user_db` / `system_db` / `ai_db` / `product_db` 等），物理隔离避免耦合
- **数据库迁移**：Flyway 统一管理，每个服务 `classpath:db/migration` 维护版本化脚本（如 ai_db 已迁移至 V7）
- **数据库账号分离**：每个服务区分 `*_migration`（DDL 权限）与 `*_app`（DML 权限）两套账号，最小权限原则

---

## 三、项目功能介绍

### 3.1 认证服务（ygh-auth-service，端口 8081）

- **用户注册**：图形验证码校验 + 密码复杂度策略（长度/大小写/数字/特殊字符/泄露库检查）
- **用户登录**：账号密码 + 可选验证码，签发 JWT Access Token + Refresh Token
- **令牌管理**：JWKS 端点暴露公钥，支持密钥轮换；Refresh Token 续期
- **安全审计**：登录失败计数、账号锁定、登录尝试记录

### 3.2 用户服务（ygh-user，端口 8082）

- **用户档案**：客户/员工基本信息管理
- **组织架构**：部门、岗位、员工岗位关联（RBAC 数据基础）
- **用户地址**：收货地址管理

### 3.3 系统服务（ygh-system，端口 8083）

- **系统字典**：数据字典维护
- **系统配置**：包括 AI 供应商配置（provider/baseUrl/chatModel/apiKey 加密存储/version 版本化）
- **角色权限**：角色与权限的分配管理
- **内部接口**：`/internal/v1/system/ai-provider-config` 供 AI 服务拉取供应商配置

### 3.4 商品服务（ygh-product，端口 8084）

- 商品 CRUD、分类管理、品牌管理
- 商品图片上传

### 3.5 库存服务（ygh-inventory，端口 8087）

- SKU 库存查询与调整
- 内部接口 `/internal/v1/inventory/{sku}` 供 AI 客服查询实时库存

### 3.6 订单服务（ygh-order，端口 8088）

- 购物车、订单创建与全流程管理
- 后台订单管理

### 3.7 钱包服务（ygh-wallet，端口 8091）

- 虚拟资金账户管理
- 充值、支付、余额查询

### 3.8 知识库服务（ygh-knowledge，端口 8085）

- 文档上传与审核流程
- 文档分块与向量化（为 RAG 提供知识来源）
- 可见性分级：PUBLIC / INTERNAL / CONFIDENTIAL

### 3.9 AI 智能客服服务（ygh-ai，端口 8090）⭐ 核心亮点

基于 **RAG（检索增强生成）架构**，融合企业知识库与大模型能力：

- **多供应商抽象**：`DynamicModelGateway` 根据数据库配置动态选择模型网关
  - `OpenAiCompatibleModelGateway`：支持 DeepSeek 等 OpenAI 兼容协议供应商
  - `DoubaoModelGateway`：支持豆包方舟 Responses API
- **检索链路**：用户提问 → `GovernedRetrievalGateway`（安全策略校验）→ `HttpRetrievalGateway`（调用 search 服务混合检索）→ 返回知识库片段
- **工具调用**：`CommerceToolGateway` 自动识别问题中的 SKU/订单号，调用商品/库存/订单服务获取实时业务数据作为上下文
- **提示词编排**：系统提示词约束模型区分"已审核知识库 / 只读业务数据 / 模型通用知识 / 互联网来源"，未命中知识库时明确标注"需人工核验"
- **容错降级**：检索服务/工具服务不可用时自动降级，AI 仍可基于通用知识回答；网络异常转译为友好错误
- **流式输出**：支持 SSE 流式响应（`/api/v1/ai/chat/stream`），分段返回提升体验
- **会话持久化**：对话、消息、引用来源、工具调用、RAG 追踪信息均落库，支持审计与优化
- **可见性控制**：根据用户角色（ADMIN/EMPLOYEE）动态决定可检索的知识库范围

### 3.10 搜索服务（ygh-search，端口 8092）

- 混合检索（词法 + 向量）`/internal/v1/search/hybrid`
- 基于 Elasticsearch，支持知识库文档检索
- 按可见性分级过滤

### 3.11 培训服务（ygh-training，端口 8089）

- 课程管理、章节、文档附件
- 测验与答题、学习心跳追踪

### 3.12 通知服务（ygh-notification，端口 8093）

- 统一消息通知管理

### 3.13 后台聚合服务（ygh-admin，端口 8094）

- 管理仪表盘数据聚合
- 审计日志查询

### 3.14 API 网关（ygh-gateway，端口 8080）

- **路由转发**：基于路径前缀路由至 12 个后端服务，`lb://` 负载均衡
- **安全过滤**：`GatewayRequestGuardFilter` 请求体大小限制、`JwtSessionValidationFilter` JWT 校验
- **限流**：Sentinel 认证接口 20 QPS / 业务接口 100 QPS
- **CORS**：可配置跨域来源
- **优雅停机**：20 秒超时，确保存量请求处理完成

### 3.15 前端工程（ygh-web）

- **ygh-web-admin**：管理后台（Vue 3 + Vite + TypeScript）
- **ygh-web-mall**：商城前台（Vue 3 + Vite + TypeScript）
- **ygh-web-shared**：公共包（HTTP 客户端、认证、SSE、类型定义）

---

## 四、部署形态

### 4.1 本地开发部署（VM 模式）

- 中间件部署于虚拟机（MySQL / Redis / Nacos / Elasticsearch）
- 各服务通过 PowerShell 脚本（`start-*.ps1`）启动，环境变量注入配置
- 前端通过 `pnpm dev` 本地开发

### 4.2 企业服务器部署

- Docker Compose 编排全部服务与中间件
- 支持 Nacos 配置中心统一管理
- 内置可观测栈（Prometheus + Loki + Grafana + Alertmanager）

---

## 五、技术亮点总结

1. **AI 多供应商抽象**：通过数据库版本化配置动态切换模型供应商，新增供应商只需实现 `ModelGateway` 接口，无需改业务代码。
2. **RAG + 工具调用融合**：AI 客服不仅检索知识库，还能调用业务系统获取实时数据（库存、订单），回答更精准。
3. **端到端安全**：外部 JWT + 内部 HMAC 签名 + 敏感数据 AES-GCM 加密，三层防护覆盖全链路。
4. **工程化质量门禁**：JaCoCo 覆盖率强制、Maven Enforcer 构建基线、CycloneDX SBOM 输出，保障交付质量。
5. **容错设计**：AI 服务对检索、工具调用、模型网络异常均做了优雅降级，核心问答功能高可用。
