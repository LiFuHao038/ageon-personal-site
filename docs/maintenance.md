# AGEON 维护与部署

## 环境变量

前端：

- `NEXT_PUBLIC_API_BASE_URL`：Spring Boot 公网地址，例如 `https://api.example.com`。

后端：

- `AGEON_JWT_SECRET`：至少 32 字节的随机密钥，生产环境必须修改。
- `AGEON_ADMIN_USERNAME`、`AGEON_ADMIN_EMAIL`、`AGEON_ADMIN_PASSWORD`：首次启动时创建管理员。
- `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`：使用 `mysql` Profile 时的数据库配置。
- `KIMI_API_KEY`：下一阶段 AI 服务使用，只能配置在后端服务器。
- `KIMI_BASE_URL`：默认 `https://api.moonshot.cn/v1`。
- `KIMI_MODEL`：按 Moonshot 控制台当前可用模型填写，不在代码中硬编码版本。

不要把 `.env`、API Key、数据库密码或 JWT Secret 提交到 Git。任何以 `NEXT_PUBLIC_` 开头的变量都会进入浏览器包，因此 Kimi Key 不能使用该前缀。

## 日常内容维护

- 首页文件夹内容：修改 `lib/profile-folders.ts`。
- 项目作品和题库数据：修改 `lib/site-data.ts`。
- 社区内容：管理员登录 `/admin` 后审核用户、发布问题、回复或删除内容。
- 新用户流程：注册为 `PENDING`，管理员改为 `APPROVED` 后才能登录。
- 新问题流程：提交为 `PENDING`，管理员改为 `PUBLISHED` 后才能公开访问。

## 数据库

本地开发使用 `backend/data/ageon-dev.mv.db`。公网部署使用 MySQL，并定期备份：

```powershell
mysqldump -u root -p ageon_site > ageon_site_backup.sql
mysql -u root -p ageon_site < ageon_site_backup.sql
```

升级实体前先备份数据库。当前使用 Hibernate `ddl-auto=update`，正式长期维护建议引入 Flyway 管理数据库迁移。

## 部署顺序

1. 创建 MySQL 数据库并配置后端环境变量。
2. 使用 `mysql` Profile 启动 Spring Boot，确认 `/api/v1/community/questions` 返回 JSON。
3. 设置前端 `NEXT_PUBLIC_API_BASE_URL` 并执行 `pnpm build`。
4. 配置域名和 HTTPS，再将实际前端域名加入后端 CORS 白名单。
5. 注册测试用户，完成“管理员审核用户 -> 用户提问 -> 管理员发布 -> 用户回复”的完整检查。

## 常见问题

- 前端端口被占用：关闭旧的 `next dev` 进程，并删除仅在进程已停止时残留的 `.next/dev/lock`。
- 页面请求失败：确认后端端口、`NEXT_PUBLIC_API_BASE_URL` 和 CORS 域名一致。
- 登录返回待审核：使用管理员端将用户状态改为 `APPROVED`。
- Kimi 请求失败：只检查后端的 `KIMI_API_KEY`、模型名、额度和服务日志，不要在浏览器中打印 Key。
