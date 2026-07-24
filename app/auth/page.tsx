import type { Metadata } from "next"
import { AuthScreen } from "@/components/auth-screen"

export const metadata: Metadata = { title: "登录与注册" }

export default function AuthPage() {
  return <AuthScreen />
}
