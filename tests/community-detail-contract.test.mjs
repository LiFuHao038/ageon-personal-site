import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const read = (path) => {
  const full = resolve(root, path)
  assert.equal(existsSync(full), true, `${path} should exist`)
  return readFileSync(full, "utf8")
}

read("app/community/[id]/page.tsx")
const detail = read("components/community-detail.tsx")
assert.match(detail, /authorRole/)
assert.match(detail, /登录后参与讨论/)
assert.match(detail, /createCommunityReply/)
const api = read("lib/community-api.ts")
assert.match(api, /getCommunityQuestion/)
const list = read("components/community-board.tsx")
assert.match(list, /\/community\/\$\{question\.id\}/)
console.log("community detail contract checks passed")
