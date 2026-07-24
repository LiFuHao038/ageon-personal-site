import type { Metadata } from "next"
import { AuthScreen } from "@/components/auth-screen"

export const metadata: Metadata = { title: "管理员登录" }

export default function AdminLoginPage() {
  return <AuthScreen adminMode />
}
