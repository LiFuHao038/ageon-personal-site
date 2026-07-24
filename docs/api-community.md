# AGEON Community API

Base URL for local development:

```text
http://localhost:8080/api/v1/community
```

The first backend phase covers the question community only. AI Q&A and interview library remain frontend demos until the next phase.

## Question Response

Every question response is shaped for the current frontend `CommunityQuestion` model:

```json
{
  "id": 1,
  "title": "Spring Boot 项目中 SSE 连接如何避免超时？",
  "detail": "正在实现 AI 流式输出，想了解连接管理和异常重试的常见方案。",
  "tag": "Java 后端",
  "author": "CodeLearner",
  "replies": 1,
  "status": "已回复",
  "time": "刚刚",
  "likes": 0,
  "createdAt": "2026-07-22T08:00:00Z",
  "updatedAt": "2026-07-22T08:00:00Z",
  "replyItems": [
    {
      "id": 1,
      "author": "李富浩",
      "content": "可以从心跳、超时配置、断线重连和任务状态恢复四个点拆。",
      "createdAt": "2026-07-22T08:00:00Z"
    }
  ]
}
```

`status` values:

- `待回复`
- `讨论中`
- `已回复`

## Endpoints

### List Questions

```http
GET /api/v1/community/questions?keyword=SSE&tag=Java 后端&status=ANSWERED
```

Query parameters are optional.

### Create Question

```http
POST /api/v1/community/questions
Content-Type: application/json

{
  "title": "如何设计社区接口？",
  "detail": "我希望前端可以直接消费响应字段。",
  "tag": "Java 后端",
  "author": "访客"
}
```

Returns `201 Created` with a `QuestionResponse`.

### Get Question

```http
GET /api/v1/community/questions/{id}
```

### Create Reply

```http
POST /api/v1/community/questions/{id}/replies
Content-Type: application/json

{
  "author": "李富浩",
  "content": "先固定请求和响应字段，再接前端。"
}
```

Returns the updated `QuestionResponse`.

### Like Question

```http
POST /api/v1/community/questions/{id}/likes
```

Returns the updated `QuestionResponse`.

### List Tags

```http
GET /api/v1/community/tags
```

Returns:

```json
["全部", "Java 后端", "AI 应用", "计算机网络", "数据库"]
```

## Local Run

The backend defaults to the `dev` profile and uses local H2 storage:

```powershell
cd C:\Users\李富浩\Documents\Codex\2026-07-22\wo\outputs\personal-site-v1\backend
mvn spring-boot:run
```

For MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/ageon_site?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your-password"
mvn spring-boot:run
```

The frontend can call `http://localhost:8080/api/v1/community/questions`.
