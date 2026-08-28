import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")

function read(relativePath) {
  const filePath = resolve(root, relativePath)
  assert.equal(existsSync(filePath), true, `${relativePath} should exist`)
  return readFileSync(filePath, "utf8")
}

const routes = [
  "app/page.tsx",
  "app/apply/page.tsx",
  "app/apply/stats/page.tsx",
  "app/ai/page.tsx",
]

for (const route of routes) read(route)
read("public/icon.svg")

const navbar = `${read("components/navbar.tsx")}\n${read("lib/site-data.ts")}`
for (const label of ["首页", "投递追踪", "AI 问答"]) {
  assert.match(navbar, new RegExp(label), `navbar should include ${label}`)
}
for (const removed of ["提问社区", "面试题库"]) {
  assert.doesNotMatch(navbar, new RegExp(removed), `navbar should not include ${removed}`)
}

const hero = read("components/hero.tsx")
assert.match(hero, /SentientSphere/, "homepage should use the supplied interactive sphere")
assert.doesNotMatch(hero, /<h1[^>]*>\s*李富浩\s*<\/h1>/, "hero should not be dominated by the user's name")
assert.doesNotMatch(hero, /BUILD|LEARN|SHARE/, "hero should not use a large slogan headline")
assert.match(hero, /AGEON/, "hero should center the AGEON brand word")
assert.doesNotMatch(hero, /techStack|TechChip|Spring Boot|MySQL|Redis|Next\.js/, "hero should no longer render technology chips")
assert.match(hero, /开始投递追踪/, "hero primary CTA should lead into application tracking")
assert.match(hero, /\/apply/, "hero primary CTA should link to /apply")
assert.match(hero, /体验 AI 问答/, "hero secondary CTA should lead into AI chat")
assert.doesNotMatch(hero, /\/community|\/interview/, "hero should not link to removed routes")

const sphere = read("components/sentient-sphere.tsx")
assert.match(sphere, /icosahedronGeometry args=\{\[1\.72, 36\]\}/, "homepage sphere should be scaled down")
assert.match(sphere, /camera=\{\{ position: \[0, 0, 5\.2\], fov: 42 \}\}/, "homepage sphere camera should sit farther back")

const data = read("lib/site-data.ts")
assert.doesNotMatch(data, /communityQuestions/, "site-data should no longer export communityQuestions")
assert.doesNotMatch(data, /interviewQuestions/, "site-data should no longer export interviewQuestions")
assert.match(data, /AI 编程小助手/)
assert.match(data, /AI 零代码应用生成平台/)
assert.match(data, /repoUrl/, "projects should carry repoUrl")
assert.match(data, /demoUrl/, "projects should carry demoUrl")

const footer = read("components/site-footer.tsx")
assert.doesNotMatch(footer, /\/community|\/interview/, "footer should not link to removed routes")

const applyBoard = read("components/apply-board.tsx")
assert.match(applyBoard, /listApplications/, "apply board should load applications through the API")
const applyForm = read("components/apply-form.tsx")
assert.match(applyForm, /previewApplicationSource/, "apply form should support source link preview")
const applyCard = read("components/apply-card.tsx")
assert.match(applyCard, /changeApplicationStatus/, "apply card should drive status transitions")
const applyStats = read("components/apply-stats.tsx")
assert.match(applyStats, /getApplicationStats/, "apply stats should load the stats overview")

const applicationApi = read("lib/application-api.ts")
assert.match(applicationApi, /apiRequest/, "application API should go through the shared apiRequest client")

const aiChat = read("components/ai-chat.tsx")
assert.match(aiChat, /streamAiMessage/)
assert.doesNotMatch(aiChat, /createDemoAnswer/)
assert.match(aiChat, /发送/)

console.log("site contract checks passed")
