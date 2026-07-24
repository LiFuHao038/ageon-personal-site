# AGEON API

Spring Boot backend for the AGEON personal technology site.

## First Phase Scope

- Question community API
- Question publishing
- Question listing and filtering
- Reply creation
- Like counter
- Local CORS for `http://localhost:3000`
- H2 local development storage
- MySQL profile for later deployment

AI Q&A and interview library APIs are intentionally not implemented in this phase. Kimi configuration is reserved through backend-only `KIMI_API_KEY`, `KIMI_BASE_URL`, and `KIMI_MODEL` variables.

## Run Locally

This project needs Java 21 and Maven.

```powershell
cd C:\Users\李富浩\Documents\Codex\2026-07-22\wo\outputs\personal-site-v1\backend
mvn spring-boot:run
```

Default local API:

```text
http://localhost:8080/api/v1/community/questions
```

H2 console:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:file:./data/ageon-dev;MODE=MySQL;DATABASE_TO_LOWER=TRUE
```

## MySQL Profile

Create database:

```sql
CREATE DATABASE ageon_site CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Run:

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/ageon_site?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your-password"
mvn spring-boot:run
```

## Frontend Integration

The frontend API client is at:

```text
lib/community-api.ts
```

Set the frontend env variable:

```powershell
$env:NEXT_PUBLIC_API_BASE_URL="http://localhost:8080"
pnpm dev
```

The community page now uses `lib/community-api.ts` to call this backend. Keep the backend running before opening `/community` in the frontend.

## Test

```powershell
mvn test
```

Never expose `KIMI_API_KEY` through a `NEXT_PUBLIC_` variable. The future AI page must call a Spring Boot endpoint, which then calls Moonshot.
