import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")

function read(relativePath) {
  const filePath = resolve(root, relativePath)
  assert.equal(existsSync(filePath), true, `${relativePath} should exist`)
  return readFileSync(filePath, "utf8")
}

const controller = read("backend/src/main/java/cn/ageon/community/CommunityQuestionController.java")
for (const route of [
  '@RequestMapping("/api/v1/community")',
  '@GetMapping("/questions")',
  '@PostMapping("/questions")',
  '@GetMapping("/questions/{id}")',
  '@PostMapping("/questions/{id}/replies")',
  '@PostMapping("/questions/{id}/likes")',
  '@GetMapping("/tags")',
]) {
  assert.match(controller, new RegExp(route.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")), `controller should expose ${route}`)
}

const response = read("backend/src/main/java/cn/ageon/community/dto/QuestionResponse.java")
for (const field of ["title", "detail", "tag", "author", "replies", "status", "time", "likes", "replyItems"]) {
  assert.match(response, new RegExp(`\\b${field}\\b`), `question response should include ${field}`)
}

const frontendClient = `${read("lib/community-api.ts")}\n${read("lib/api-client.ts")}`
for (const api of ["listCommunityQuestions", "createCommunityQuestion", "createCommunityReply", "likeCommunityQuestion"]) {
  assert.match(frontendClient, new RegExp(`export function ${api}`), `frontend API client should expose ${api}`)
}
assert.match(frontendClient, /NEXT_PUBLIC_API_BASE_URL/, "frontend API client should allow API base URL configuration")

const backendConfig = read("backend/src/main/resources/application.yml")
assert.match(backendConfig, /https:\/\/dashscope\.aliyuncs\.com\/compatible-mode\/v1/)
assert.match(backendConfig, /AI_PRIMARY_MODEL:qwen-plus/)
assert.match(backendConfig, /AI_FALLBACK_MODEL:\$\{KIMI_MODEL:kimi\/kimi-k3\}/)
assert.match(backendConfig, /AI_CONTEXT_WINDOW_TOKENS:\$\{KIMI_CONTEXT_WINDOW_TOKENS:128000\}/)
const securityConfig = read("backend/src/main/java/cn/ageon/config/SecurityConfig.java")
assert.match(securityConfig, /"\/actuator\/health"/)

const frontendDockerfile = read("Dockerfile")
assert.match(frontendDockerfile, /ARG NEXT_PUBLIC_API_BASE_URL/)
assert.match(frontendDockerfile, /ENV NEXT_PUBLIC_API_BASE_URL=\$NEXT_PUBLIC_API_BASE_URL/)
assert.match(frontendDockerfile, /\.next\/standalone/)

const backendDockerfile = read("backend/Dockerfile")
assert.match(backendDockerfile, /eclipse-temurin:21-jre/)
assert.match(backendDockerfile, /\/actuator\/health/)

const deployment = read("docs/deployment-sealos.md")
for (const variable of [
  "MYSQL_URL",
  "MYSQL_USERNAME",
  "MYSQL_PASSWORD",
  "AGEON_JWT_SECRET",
  "AGEON_ADMIN_PASSWORD",
  "DASHSCOPE_API_KEY",
  "AI_PRIMARY_MODEL",
  "AI_FALLBACK_MODEL",
  "NEXT_PUBLIC_API_BASE_URL",
]) {
  assert.match(deployment, new RegExp(variable), `deployment guide should document ${variable}`)
}
assert.doesNotMatch(deployment, /sk-[A-Za-z0-9_-]{10,}/)

const docs = read("docs/api-community.md")
assert.match(docs, /GET \/api\/v1\/community\/questions/)
assert.match(docs, /POST \/api\/v1\/community\/questions/)
assert.match(docs, /POST \/api\/v1\/community\/questions\/\{id\}\/replies/)
assert.match(docs, /POST \/api\/v1\/community\/questions\/\{id\}\/likes/)

console.log("backend contract checks passed")
