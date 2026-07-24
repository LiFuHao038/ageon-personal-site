# AGEON Sealos 部署指南

本方案面向低访问量个人网站，在 Sealos 中运行 Next.js、Spring Boot 和 MySQL 8。模型 API Key、JWT Secret 和数据库密码只配置在后端环境变量中。

## 1. 准备代码仓库

将项目推送到 GitHub。仓库可以设为私有；在 Sealos 中授权读取该仓库，或使用 GitHub Actions 构建镜像后推送到镜像仓库。

禁止提交 `.env`、API Key、JWT Secret、数据库密码、H2 数据文件、`node_modules` 和构建产物。

## 2. 创建 MySQL 8

1. 在 Sealos 应用管理中创建 MySQL 8。
2. 为 `/var/lib/mysql` 挂载持久化存储。
3. 创建数据库 `ageon_site` 和独立应用用户。
4. 只开放集群内网连接，不创建 MySQL 公网入口。
5. 记录内网主机名、端口、用户名和密码，后续写入后端 Secret。

Spring Boot 首次连接空数据库时，由 Flyway 自动执行 `V1__baseline.sql`，禁止在生产环境手工执行建表 SQL。

## 3. 部署后端

以 `backend` 为 Docker 构建上下文，Dockerfile 为 `backend/Dockerfile`。容器端口为 `8080`。

后端环境变量：

```text
SPRING_PROFILES_ACTIVE=mysql
MYSQL_URL=jdbc:mysql://<mysql-internal-host>:3306/ageon_site?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=<secret>
MYSQL_PASSWORD=<secret>
AGEON_JWT_SECRET=<至少32字节随机值>
AGEON_ADMIN_USERNAME=<管理员用户名>
AGEON_ADMIN_EMAIL=<管理员邮箱>
AGEON_ADMIN_PASSWORD=<高强度密码>
AGEON_CORS_ALLOWED_ORIGINS=https://<frontend-public-domain>
DASHSCOPE_API_KEY=<百炼密钥>
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_PRIMARY_MODEL=qwen-plus
AI_FALLBACK_ENABLED=true
AI_FALLBACK_MODEL=kimi/kimi-k3
AI_CONTEXT_WINDOW_TOKENS=128000
AI_MAX_OUTPUT_TOKENS=2000
```

将敏感变量放入 Sealos Secret，不要写入 Dockerfile 或仓库。为后端创建 HTTPS 公网入口，并配置探针：

```text
Path: /actuator/health
Port: 8080
Initial delay: 40 seconds
Period: 30 seconds
Timeout: 5 seconds
```

## 4. 部署前端

Next.js 的公开变量在构建时写入浏览器包，必须使用 Docker Build Argument：

```text
NEXT_PUBLIC_API_BASE_URL=https://<backend-public-domain>
```

等价 Docker 命令：

```powershell
docker build `
  --build-arg NEXT_PUBLIC_API_BASE_URL="https://<backend-public-domain>" `
  -t ageon-web:latest .
```

运行阶段再修改 `NEXT_PUBLIC_API_BASE_URL` 不会生效，修改后必须重新构建前端镜像。前端容器端口为 `3000`，创建 HTTPS 公网入口后，将其完整 Origin 写回后端 `AGEON_CORS_ALLOWED_ORIGINS` 并重新部署后端。

## 5. 上线验证

依次检查：

1. `https://<backend-public-domain>/actuator/health` 返回 `{"status":"UP"}`。
2. 首页、注册、登录、管理员审核和社区接口可用。
3. AI 请求正常收到 `message`、`quota`、`done` SSE 事件。
4. 模拟主模型 `429` 时收到 `model_status`，随后由备用模型继续回答。
5. 同一用户并发发送时显示“已有回答正在生成，请稍后再试”。
6. 两个模型均失败后，每日额度没有增加。

## 6. 备份与升级

升级前使用 Sealos 数据库备份功能或 `mysqldump` 备份 `ageon_site`。数据库结构只通过新的 Flyway 版本文件升级，例如 `V2__add_article_tables.sql`；已经执行过的迁移文件禁止修改。

## 7. 回滚

应用回滚使用上一版前端和后端镜像。若新版本只增加向后兼容字段，保留数据库迁移；不要直接删除 Flyway 历史记录。涉及破坏性 DDL 时，先恢复数据库备份，再回滚应用镜像。

## 8. 默认域名与备案

可以先使用 Sealos 提供的 HTTPS 公网地址进行小范围访问。平台区域、默认域名和大陆访问策略可能变化，创建应用时以控制台显示为准。后续绑定中国大陆自定义域名时，再按云厂商要求完成 ICP 备案。
