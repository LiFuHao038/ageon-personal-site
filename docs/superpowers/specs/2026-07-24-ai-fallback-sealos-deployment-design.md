# AGEON AI 主备模型与 Sealos 部署设计

## 1. 目标

在不改变现有前端 AI 对话接口的前提下，提高模型回复可用性，并为低访问量个人网站准备可重复执行的上线方案。

本阶段交付结果：

- 默认使用阿里云百炼 `qwen-plus`。
- 主模型在可恢复故障下自动切换到 `kimi/kimi-k3`。
- 前端继续使用现有 SSE 接口，不接触模型 API Key。
- 前端、Spring Boot 后端和 MySQL 可部署到 Sealos Cloud。
- 生产环境具备健康检查、明确的环境变量和最小化运维文档。

## 2. 范围

### 2.1 包含

- 将现有单模型配置改为主模型与备用模型配置。
- 抽离通用的 OpenAI Compatible 流式客户端命名，避免业务代码继续绑定 Kimi。
- 在首个 Token 输出前处理模型切换。
- 区分可切换错误与不可切换错误。
- 限制同一用户同时只能进行一个 AI 生成任务。
- 保留每日 20 次额度，并在完整生成失败时释放额度。
- 增加前后端 Dockerfile、容器忽略文件、生产配置和 Sealos 部署说明。
- 增加后端健康检查端点，并用于容器健康检查。

### 2.2 不包含

- RAG、向量数据库和个人文档检索。
- LangChain4j、Agent、工具调用和长期记忆。
- 第三个模型供应商或独立 DeepSeek API Key。
- 自动扩缩容、消息队列、Redis 分布式锁和多实例并发协调。
- 自定义域名购买、ICP备案代办和支付功能。

## 3. 推荐架构

```text
Browser
  -> Next.js frontend container
  -> Spring Boot /api/v1/ai/.../messages/stream
  -> AI orchestration client
       -> primary: qwen-plus
       -> fallback: kimi/kimi-k3
  -> DashScope OpenAI-compatible API

Spring Boot
  -> Sealos MySQL with persistent volume
```

前端只调用 Spring Boot。模型 API Key、模型名称和降级规则只存在于后端环境变量中。

Sealos 上运行三个资源：

1. Next.js 前端容器。
2. Spring Boot 后端容器。
3. MySQL 8 持久化实例。

## 4. AI 模型配置

采用供应商无关的配置名称：

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `DASHSCOPE_API_KEY` | 无 | 百炼 API Key，仅后端配置 |
| `AI_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI Compatible API 地址 |
| `AI_PRIMARY_MODEL` | `qwen-plus` | 主模型 |
| `AI_FALLBACK_ENABLED` | `true` | 是否启用备用模型 |
| `AI_FALLBACK_MODEL` | `kimi/kimi-k3` | 备用模型 |
| `AI_CONNECT_TIMEOUT_SECONDS` | `10` | 建连超时 |
| `AI_RESPONSE_TIMEOUT_SECONDS` | `120` | 单次模型响应超时 |
| `AI_CONTEXT_WINDOW_TOKENS` | `128000` | Prompt 截断上限依据 |
| `AI_MAX_OUTPUT_TOKENS` | `2000` | 最大输出 Token |

为平滑迁移，第一版实现继续兼容已有 `KIMI_API_KEY`、`KIMI_BASE_URL`、`KIMI_MODEL` 等变量，但 README 和部署文档只推荐新的 `AI_*` 变量。

上下文窗口按主模型和备用模型中的较小值配置，避免主模型可接收而备用模型拒绝同一 Prompt。默认使用保守的 `128000`，部署前以百炼控制台实际开通模型规格为准。

## 5. 模型切换规则

### 5.1 可切换错误

仅在主模型尚未输出任何 Token 时，以下错误允许切换一次备用模型：

- HTTP `429`，包括 `EngineOverloadedError`。
- HTTP `500`、`502`、`503`、`504`。
- 建连失败。
- 响应超时。
- 上游在流开始前关闭连接。

### 5.2 不可切换错误

以下错误直接返回，不调用备用模型：

- HTTP `400`、`404`、`422`：模型名或请求参数错误。
- HTTP `401`、`403`：API Key 或模型权限错误。
- 用户主动断开 SSE。
- 主模型已经输出至少一个 Token 后发生异常。

已产生部分回答后禁止切换，避免两个模型的内容拼接在同一条回答中。

### 5.3 尝试次数

每次用户请求最多调用两个模型：主模型一次、备用模型一次。不增加同模型指数重试，避免用户等待时间过长和重复计费。

## 6. SSE 事件与前端行为

现有 `message`、`quota`、`done`、`error` 事件保持兼容，新增可选事件：

```text
event: model_status
data: {"status":"fallback","model":"kimi/kimi-k3"}
```

前端收到该事件后，在当前回答区域显示“主模型繁忙，正在切换备用模型…”。开始收到 `message` 后隐藏该状态。

用户点击发送后按钮立即禁用，直到收到 `done`、`error` 或连接终止。同一用户在后端同时只能有一个活动生成任务，即使其切换到另一个会话也不能并发调用模型。

## 7. 数据与额度一致性

- 用户消息在额度预留成功后保存，保持现有行为。
- 主模型失败并切换备用模型时，不重复扣减额度，也不重复保存用户消息。
- 任一模型完整生成成功后消费一次额度并保存一条助手消息。
- 两个模型都失败、用户断开或 SSE 超时后，释放本次额度预留。
- 失败记录保存最终错误信息，不保存 API Key 或完整上游响应体。

本阶段保持单个 Spring Boot 实例，因此使用 JVM 内存集合限制用户并发。未来扩展为多个后端实例时，再将并发锁迁移到 Redis。

## 8. 日志与错误信息

后端日志记录：

- 请求使用的模型名称。
- HTTP 状态码。
- 是否触发备用模型。
- 百炼 `request_id` 和截断后的错误摘要。

后端日志禁止记录：

- API Key。
- JWT。
- 用户完整 Prompt。
- 未截断的上游响应。

用户可见错误统一为中文：

- 两个模型均繁忙：`模型服务繁忙，请稍后再试`。
- 鉴权失败：`模型服务配置错误，请联系管理员`。
- 请求参数错误：`模型配置或请求参数不正确`。
- 用户并发请求：`已有回答正在生成，请稍后再试`。

## 9. Sealos 部署设计

### 9.1 前端容器

- 使用多阶段 Node.js 镜像构建 Next.js。
- 构建阶段注入 `NEXT_PUBLIC_API_BASE_URL`。
- 生产阶段运行 `next start`，监听平台提供的 `PORT`，默认 `3000`。
- 不把模型 Key 或数据库密码写入镜像。

### 9.2 后端容器

- 使用 Maven + JDK 21 多阶段构建。
- 生产阶段使用 JRE 21 运行 Spring Boot JAR。
- 激活 `mysql` Profile。
- 读取 Sealos Secret 中的数据库、JWT、管理员和 AI 配置。
- 健康检查访问 `/actuator/health`。

### 9.3 MySQL

- 使用 MySQL 8。
- 数据目录挂载持久化存储。
- 后端通过 Sealos 内网地址访问 MySQL，不公开数据库端口。
- 上线前导入现有建表 SQL，后续迁移到 Flyway。

### 9.4 公网与 CORS

- 前端和后端分别获得 Sealos 提供的 HTTPS 地址。
- `NEXT_PUBLIC_API_BASE_URL` 指向后端 HTTPS 地址。
- `AGEON_CORS_ALLOWED_ORIGINS` 配置为前端完整 Origin。
- 只公开前端和后端 HTTP 端口，不公开 MySQL。

平台默认域名和大陆区域可用性以创建应用时的 Sealos 控制台为准。若默认域名策略变化，部署结构不变，只需替换前后端公网域名。

## 10. 生产环境变量

后端必填：

```text
SPRING_PROFILES_ACTIVE=mysql
MYSQL_URL
MYSQL_USERNAME
MYSQL_PASSWORD
AGEON_JWT_SECRET
AGEON_ADMIN_USERNAME
AGEON_ADMIN_EMAIL
AGEON_ADMIN_PASSWORD
AGEON_CORS_ALLOWED_ORIGINS
DASHSCOPE_API_KEY
AI_PRIMARY_MODEL=qwen-plus
AI_FALLBACK_ENABLED=true
AI_FALLBACK_MODEL=kimi/kimi-k3
```

前端必填：

```text
NEXT_PUBLIC_API_BASE_URL=https://<backend-public-domain>
```

所有密钥必须配置在 Sealos Secret 或环境变量中，禁止提交 `.env`、API Key、数据库密码和 JWT Secret。

## 11. 测试策略

### 11.1 后端单元测试

- 主模型成功时不调用备用模型。
- 主模型返回 `429` 时切换备用模型。
- 主模型返回 `503` 或超时时切换备用模型。
- 主模型返回 `401`、`403`、`400` 时不切换。
- 主模型输出首个 Token 后失败时不切换。
- 主备模型都失败时返回统一错误并释放额度。
- 同一用户跨会话并发请求返回冲突错误。

### 11.2 前端契约测试

- 正确处理 `model_status` 事件。
- 切换状态不会覆盖已经收到的回答。
- 结束或失败后恢复发送按钮。

### 11.3 构建与容器验证

- `mvn test` 全部通过。
- `pnpm test` 和 `pnpm build` 通过。
- 前后端 Docker 镜像成功构建。
- 本地容器连接 MySQL 后，健康检查、登录、社区和 AI SSE 请求可用。

## 12. 验收标准

满足以下条件即完成本阶段：

1. 主模型发生引擎过载、服务端错误或超时时，系统能自动切换到配置的备用模型完成回答。
2. 主备切换不会重复扣减额度、重复保存消息或混合两个模型的输出。
3. 前端能够明确展示模型切换状态。
4. 项目不依赖本机环境即可通过容器构建。
5. 部署文档包含 Sealos 创建资源、配置环境变量、绑定公网地址、验证和回滚步骤。
6. 仓库中不存在明文 API Key、数据库密码和生产 JWT Secret。

## 13. 后续扩展

当前直接使用 JDK HttpClient 足以支持主备模型和 SSE。接入个人技术文档、向量检索、工具调用或复杂对话记忆时，再评估 LangChain4j；本阶段不引入该依赖，以控制复杂度和部署资源。
