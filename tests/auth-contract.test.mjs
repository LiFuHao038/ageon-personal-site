import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import { resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const read = (path) => {
  const full = resolve(root, path)
  assert.equal(existsSync(full), true, `${path} should exist`)
  return readFileSync(full, "utf8")
}

read("app/auth/page.tsx")
read("app/admin/login/page.tsx")
const api = read("lib/auth-api.ts")
for (const name of ["registerUser", "loginUser", "getCurrentUser"]) assert.match(api, new RegExp(name))
const provider = read("components/auth-provider.tsx")
assert.match(provider, /ageon-access-token/)
assert.match(provider, /useAuth/)
const screen = read("components/auth-screen.tsx")
assert.match(screen, /等待管理员审核/)
assert.doesNotMatch(screen, /Google|Apple/)
console.log("auth contract checks passed")
