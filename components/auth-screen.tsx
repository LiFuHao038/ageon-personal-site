"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { ArrowLeft, ArrowRight, Check, Eye, EyeOff, LockKeyhole, ShieldCheck, UserRound } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiClientError } from "@/lib/api-client"

export function AuthScreen({ adminMode = false }: { adminMode?: boolean }) {
  const router = useRouter()
  const { login, register } = useAuth()
  const [signUp, setSignUp] = useState(!adminMode)
  const [showPassword, setShowPassword] = useState(false)
  const [acceptedTerms, setAcceptedTerms] = useState(true)
  const [pendingReview, setPendingReview] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    const data = new FormData(event.currentTarget)
    setSubmitting(true)
    setError("")
    try {
      if (signUp && !adminMode) {
        await register({
          displayName: String(data.get("displayName") ?? "").trim(),
          username: String(data.get("username") ?? "").trim(),
          email: String(data.get("email") ?? "").trim(),
          password: String(data.get("password") ?? ""),
          acceptedTerms,
        })
        setPendingReview(true)
      } else {
        const response = await login(
          String(data.get("identifier") ?? "").trim(),
          String(data.get("password") ?? ""),
        )
        router.push(response.user.role === "ADMIN" ? "/admin" : "/community")
      }
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "服务暂时不可用，请稍后重试")
    } finally {
      setSubmitting(false)
    }
  }

  if (pendingReview) {
    return (
      <main className="auth-page grid min-h-screen place-items-center px-4 pb-16 pt-28">
        <section className="w-full max-w-xl border border-[#9ef01a]/35 bg-[#090b09]/95 p-8 text-center md:p-12">
          <span className="mx-auto grid h-14 w-14 place-items-center border border-[#9ef01a] text-[#9ef01a]"><Check /></span>
          <p className="eyebrow mt-7">REGISTRATION RECEIVED</p>
          <h1 className="mt-4 text-3xl md:text-5xl">等待管理员审核</h1>
          <p className="mx-auto mt-5 max-w-md text-sm leading-7 text-white/55">账号已创建。审核通过后即可登录、发布问题和参与回复。</p>
          <button type="button" onClick={() => { setPendingReview(false); setSignUp(false) }} className="interactive mt-8 inline-flex h-11 items-center gap-2 border border-white/20 px-4 text-sm">返回登录 <ArrowRight size={16} /></button>
        </section>
      </main>
    )
  }

  return (
    <main className="auth-page min-h-screen px-4 pb-14 pt-24 md:grid md:place-items-center md:pt-28">
      <section className="mx-auto grid w-full max-w-5xl overflow-hidden border border-white/15 bg-[#080a08]/94 shadow-[0_30px_100px_rgba(0,0,0,.55)] lg:grid-cols-[.9fr_1.1fr]">
        <div className="auth-visual relative min-h-72 overflow-hidden border-b border-white/10 p-6 lg:min-h-[640px] lg:border-b-0 lg:border-r">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_55%_42%,rgba(158,240,26,.16),transparent_24%),linear-gradient(135deg,transparent_0_48%,rgba(158,240,26,.08)_49%,transparent_50%)]" />
          <div className="relative flex h-full flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="mono text-xs font-bold text-[#9ef01a]">AGEON / ACCESS</span>
              <Link href="/" className="interactive flex h-9 items-center gap-2 border border-white/15 px-3 text-xs text-white/60"><ArrowLeft size={14} /> 返回网站</Link>
            </div>
            <div>
              <span className="grid h-12 w-12 place-items-center border border-[#9ef01a] text-[#9ef01a]">{adminMode ? <ShieldCheck /> : <UserRound />}</span>
              <p className="mono mt-6 text-[10px] tracking-[.15em] text-[#9ef01a]">{adminMode ? "ADMIN CONSOLE" : "BUILD / LEARN / SHARE"}</p>
              <h2 className="mt-3 max-w-md text-3xl leading-tight md:text-5xl">{adminMode ? "审核内容，维护社区秩序。" : "进入属于开发者的交流空间。"}</h2>
            </div>
          </div>
        </div>

        <div className="flex min-h-[560px] flex-col justify-center p-6 md:p-10 lg:p-14">
          <p className="eyebrow">{adminMode ? "SECURE LOGIN" : signUp ? "CREATE ACCOUNT" : "WELCOME BACK"}</p>
          <h1 className="mt-3 text-3xl md:text-4xl">{adminMode ? "管理员登录" : signUp ? "注册账号" : "登录 AGEON"}</h1>
          {!adminMode && <p className="mt-3 text-sm text-white/45">{signUp ? "已有账号？" : "还没有账号？"}<button type="button" onClick={() => { setSignUp((value) => !value); setError("") }} className="ml-2 text-[#9ef01a] hover:underline">{signUp ? "直接登录" : "申请注册"}</button></p>}

          <form onSubmit={submit} className="mt-8 grid gap-4">
            {signUp && !adminMode && (
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="grid gap-2 text-xs text-white/50"><span>显示名称</span><input required name="displayName" minLength={2} maxLength={40} className="auth-input" placeholder="你的名称" /></label>
                <label className="grid gap-2 text-xs text-white/50"><span>用户名</span><input required name="username" minLength={3} maxLength={30} pattern="[A-Za-z0-9_-]+" className="auth-input" placeholder="developer_01" /></label>
              </div>
            )}
            {signUp && !adminMode ? (
              <label className="grid gap-2 text-xs text-white/50"><span>邮箱</span><input required name="email" type="email" className="auth-input" placeholder="name@example.com" /></label>
            ) : (
              <label className="grid gap-2 text-xs text-white/50"><span>{adminMode ? "管理员账号" : "用户名或邮箱"}</span><input required name="identifier" className="auth-input" placeholder={adminMode ? "ageon-admin" : "username / email"} /></label>
            )}
            <label className="grid gap-2 text-xs text-white/50"><span>密码</span><span className="relative"><input required name="password" minLength={8} type={showPassword ? "text" : "password"} className="auth-input w-full pr-12" placeholder="至少 8 位" /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-0 top-0 grid h-12 w-12 place-items-center text-white/35 hover:text-white" aria-label="显示或隐藏密码">{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span></label>
            {signUp && !adminMode && <label className="flex cursor-pointer items-center gap-3 py-1 text-xs text-white/45"><input type="checkbox" checked={acceptedTerms} onChange={(event) => setAcceptedTerms(event.target.checked)} className="accent-[#9ef01a]" /><span>我同意社区规则与内容审核机制</span></label>}
            {error && <div className="border border-[#ff6b5f]/40 bg-[#ff6b5f]/8 p-3 text-sm text-[#ffaaa3]">{error}</div>}
            <button disabled={submitting || (signUp && !adminMode && !acceptedTerms)} className="interactive mt-2 flex h-12 items-center justify-center gap-2 bg-[#9ef01a] font-semibold text-black disabled:cursor-not-allowed disabled:opacity-40">{adminMode && <LockKeyhole size={16} />}{submitting ? "处理中..." : adminMode ? "进入管理端" : signUp ? "提交注册申请" : "登录"}<ArrowRight size={16} /></button>
          </form>
        </div>
      </section>
    </main>
  )
}
