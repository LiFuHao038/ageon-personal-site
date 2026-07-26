import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const read = (path) => {
  const full = resolve(root, path)
  assert.equal(existsSync(full), true, `${path} should exist`)
  return readFileSync(full, "utf8")
}

const particles = read("components/particle-background.tsx")
for (const count of ["18000", "10000", "6000"]) assert.match(particles, new RegExp(count))
assert.match(particles, /prefers-reduced-motion/)
assert.match(particles, /visibilitychange/)
assert.match(particles, /webglcontextlost/)
assert.match(particles, /webglcontextrestored/)
assert.match(particles, /dispose\(\)/)
assert.doesNotMatch(particles, /cdn\.jsdelivr|GLTFLoader|90000/)
assert.doesNotMatch(particles, /attribute\s+vec3\s+color/, "particle shader should not redeclare Three.js color attribute")
assert.match(particles, /aParticleColor/, "particle shader should use a custom color attribute")
assert.doesNotMatch(particles, /setAttribute\("color"/, "particle geometry should not use Three.js reserved color attribute")
assert.doesNotMatch(particles, /vertexColors:\s*true/, "custom particle shader should not request Three.js vertex color injection")

const data = read("lib/profile-folders.ts")
for (const title of ["技术栈", "正在学习", "音乐", "游戏", "近期目标"]) assert.match(data, new RegExp(title))
const folders = read("components/profile-folders.tsx")
assert.doesNotMatch(folders, /confetti|Deleting|Generating|onRename/)
const home = read("components/home-content.tsx")
assert.match(home, /ProfileFolders/)
assert.doesNotMatch(home, /软件工程学生|正在构建 AI 应用/)
console.log("visual contract checks passed")
