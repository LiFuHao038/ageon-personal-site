import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const read = (path) => {
  const full = resolve(root, path)
  assert.equal(existsSync(full), true, `${path} should exist`)
  return readFileSync(full, "utf8")
}

read("app/admin/page.tsx")
const api = read("lib/admin-api.ts")
for (const name of [
  "getAdminOverview", "listAdminUsers", "updateAdminUserStatus", "listAdminQuestions",
  "moderateAdminQuestion", "deleteAdminQuestion", "createAdminReply", "listAdminReplies", "deleteAdminReply",
]) assert.match(api, new RegExp(name))
const dashboard = read("components/admin-dashboard.tsx")
for (const label of ["用户审核", "问题管理", "回复管理", "管理员回复"]) assert.match(dashboard, new RegExp(label))
for (const feedback of ["用户已审核通过", "用户已拒绝"]) assert.match(dashboard, new RegExp(feedback))
assert.match(dashboard, /disabled=\{item\.status === "APPROVED"\}/)
assert.match(dashboard, /disabled=\{item\.status === "REJECTED"\}/)
console.log("admin contract checks passed")
