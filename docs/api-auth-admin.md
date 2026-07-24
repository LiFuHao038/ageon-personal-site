# 认证与管理 API

所有请求使用 JSON。受保护接口通过 `Authorization: Bearer <token>` 发送 JWT。

## 认证

- `POST /api/v1/auth/register`：注册普通用户，返回 `PENDING`，不返回 Token。
- `POST /api/v1/auth/login`：已审核用户或管理员登录。
- `GET /api/v1/auth/me`：读取当前用户。

## 社区

- `GET /api/v1/community/questions`：公开问题列表，只返回 `PUBLISHED`。
- `GET /api/v1/community/questions/{id}`：公开问题详情。
- `POST /api/v1/community/questions`：登录用户提交问题，初始为 `PENDING`。
- `POST /api/v1/community/questions/{id}/replies`：登录用户回复已发布问题。
- `POST /api/v1/community/questions/{id}/likes`：登录用户点赞。

## 管理端

- `GET /api/v1/admin/overview`：待审核用户、问题和回复统计。
- `GET /api/v1/admin/users`：用户列表，可使用 `status` 筛选。
- `PATCH /api/v1/admin/users/{id}/status`：设置 `PENDING`、`APPROVED`、`REJECTED` 或 `DISABLED`。
- `GET /api/v1/admin/questions`：全部问题，可使用 `moderationStatus` 筛选。
- `PATCH /api/v1/admin/questions/{id}/moderation`：设置 `PENDING`、`PUBLISHED` 或 `REJECTED`。
- `DELETE /api/v1/admin/questions/{id}`：删除问题及回复。
- `POST /api/v1/admin/questions/{id}/replies`：管理员回复。
- `GET /api/v1/admin/replies`、`DELETE /api/v1/admin/replies/{id}`：管理回复。

## Kimi 预留

下一阶段由 Spring Boot 提供站内 AI 接口，后端再调用 Moonshot OpenAI 兼容 API。前端只访问本站后端，不接触 `KIMI_API_KEY`。建议增加会话表、消息表、限流、超时、敏感内容策略和流式 SSE 响应。
