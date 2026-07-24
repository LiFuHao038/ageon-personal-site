# AGEON Personal Site

个人技术博客与问答社区，前端使用 Next.js 16，后端使用 Spring Boot 3、JWT、JPA 和 H2/MySQL。

## 当前页面

- `/`：粒子背景、个人 FOLDERS、项目和功能入口。
- `/community`：公开问题列表；审核用户可提问、回复和点赞。
- `/community/{id}`：问题详情与完整回复。
- `/ai`：已接入千问主模型、Kimi 备用模型、SSE 流式问答、会话历史和每日额度。
- `/interview`：计算机网络题库与模拟面试。
- `/auth`：普通用户注册与登录，注册后等待管理员审核。
- `/admin/login`、`/admin`：管理员登录与审核控制台。

## 启动

后端需要 Java 21。项目默认通过阿里云百炼使用 `qwen-plus`，在 429、503 或超时且尚未输出首个 Token 时自动切换到 `kimi/kimi-k3`：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:DASHSCOPE_API_KEY="<阿里云百炼 API Key>"
$env:AGEON_JWT_SECRET="<至少 32 字节随机值>"
$env:AGEON_ADMIN_USERNAME="<管理员用户名>"
$env:AGEON_ADMIN_EMAIL="<管理员邮箱>"
$env:AGEON_ADMIN_PASSWORD="<高强度管理员密码>"
cd backend
mvn clean package -DskipTests
java -jar .\target\ageon-api-0.1.0.jar
```

必须确认 `mvn -version` 显示 `Java version: 21`。如果 PowerShell 提示找不到 `mvn`，请先把 Maven 的 `bin` 目录加入 `PATH`，或在 IDEA 中使用 Maven 面板执行 `clean` 和 `package`，再运行上面的 `java -jar` 命令。中文用户目录下不建议使用 `mvn spring-boot:run`，其 Windows 类路径参数文件可能出现编码错误。

需要覆盖默认配置时可设置：

```powershell
$env:AI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:AI_PRIMARY_MODEL="qwen-plus"
$env:AI_FALLBACK_ENABLED="true"
$env:AI_FALLBACK_MODEL="kimi/kimi-k3"
$env:AI_CONTEXT_WINDOW_TOKENS="128000"
```

再启动前端：

```powershell
$env:NEXT_PUBLIC_API_BASE_URL="http://localhost:8080"
pnpm dev
```

默认前端为 `http://localhost:3000`，后端为 `http://localhost:8080`。

## 配置

正式部署前必须设置 `AGEON_JWT_SECRET`、`AGEON_ADMIN_USERNAME`、`AGEON_ADMIN_EMAIL`、`AGEON_ADMIN_PASSWORD`。百炼 Key 仅通过后端环境变量 `DASHSCOPE_API_KEY` 接入，禁止使用任何 `NEXT_PUBLIC_` 前缀暴露模型密钥。

Sealos 部署见 `docs/deployment-sealos.md`，维护说明见 `docs/maintenance.md`，接口说明见 `docs/api-auth-admin.md`。

## 验证

```powershell
pnpm test
.\node_modules\.bin\tsc.cmd --noEmit
pnpm build
```

后端：

```powershell
mvn test
```
