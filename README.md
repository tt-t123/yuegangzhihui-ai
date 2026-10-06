# 粤港甄选跨境智汇 AI（yuegang-zhihui-ai）

面向跨境电商企业的**企业级分布式系统**：微服务 + API 网关 + RAG 智能客服 + 企业知识库 + 员工培训。

> 详细设计说明见 [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)

## 技术栈

| 维度 | 选型 |
| --- | --- |
| 语言 / 运行时 | Java 25（record、虚拟线程、模式匹配） |
| 应用框架 | Spring Boot 4.0.7 + Spring Cloud 2025.1.2 |
| 服务治理 | Spring Cloud Alibaba 2025.1.0.0 + Nacos（服务注册发现） |
| 网关 | Spring Cloud Gateway（WebFlux 响应式） |
| 数据存储 | MySQL 8.4（业务）+ PostgreSQL/pgvector（1024 维向量检索） |
| 缓存 / 会话 | Redis 7（会话、幂等键、限流） |
| 搜索 | Elasticsearch（BM25 + 向量混合检索） |
| 消息 | RocketMQ 5.3.1（领域事件，默认关闭可启用） |
| 认证与安全 | Nimbus JOSE+JWT（RS256 + JWKS）、BouncyCastle、Argon2id |
| 大模型 | DeepSeek / 豆包方舟（OpenAI 兼容协议，多供应商动态切换） |
| 文档解析 / 向量化 | Apache Tika 3.3.1、LangChain4j 1.17.2 |
| 前端 | Vue 3 + Vite + TypeScript + pnpm Monorepo |
| 部署 / 可观测 | Docker Compose、Prometheus + Loki + Grafana + Alertmanager |

## 模块结构

```
yuegang-zhihui-ai1
├── ygh-dependencies        # BOM：依赖版本统一治理
├── ygh-common              # 公共组件：core / web / security / redis / mybatis / mq / test
├── ygh-platform            # 基础设施：ygh-gateway（网关）、ygh-auth-service（认证）
├── ygh-applications        # 业务服务（每个服务拆分 *-api / *-service）
│   ├── ygh-user            #   用户与组织架构（RBAC 基础）
│   ├── ygh-system          #   字典、系统配置、角色权限、AI 供应商配置
│   ├── ygh-product         #   商品 SPU/SKU、类目品牌、批次溯源
│   ├── ygh-inventory       #   库存预占 / 确认 / 释放与对账
│   ├── ygh-order           #   购物车、订单状态机、支付确认
│   ├── ygh-wallet          #   虚拟钱包：充值、支付、退款、账本
│   ├── ygh-knowledge       #   知识文档上传 / 解析 / 审核 / 索引
│   ├── ygh-search          #   混合检索（词法 + 向量）
│   ├── ygh-ai              #   AI 智能客服（RAG + Agent 工具调用）
│   ├── ygh-training        #   课程、章节、测验、学习进度
│   ├── ygh-notification    #   站内通知：模板、派发、重试、死信重放
│   └── ygh-admin           #   后台仪表盘聚合、审计日志查询
├── ygh-deploy              # 部署层：Docker Compose 编排 + ygh-web 前端工程
└── ygh-testes              # 测试体系：契约 / 集成 / E2E / 性能 / 安全 / 兼容性
```

## 架构概览

```
前端（Vue 3：ygh-web-admin / ygh-web-mall）
        │ HTTPS
        ▼
ygh-gateway  ——  JWT 会话校验（Redis）· 路由转发（lb://）· CORS / 限流（Sentinel）
        │
        ├── auth / user / system / product / inventory / order / wallet
        └── knowledge / ai / search / training / notification / admin
                 │                    │
                 │            RAG 检索链路（knowledge → search → ai）
                 └── DeepSeek / 豆包方舟（外部大模型）
基础设施：MySQL · Redis · Nacos · Elasticsearch · PostgreSQL/pgvector · RocketMQ
```

## 核心亮点

1. **AI 多供应商抽象**：`DynamicModelGateway` 依据数据库版本化配置动态选择模型网关，新增供应商只需实现 `ModelGateway` 接口，业务代码零改动。
2. **RAG + 工具调用融合**：检索链路为「提问 → 安全策略校验 → Elasticsearch 混合检索（词法 + 向量）→ 上下文构建 → LLM 生成」；`CommerceToolGateway` 自动识别 SKU / 订单号并调用商品、库存、订单服务获取实时数据。
3. **端到端三层安全**：外部请求 JWT（RS256 + JWKS 动态轮换）鉴权 → 服务间 HMAC-SHA256 签名（防篡改、防重放，30 秒时间窗）→ 敏感配置 AES-GCM 加密存储。
4. **容错降级**：检索服务 / 工具服务不可用时 AI 自动降级为通用知识回答，网络异常转译为友好错误，核心问答高可用。
5. **工程化质量门禁**：JaCoCo 行覆盖 ≥70% / 分支覆盖 ≥60% 强制校验、Maven Enforcer 构建基线、CycloneDX SBOM 输出。
6. **分布式一致性**：跨服务采用「本地事务 + Outbox 发件箱 + Inbox 幂等 + 定时对账」实现最终一致，未引入 Seata 等重型分布式事务框架。

## 快速开始

```bash
# 1. 后端：构建全部模块
mvn -DskipTests package

# 2. 前端：管理后台与商城前台
cd ygh-deploy/ygh-web
pnpm install
pnpm dev
```

中间件（MySQL / Redis / Nacos / Elasticsearch / PostgreSQL）通过 `ygh-deploy` 下的 Docker Compose 编排启动。

## 配置说明

所有敏感配置均通过**环境变量**注入，代码与仓库中不含任何真实口令或密钥：

- 服务数据库 / Nacos / Redis 等连接参数：见各服务 `application.yml` 中的 `${YGH_*}` 占位符
- 部署环境变量清单：`ygh-deploy/constrained-dev/.env.example`、`ygh-deploy/enterprise-server/.env.example`
- 本地开发请自行复制为 `.env` 并填写真实值（`.env`、`secrets/` 已在 `.gitignore` 中排除）

## 构建与质量

```bash
mvn verify              # 编译 + 单元测试 + JaCoCo 覆盖率门禁
mvn -Prelease-sbom package   # 附带 CycloneDX SBOM
```

要求：Java 25、Maven 3.9.16+、Node.js 20.19+ / 22.12+、pnpm 10.13.1
