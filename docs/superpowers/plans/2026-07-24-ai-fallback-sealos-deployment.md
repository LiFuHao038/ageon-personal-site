# AI Fallback and Sealos Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe primary/fallback AI streaming, user-level concurrency control, frontend fallback status handling, Flyway migrations, health checks, and reproducible Sealos container deployment.

**Architecture:** `AiModelGateway` owns the primary/fallback decision and delegates one model attempt at a time to an OpenAI-compatible HTTP client. `AiStreamService` remains the single owner of quota reservation, message persistence, SSE lifecycle, and user-level concurrency. Next.js consumes a new `model_status` SSE event without replacing streamed content.

**Tech Stack:** Spring Boot 3.3.5, JDK 21 HttpClient, JUnit 5, Mockito, Next.js 16 App Router, React 19, TypeScript, Vitest, Flyway, MySQL 8, Docker, Sealos Cloud.

## Global Constraints

- Never log or expose API keys, JWT secrets, database passwords, or complete user prompts.
- Error summaries are limited to 500 characters; logs may contain model, HTTP status, fallback flag, and upstream `request_id` only.
- Fallback is allowed only for HTTP 429, HTTP 503, connection timeout, or read timeout before the first token.
- HTTP 400/401/403 and failures after the first token must never trigger fallback.
- A request reserves quota and persists the user message once; full failure releases the reservation.
- One JVM instance allows at most one active AI generation per user ID.
- `NEXT_PUBLIC_API_BASE_URL` is injected during Docker build with `ARG` and `--build-arg`.
- MySQL DDL is managed by Flyway; production Hibernate uses `ddl-auto=validate`.
- No LangChain4j, Redis, queue, or third model provider is introduced.

---

### Task 1: Provider-Neutral AI Configuration

**Files:**
- Create: `backend/src/main/java/cn/ageon/ai/AiProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/cn/ageon/ai/AiPropertiesTest.java`

**Interfaces:**
- Produces: `AiProperties#getPrimaryModel()`, `getFallbackModel()`, `isFallbackEnabled()`, timeout and token-limit getters.
- Preserves: legacy `KIMI_*` environment variable fallback expressions during migration.

- [ ] **Step 1: Write the failing configuration test**

```java
@SpringBootTest(properties = {
    "ageon.ai.primary-model=qwen-plus",
    "ageon.ai.fallback-enabled=true",
    "ageon.ai.fallback-model=kimi/kimi-k3"
})
class AiPropertiesTest {
    @Autowired AiProperties properties;

    @Test void bindsPrimaryAndFallbackModels() {
        assertThat(properties.getPrimaryModel()).isEqualTo("qwen-plus");
        assertThat(properties.getFallbackModel()).isEqualTo("kimi/kimi-k3");
        assertThat(properties.isFallbackEnabled()).isTrue();
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `mvn -Dtest=AiPropertiesTest test`

Expected: compilation failure because `AiProperties` does not exist.

- [ ] **Step 3: Implement `AiProperties` and YAML bindings**

Create a `@ConfigurationProperties(prefix = "ageon.ai")` component with API key, base URL, primary model, fallback flag/model, timeouts, context window, and max output tokens. Bind defaults to `qwen-plus` and `kimi/kimi-k3`, with `DASHSCOPE_API_KEY` and legacy `KIMI_*` aliases.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `mvn -Dtest=AiPropertiesTest test`

Expected: one passing test.

### Task 2: Single-Attempt OpenAI-Compatible Client

**Files:**
- Create: `backend/src/main/java/cn/ageon/ai/AiModelClient.java`
- Create: `backend/src/main/java/cn/ageon/ai/AiModelException.java`
- Create: `backend/src/main/java/cn/ageon/ai/HttpAiModelClient.java`
- Modify: `backend/src/main/java/cn/ageon/ai/KimiStreamParser.java`
- Delete after migration: `backend/src/main/java/cn/ageon/ai/HttpKimiClient.java`
- Delete after migration: `backend/src/main/java/cn/ageon/ai/KimiClient.java`
- Delete after migration: `backend/src/main/java/cn/ageon/ai/KimiClientException.java`
- Test: `backend/src/test/java/cn/ageon/ai/HttpAiModelClientTest.java`

**Interfaces:**
- Produces: `void stream(String model, List<KimiChatMessage> messages, KimiDeltaHandler handler)`.
- Produces: `AiModelException` carrying public code/message, HTTP status, retryable-before-first-token flag, and sanitized request ID.

- [ ] **Step 1: Write HTTP server tests for status classification**

Use JDK `HttpServer` to assert that 429 and 503 produce retryable exceptions, 400/401/403 produce non-retryable exceptions, SSE deltas are forwarded, and no request body is included in exception messages.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `mvn -Dtest=HttpAiModelClientTest test`

Expected: compilation failure because the provider-neutral client is missing.

- [ ] **Step 3: Implement the single-attempt client**

Build a POST `/chat/completions` request with `model`, `messages`, `stream=true`, and `max_tokens`. Parse `request_id` from JSON error bodies, truncate summaries to 500 characters, and log only model/status/request ID/summary. Convert connection and read timeouts to retryable `AiModelException`; preserve thread interruption.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `mvn -Dtest=HttpAiModelClientTest test`

Expected: all status and SSE parsing tests pass.

### Task 3: Primary/Fallback Gateway

**Files:**
- Create: `backend/src/main/java/cn/ageon/ai/AiModelGateway.java`
- Create: `backend/src/main/java/cn/ageon/ai/AiModelStatusHandler.java`
- Test: `backend/src/test/java/cn/ageon/ai/AiModelGatewayTest.java`

**Interfaces:**
- Produces: `void stream(List<KimiChatMessage> messages, AiModelStatusHandler statusHandler, KimiDeltaHandler deltaHandler)`.
- Status callback: `onFallback(String model)` exactly once before the fallback attempt.

- [ ] **Step 1: Write gateway tests with Mockito**

Cover primary success without fallback, primary 429 fallback success, primary 503 fallback, timeout fallback, 400/401/403 without fallback, first-token-then-failure without fallback, disabled fallback, and both attempts failing.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `mvn -Dtest=AiModelGatewayTest test`

Expected: compilation failure because `AiModelGateway` does not exist.

- [ ] **Step 3: Implement minimal fallback orchestration**

Track first-token emission with `AtomicBoolean`. Invoke the primary model once. Only when the exception is retryable, no token was emitted, fallback is enabled, and model names differ, log the fallback decision, invoke `statusHandler.onFallback`, and call the fallback once with the same immutable message list.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `mvn -Dtest=AiModelGatewayTest test`

Expected: all fallback boundary tests pass.

### Task 4: SSE Integration and User-Level Concurrency

**Files:**
- Create: `backend/src/main/java/cn/ageon/ai/dto/AiModelStatusEvent.java`
- Modify: `backend/src/main/java/cn/ageon/ai/AiStreamService.java`
- Modify: `backend/src/test/java/cn/ageon/ai/AiStreamControllerTest.java`

**Interfaces:**
- SSE event: `model_status` with `{ "status": "fallback", "model": "..." }`.
- Conflict code/message: `AI_REQUEST_IN_PROGRESS` / `已有回答正在生成，请稍后再试`.

- [ ] **Step 1: Add failing integration tests**

Replace the old model client mock with `AiModelGateway`. Verify `model_status` is emitted before fallback deltas, quota is charged once on success, full failure releases quota, and two conversations owned by one user cannot stream concurrently.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `mvn -Dtest=AiStreamControllerTest test`

Expected: failures because the gateway and user-level lock are not wired.

- [ ] **Step 3: Integrate gateway and user lock**

Change the active set key from conversation ID to user ID. Send `model_status` from the gateway callback. Keep quota reservation and user-message persistence outside model attempts. In every terminal path, cancel the worker when necessary, remove the user ID, save one failed attempt, release quota once, and complete the emitter.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `mvn -Dtest=AiStreamControllerTest test`

Expected: stream, failure, quota, status, and concurrency tests pass.

### Task 5: Frontend SSE Contract and UI State

**Files:**
- Modify: `lib/ai-api.ts`
- Modify: `components/ai-chat.tsx`
- Create: `lib/ai-errors.ts`
- Create: `vitest.config.ts`
- Create: `tests/ai-stream-state.test.ts`
- Modify: `package.json`
- Modify: `pnpm-lock.yaml`

**Interfaces:**
- Adds: `AiModelStatus` and `onModelStatus(status)` to `streamAiMessage`.
- Adds: `toAiErrorMessage(code, fallback)` returning required Chinese messages.

- [ ] **Step 1: Add Vitest and write failing state/error tests**

Test the error-code mapping, `model_status` parsing, fallback text visibility before the first delta, preservation of existing answer content, and reset of generating state on done/error/abort.

- [ ] **Step 2: Run focused frontend tests and verify RED**

Run: `pnpm exec vitest run tests/ai-stream-state.test.ts`

Expected: failures because status parsing and error mapping are missing.

- [ ] **Step 3: Implement frontend behavior**

Parse `model_status`, store a separate fallback notice, clear it on first delta/done/error, never place it in the assistant message content, map backend error codes to Chinese, and retain the existing `finally { setGenerating(false) }` guarantee.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `pnpm exec vitest run tests/ai-stream-state.test.ts`

Expected: all frontend AI state tests pass.

### Task 6: Flyway and Health Check

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__baseline.sql`
- Test: `backend/src/test/java/cn/ageon/deployment/ProductionConfigurationTest.java`

**Interfaces:**
- Produces: `/actuator/health` with unauthenticated health status.
- Production profile: Flyway enabled and Hibernate `ddl-auto=validate`.

- [ ] **Step 1: Write failing production configuration test**

Assert Actuator health is permitted and migration `V1__baseline.sql` contains all current application tables, foreign keys, unique indexes, and `ai_daily_limit`.

- [ ] **Step 2: Run focused test and verify RED**

Run: `mvn -Dtest=ProductionConfigurationTest test`

Expected: failure because Actuator and Flyway migration are missing.

- [ ] **Step 3: Add dependencies, migration, and profile configuration**

Add `spring-boot-starter-actuator`, `flyway-core`, and `flyway-mysql`. Convert the complete MySQL schema to one baseline migration. Disable H2 Flyway in tests/dev until a dedicated H2 migration is needed; enable Flyway for `mysql`, set `ddl-auto=validate`, and expose only `health`.

- [ ] **Step 4: Run focused and full backend tests**

Run: `mvn test`

Expected: all backend tests pass with no migration conflicts.

### Task 7: Docker and Sealos Deployment Assets

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`
- Create or modify: `next.config.mjs`
- Create: `docs/deployment-sealos.md`
- Modify: `README.md`

**Interfaces:**
- Frontend build argument: `NEXT_PUBLIC_API_BASE_URL`.
- Backend runtime port: `PORT`, default 8080.
- Probe: `/actuator/health`.

- [ ] **Step 1: Add deployment contract assertions**

Extend `tests/backend-contract.test.mjs` to assert both Dockerfiles exist, frontend uses `ARG NEXT_PUBLIC_API_BASE_URL`, backend uses JRE 21 and health check, and the Sealos document lists every required secret without values.

- [ ] **Step 2: Run contract tests and verify RED**

Run: `node tests/backend-contract.test.mjs`

Expected: failure because deployment assets are missing.

- [ ] **Step 3: Implement production container files and guide**

Use multi-stage builds. Next.js builds in standalone mode and copies `.next/standalone`, `.next/static`, and `public`. Spring Boot builds with Maven/JDK 21 and runs the JAR on JRE 21. Document Sealos MySQL creation, secret configuration, `--build-arg`, CORS, probes, verification, backup, and rollback.

- [ ] **Step 4: Run frontend contracts and builds**

Run: `pnpm test`

Run: `pnpm build`

Expected: all contract tests and the Next.js production build pass.

### Task 8: Final Security and Regression Verification

**Files:**
- Review all modified files.

**Interfaces:**
- No new runtime interface; this task verifies the complete system contract.

- [ ] **Step 1: Scan for exposed secrets and unsafe logging**

Run: `rg -n "sk-|Bearer [A-Za-z0-9]|API_KEY=.+|JWT_SECRET=.+|password: .+|log\.(info|warn|error).*prompt" . -g '!node_modules/**' -g '!backend/target/**'`

Expected: no real credential values and no prompt logging.

- [ ] **Step 2: Run complete backend verification**

Run: `mvn test`

Expected: zero failures and zero errors.

- [ ] **Step 3: Run complete frontend verification**

Run: `pnpm test`

Run: `pnpm exec vitest run`

Run: `pnpm build`

Expected: all tests pass and production build exits zero.

- [ ] **Step 4: Build containers when Docker is available**

Run: `docker build --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 -t ageon-web:test .`

Run: `docker build -t ageon-api:test backend`

Expected: both images build successfully. If Docker is unavailable, record that verification limitation explicitly.
