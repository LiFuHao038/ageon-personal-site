# AGEON 秋招投递追踪 — 设计文档

> 状态：实施中　|　范围：v1 MVP　|　替代原「提问社区 + 面试题库」定位

## 1. 定位

面向秋招本人的**投递记录 + 状态时间线 + 统计看板**。数据私有、按用户隔离。
保留：AI 问答、JWT 登录、管理员后台、部署链路。
下线：提问社区、面试题库（先从导航与首页移除入口，不再作为主功能）。

## 2. 硬性工程约束（违反会导致构建失败）

1. **禁止新增任何 npm 依赖。** 本机 `node_modules` 有过安装失败痕迹（`node_modules.failed-local-store`、
   `@microsoft/fetch-event-source` 以 `file:vendor/` 方式引入）。`clsx`、`tailwind-merge`、`recharts`、
   `@radix-ui/*`、`date-fns` **全部未安装**。
2. **禁止 import `@/components/ui/*` 与 `@/lib/utils`。** 这 57 个 shadcn 文件依赖缺失，已被
   `tsconfig.json` 的 `exclude` 藏起来；新代码引用它们会立刻让 `tsc --noEmit` 失败。
   → 图表一律用**手写内联 SVG**（条形/漏斗/折线），不引入图表库。
3. **禁止在 `tsconfig.json` 里靠 `exclude` 掩盖新代码的类型错误。**
4. 不修改 `components/ai-chat.tsx`、`lib/ai-*.ts`、`backend/.../ai/**`（AI 问答保持现状）。
5. 中文内容文件必须以 **UTF-8 无 BOM** 写盘（README 已记录中文路径下的编码坑）。
6. 后端 Java 21；沿用现有 `java.net.http.HttpClient` 与虚拟线程，不引入 WebFlux / OkHttp / Jsoup。
   HTML 解析用正则 + 有限状态即可，不要为此加依赖。

## 3. 领域模型

### 3.1 状态机 `ApplicationStatus`

包：`cn.ageon.apply`

| 枚举 | 中文 | 终态 | 漏斗序 |
|---|---|---|---|
| `PREPARING` | 准备投递 | 否 | 0 |
| `APPLIED` | 已投递 | 否 | 1 |
| `WRITTEN_TEST` | 笔试/测评 | 否 | 2 |
| `INTERVIEW_1` | 一面 | 否 | 3 |
| `INTERVIEW_2` | 二面 | 否 | 4 |
| `INTERVIEW_FINAL` | 终面/HR面 | 否 | 5 |
| `OFFER` | 已获得 offer | 是 | 6 |
| `REJECTED` | 未通过 | 是 | -1 |
| `WITHDRAWN` | 已撤回 | 是 | -1 |

流转规则（`ApplicationStatus.canTransitionTo`）：
- 允许**沿漏斗向前跳跃**（如 `APPLIED → INTERVIEW_1`，跳过笔试），不允许向后退到序更小的漏斗状态。
- 任意非终态 → `REJECTED` / `WITHDRAWN` 合法。
- 终态 → 任何状态**非法**（要改请删除记录或新建），例外：`OFFER → WITHDRAWN` 合法（接了别家）。
- 同状态重复设置非法（会产生无意义事件）。
- 违反规则返回 `ApiException("INVALID_STATUS_TRANSITION", …, HttpStatus.BAD_REQUEST)`。

### 3.2 表 `job_applications`

```sql
id                BIGINT PK AI
user_id           BIGINT NOT NULL            -- 归属，FK site_users(id) ON DELETE CASCADE
company           VARCHAR(60)  NOT NULL
position          VARCHAR(80)  NOT NULL
city              VARCHAR(40)  NULL
company_type      VARCHAR(20)  NULL          -- 国企/民企/外企/银行/互联网/其他
channel           VARCHAR(20)  NULL          -- 官网/内推/宣讲会/公众号/猎头/其他
status            VARCHAR(20)  NOT NULL
source_url        VARCHAR(500) NULL
source_title      VARCHAR(200) NULL          -- 抓取结果
source_logo_url   VARCHAR(500) NULL          -- 抓取结果
source_error      VARCHAR(200) NULL          -- 抓取失败原因（成功时置 NULL）
source_fetched_at DATETIME(6)  NULL
deadline_at       DATE NULL                  -- 网申截止
applied_at        DATE NOT NULL              -- 投递日期，统计基准
note              VARCHAR(1000) NULL
created_at        DATETIME(6) NOT NULL
updated_at        DATETIME(6) NOT NULL
UNIQUE uk_job_applications_owner (user_id, company, position)
INDEX  idx_job_applications_user_status  (user_id, status)
INDEX  idx_job_applications_user_applied (user_id, applied_at)
INDEX  idx_job_applications_user_deadline(user_id, deadline_at)
```

### 3.3 表 `job_application_events`（时间线，统计的唯一真相来源）

```sql
id             BIGINT PK AI
application_id BIGINT NOT NULL          -- FK job_applications(id) ON DELETE CASCADE
from_status    VARCHAR(20) NULL         -- 首次创建时为 NULL
to_status      VARCHAR(20) NOT NULL
occurred_at    DATETIME(6) NOT NULL     -- 业务发生时间（用户可指定），非入库时间
note           VARCHAR(300) NULL
created_at     DATETIME(6) NOT NULL
INDEX idx_job_application_events_app_time (application_id, occurred_at)
```

**为什么要事件表而不是只存当前状态**：状态会变，只留最新值就算不出「从投递到收到笔试隔了几天」。
阶段耗时、漏斗到达数全部从事件表推导，`updated_at` 不参与统计。

## 4. REST 契约

统一前缀 `/api/v1/applications`，**全部要求登录**（`SecurityConfig` 中 `anyRequest().authenticated()`
已默认覆盖，需显式加一条与 AI 同级的规则以便阅读）。所有查询强制 `user_id = 当前用户`；
访问他人资源一律 `NotFoundException`（404，不泄露资源是否存在）。
复用 `AuthenticatedUser.requireApprovedUser(authentication)`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/applications` | 列表，query: `status`、`keyword`、`companyType`、`hasDeadline` |
| POST | `/api/v1/applications` | 新建；`sourceUrl` 存在时同步抓取元信息 |
| GET | `/api/v1/applications/{id}` | 单条（含事件时间线） |
| PATCH | `/api/v1/applications/{id}` | 修改基本信息（**不改变 status**） |
| DELETE | `/api/v1/applications/{id}` | 删除，204 |
| POST | `/api/v1/applications/{id}/status` | 状态流转，写事件 |
| POST | `/api/v1/applications/source-preview` | 仅抓元信息供表单回填，不落库 |
| GET | `/api/v1/applications/stats/overview` | 统计看板 |

### 4.1 DTO（Java record，放 `cn.ageon.apply.dto`）

```java
CreateApplicationRequest(String company, String position, String city, String companyType,
                         String channel, String sourceUrl, LocalDate deadlineAt,
                         LocalDate appliedAt, String note)
UpdateApplicationRequest(String company, String position, String city, String companyType,
                         String channel, String sourceUrl, LocalDate deadlineAt,
                         LocalDate appliedAt, String note)     // 字段为 null 表示不修改
ChangeStatusRequest(ApplicationStatus status, Instant occurredAt, String note)
SourcePreviewRequest(String url)

ApplicationResponse(Long id, String company, String position, String city, String companyType,
                    String channel, ApplicationStatus status, String statusLabel,
                    String sourceUrl, String sourceTitle, String sourceLogoUrl, String sourceError,
                    Instant sourceFetchedAt, LocalDate deadlineAt, LocalDate appliedAt, String note,
                    Long daysSinceApplied, Long daysToDeadline, Instant createdAt, Instant updatedAt,
                    List<ApplicationEventResponse> events)
ApplicationEventResponse(Long id, ApplicationStatus fromStatus, String fromStatusLabel,
                         ApplicationStatus toStatus, String toStatusLabel,
                         Instant occurredAt, String note)
SourcePreviewResponse(String url, String title, String logoUrl, String error)
```

### 4.2 统计响应

```java
ApplicationStatsResponse(
    int total, int active, int offers, int rejected,
    List<FunnelStage> funnel,            // 见下，严格 7 项，序固定
    List<StageDuration> stageDurations,
    List<GroupStat> byCompanyType,
    List<GroupStat> byCity,
    List<WeeklyPoint> weekly,
    List<DeadlineItem> upcomingDeadlines // 含已逾期，按 deadlineAt 升序，最多 20 条
)
FunnelStage(String status, String label, int reached)      // reached = 曾到达过该状态的去重投递数
StageDuration(String from, String fromLabel, String to, String toLabel,
              double averageDays, int samples)
GroupStat(String key, int total, int offers, int rejected, double responseRate)
WeeklyPoint(LocalDate weekStart, int applied)               // weekStart 为周一，ISO
DeadlineItem(Long id, String company, String position, LocalDate deadlineAt, long daysLeft, boolean overdue)
```

**漏斗口径（重要，面试会问）**：`reached(stage)` = 事件表中 `to_status == stage` 出现过的
**去重 application 数**。因为允许跳跃，`APPLIED` 的人数 ≥ `WRITTEN_TEST`，但「跳过笔试直接面试」
会让 `WRITTEN_TEST` 漏斗不单调——所以**不做「本级/上级转化率」，只报绝对到达数**，
并在前端注释说明口径。不要谎称这是转化率。

**阶段耗时口径**：对每个 application，取事件按 `occurredAt` 升序，对每对相邻事件
`(from → to)` 累加天数差，按 `(from,to)` 分组求均值与样本数。样本数 < 3 的组前端标灰，
提示「样本过少」。

**responseRate** = `(到过 WRITTEN_TEST 及以后任一状态的人数) / 该组 APPLIED 人数`，保留两位小数。

## 5. 链接元信息抓取 `LinkSnapshotService`

- 输入校验：仅 `http`/`https`；解析主机后**拒绝回环、私有、链路本地地址（SSRF 防护）**，
  重定向逐跳重新校验（最多 3 跳，`HttpClient.Redirect.NEVER`，手动跟进）。
- 超时：连接 5s / 请求 8s，硬上限 12s（`Future.get(timeout)` 包裹）。
- 大小上限：响应体最多读 512 KiB，超出即截断停止；只处理 `text/html`。
- UA：`Mozilla/5.0 (compatible; AgeonBot/1.0; +https://github.com/)`。
- 解析顺序：`og:title` → `twitter:title` → `<title>`；logo 取 `og:image` → `apple-touch-icon` →
  `favicon.ico`（相对路径按 `<base>`/原 URL 解析为绝对）。
- 失败**绝不抛异常打断主流程**：返回 `SnapshotResult(title=null, logo=null, error="简短原因")`，
  新建投递照常成功。
- 并发：虚拟线程 `Executors.newVirtualThreadPerTaskExecutor()` + `Semaphore` 限 4，
  批量重抓接口预留。

## 6. 前端

- `lib/application-api.ts`：类型 + `listApplications/createApplication/updateApplication/
  deleteApplication/changeApplicationStatus/previewApplicationSource/getApplicationStats`，
  全部走现有 `apiRequest`（默认带 token）。
- `app/apply/page.tsx` + `components/apply-board.tsx`：看板（按 9 状态分列，横向滚动）
  / 表格双视图切换；卡片显示公司、岗位、状态色条、距投递天数、截止倒计时（逾期红）。
- `components/apply-form.tsx`：新建/编辑抽屉。粘贴网申链接 → 点「解析」调 `source-preview`
  回填公司名与标题；解析失败仅提示，不阻塞提交。
- `components/apply-card.tsx`：状态推进按钮（按 `canTransitionTo` 生成），点击即写事件。
- `app/apply/stats/page.tsx` + `components/apply-stats.tsx`：手写 SVG 漏斗、阶段耗时条、
  分组表、周趋势折线、临期/逾期列表。
- 视觉沿用现有 token：`site-shell` `page-main` `eyebrow` `panel` `mono` `interactive`，
  主色 `#9ef01a`、`--danger: #ff6b5f`，深色网格背景。**不要引入浅色主题。**
- 未登录时 `/apply` 展示登录引导（复用 `useAuth()`），不在客户端直接 fetch 前判断。

## 7. 清理与 SEO

- 导航 `siteNav`：`首页 / 投递追踪 / AI 问答`（移除提问社区、面试题库）。
- `lib/site-data.ts`：删除 `communityQuestions`、`interviewQuestions` 硬编码假数据
  （含写死的 `"18 分钟前"`）；`projects` 补真实字段 `repoUrl`/`demoUrl`，项目卡片改为可点。
- 移除 `app/community/**`、`app/interview/**`、`components/community-*.tsx`、
  `components/interview-library.tsx`、`lib/community-api.ts`。
  后端 `community` 包**暂不删除**（保留数据与迁移，只是不再对外暴露入口）。
- `app/sitemap.ts` + `app/robots.ts`；`metadata.metadataBase` 用
  `process.env.NEXT_PUBLIC_SITE_URL`；补 `openGraph` 与 `Person` JSON-LD。
- 删除 `tsconfig.json` 里被 exclude 的 57 个 `components/ui/*` 死文件与
  `lib/utils.ts`、`hooks/use-toast.ts`（连同 exclude 项一起清理）。

## 8. 验收

```powershell
# 前端
.\node_modules\.bin\tsc.cmd --noEmit
pnpm test
pnpm build
# 后端（本机 mvn 不在 PATH，用 wrapper 发行版）
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.16\*\bin\mvn.cmd" -f backend\pom.xml test
```

后端新增覆盖：状态机非法流转、跨用户访问返回 404、漏斗到达数口径、阶段耗时均值、
SSRF 拦截（`http://127.0.0.1` 与 `http://169.254.169.254`）、抓取失败不阻断创建。
前端契约测试同步改写为新功能断言，不允许留下引用已删文件的测试。
