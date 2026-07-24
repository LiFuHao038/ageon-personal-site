import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const aiApiPath = resolve(root, "lib/ai-api.ts")

assert.equal(existsSync(aiApiPath), true, "lib/ai-api.ts should exist")
const aiApi = readFileSync(aiApiPath, "utf8")
const aiChat = readFileSync(resolve(root, "components/ai-chat.tsx"), "utf8")

assert.match(aiApi, /@microsoft\/fetch-event-source/)
assert.match(aiApi, /method:\s*["']POST["']/)
assert.match(aiApi, /Authorization:\s*`Bearer \$\{options\.token\}`/)
for (const eventName of ["message", "model_status", "quota", "done", "error"]) {
  assert.match(aiApi, new RegExp(`event === ["']${eventName}["']`))
}
assert.doesNotMatch(aiApi, /new EventSource/)
assert.match(aiChat, /listAiConversations/)
assert.match(aiChat, /streamAiMessage/)
assert.match(aiChat, /AbortController/)
assert.match(aiChat, /streamRuntime\.modelNotice/)
assert.match(aiChat, /dispatchStream\(\{ type: "delta" \}\)/)
assert.match(aiChat, /const wasAborted = abortController\.current\?\.signal\.aborted \?\? false/)
assert.match(aiChat, /dispatchStream\(\{ type: wasAborted \? "abort" : "done" \}\)/)
assert.match(aiChat, /删除对话/)
assert.match(aiChat, /今日剩余/)
assert.doesNotMatch(aiChat, /createDemoAnswer/)
assert.doesNotMatch(aiChat, /\(quota\?\.remaining \?\? 0\) <= 0/, "AI chat should not silently block sending while quota is still loading")
assert.match(aiChat, /id="ai-question-input"/)
assert.match(aiChat, /name="question"/)

console.log("AI stream contract checks passed")
