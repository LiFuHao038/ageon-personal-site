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
  "app/community/page.tsx",
  "app/ai/page.tsx",
  "app/interview/page.tsx",
]

for (const route of routes) read(route)
read("public/icon.svg")

const navbar = `${read("components/navbar.tsx")}\n${read("lib/site-data.ts")}`
for (const label of ["首页", "提问社区", "AI 问答", "面试题库"]) {
  assert.match(navbar, new RegExp(label), `navbar should include ${label}`)
}

const hero = read("components/hero.tsx")
assert.match(hero, /SentientSphere/, "homepage should use the supplied interactive sphere")
assert.doesNotMatch(hero, /<h1[^>]*>\s*李富浩\s*<\/h1>/, "hero should not be dominated by the user's name")
assert.doesNotMatch(hero, /BUILD|LEARN|SHARE/, "hero should not use a large slogan headline")
assert.match(hero, /AGEON/, "hero should center the AGEON brand word")
assert.doesNotMatch(hero, /techStack|TechChip|Spring Boot|MySQL|Redis|Next\.js/, "hero should no longer render technology chips")

const sphere = read("components/sentient-sphere.tsx")
assert.match(sphere, /icosahedronGeometry args=\{\[1\.72, 36\]\}/, "homepage sphere should be scaled down")
assert.match(sphere, /camera=\{\{ position: \[0, 0, 5\.2\], fov: 42 \}\}/, "homepage sphere camera should sit farther back")

const data = read("lib/site-data.ts")
assert.match(data, /AI 编程小助手/)
assert.match(data, /AI 零代码应用生成平台/)
assert.match(data, /计算机网络/)

const community = read("components/community-board.tsx")
assert.match(community, /listCommunityQuestions/)
assert.match(community, /createCommunityQuestion/)
assert.doesNotMatch(community, /localStorage/)
assert.match(community, /发布问题/)
assert.match(community, /\/community\/\$\{question\.id\}/)

const communityDetail = read("components/community-detail.tsx")
assert.match(communityDetail, /createCommunityReply/)
assert.match(communityDetail, /likeCommunityQuestion/)

const aiChat = read("components/ai-chat.tsx")
assert.match(aiChat, /streamAiMessage/)
assert.doesNotMatch(aiChat, /createDemoAnswer/)
assert.match(aiChat, /发送/)

const interview = read("components/interview-library.tsx")
assert.match(interview, /模拟面试/)
assert.match(interview, /查看答案/)

console.log("site contract checks passed")
