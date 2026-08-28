"use client"

import { type FormEvent, useCallback, useEffect, useState } from "react"
import { Check, MessageSquareReply, RefreshCw, ShieldCheck, Trash2, UserCheck, X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiClientError } from "@/lib/api-client"
import type { AccountStatus, CurrentUser } from "@/lib/auth-api"
import { createAdminReply, deleteAdminQuestion, deleteAdminReply, getAdminOverview, listAdminQuestions, listAdminReplies, listAdminUsers, moderateAdminQuestion, updateAdminUserStatus, type AdminOverview, type AdminReply, type CommunityQuestionResponse } from "@/lib/admin-api"

type View = "users" | "questions" | "replies"

export function AdminDashboard() {
  const { user, loading: authLoading } = useAuth()
  const [view, setView] = useState<View>("users")
  const [overview, setOverview] = useState<AdminOverview | null>(null)
  const [users, setUsers] = useState<CurrentUser[]>([])
  const [questions, setQuestions] = useState<CommunityQuestionResponse[]>([])
  const [replies, setReplies] = useState<AdminReply[]>([])
  const [replyingQuestion, setReplyingQuestion] = useState<CommunityQuestionResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")

  const loadAll = useCallback(async () => {
    if (user?.role !== "ADMIN") return
    setBusy(true)
    setError("")
    try {
      const [nextOverview, nextUsers, nextQuestions, nextReplies] = await Promise.all([getAdminOverview(), listAdminUsers(), listAdminQuestions(), listAdminReplies()])
      setOverview(nextOverview); setUsers(nextUsers); setQuestions(nextQuestions); setReplies(nextReplies)
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "管理数据加载失败。")
    } finally { setBusy(false) }
  }, [user])

  useEffect(() => { void loadAll() }, [loadAll])

  async function changeUser(id: number, status: AccountStatus) {
    setError("")
    setNotice("")
    setUsers((current) => current.map((item) => item.id === id ? { ...item, status } : item))
    try {
      const updated = await updateAdminUserStatus(id, status)
      setUsers((current) => current.map((item) => item.id === id ? updated : item))
      setOverview(await getAdminOverview())
      setNotice(status === "APPROVED" ? "用户已审核通过。" : "用户已拒绝。")
    } catch {
      setError("用户状态更新失败，请刷新重试。")
      void loadAll()
    }
  }

  async function moderate(id: number, moderationStatus: "PUBLISHED" | "REJECTED") {
    try { const updated = await moderateAdminQuestion(id, moderationStatus); setQuestions((current) => current.map((item) => item.id === id ? updated : item)); setOverview(await getAdminOverview()) } catch { setError("问题审核失败，请刷新重试。") }
  }

  async function removeQuestion(id: number) {
    if (!window.confirm("确认永久删除这个问题及其回复？")) return
    try { await deleteAdminQuestion(id); setQuestions((current) => current.filter((item) => item.id !== id)); setOverview(await getAdminOverview()) } catch { setError("问题删除失败。") }
  }

  async function submitAdminReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!replyingQuestion) return
    const content = String(new FormData(event.currentTarget).get("content") || "").trim()
    if (!content) return
    setBusy(true)
    try { await createAdminReply(replyingQuestion.id, content); setReplyingQuestion(null); await loadAll() } catch { setError("管理员回复提交失败。") } finally { setBusy(false) }
  }

  async function removeReply(id: number) {
    if (!window.confirm("确认删除这条回复？")) return
    try { await deleteAdminReply(id); setReplies((current) => current.filter((item) => item.id !== id)); setOverview(await getAdminOverview()) } catch { setError("回复删除失败。") }
  }

  if (authLoading) return <div className="grid min-h-[60vh] place-items-center"><RefreshCw className="animate-spin text-[#9ef01a]" /></div>
  if (!user) return <AdminState title="需要管理员登录" detail="请从管理员登录页进入管理控制台。" href="/admin/login" action="前往登录" />
  if (user.role !== "ADMIN") return <AdminState title="无权访问" detail="当前账号不是管理员，后端也会拒绝所有管理接口。" href="/" action="返回首页" />

  const stats = [{ label: "待审核用户", value: overview?.pendingUsers ?? 0 }, { label: "待审核问题", value: overview?.pendingQuestions ?? 0 }, { label: "已发布问题", value: overview?.publishedQuestions ?? 0 }, { label: "全部回复", value: overview?.totalReplies ?? 0 }]

  return (
    <section>
      <header className="flex flex-col gap-5 border-b border-white/15 pb-8 md:flex-row md:items-end md:justify-between"><div><p className="eyebrow">ADMIN CONSOLE</p><h1 className="mt-4 text-4xl md:text-6xl">社区管理端</h1></div><button type="button" onClick={loadAll} className="interactive flex h-10 items-center justify-center gap-2 border border-white/15 px-3 text-xs"><RefreshCw size={14} className={busy ? "animate-spin" : ""} /> 刷新数据</button></header>
      {notice && <div className="mt-5 border border-[#9ef01a]/35 bg-[#9ef01a]/8 p-4 text-sm text-[#b9ff58]" role="status">{notice}</div>}
      {error && <div className="mt-5 border border-[#ff6b5f]/40 bg-[#ff6b5f]/8 p-4 text-sm text-[#ffb3ad]">{error}</div>}
      <div className="grid border-l border-t border-white/15 sm:grid-cols-2 lg:grid-cols-4">{stats.map((stat) => <div key={stat.label} className="border-b border-r border-white/15 bg-[#090b09]/75 p-5"><span className="mono text-[9px] text-white/35">{stat.label}</span><strong className="mt-3 block text-3xl font-medium">{stat.value}</strong></div>)}</div>
      <div className="mt-8 flex gap-2 overflow-x-auto">{([ ["users", "用户审核"], ["questions", "问题管理"], ["replies", "回复管理"] ] as const).map(([key, label]) => <button key={key} onClick={() => setView(key)} className={`h-10 shrink-0 border px-4 text-sm ${view === key ? "border-[#9ef01a] bg-[#9ef01a]/8 text-[#9ef01a]" : "border-white/15 text-white/50"}`}>{label}</button>)}</div>

      <div className="mt-5 overflow-x-auto border border-white/15 bg-[#080a08]/72 backdrop-blur-sm">
        {view === "users" && <UsersTable users={users} onChange={changeUser} />}
        {view === "questions" && <QuestionsTable questions={questions} onModerate={moderate} onDelete={removeQuestion} onReply={setReplyingQuestion} />}
        {view === "replies" && <RepliesTable replies={replies} onDelete={removeReply} />}
      </div>

      {replyingQuestion && <div className="fixed inset-0 z-[90] grid place-items-center bg-black/80 p-4 backdrop-blur-sm"><form onSubmit={submitAdminReply} className="w-full max-w-xl border border-white/20 bg-[#090b09] p-6"><div className="flex items-start justify-between gap-4"><div><p className="eyebrow">管理员回复</p><h2 className="mt-3 text-2xl">{replyingQuestion.title}</h2></div><button type="button" onClick={() => setReplyingQuestion(null)} className="grid h-9 w-9 place-items-center border border-white/15"><X size={16} /></button></div><textarea name="content" required maxLength={2000} rows={7} className="mt-6 w-full resize-none border border-white/15 bg-black/30 p-3 text-sm leading-7 outline-none focus:border-[#9ef01a]" placeholder="以管理员身份回复..." /><button disabled={busy} className="mt-4 h-11 w-full bg-[#9ef01a] text-sm font-semibold text-black disabled:opacity-45">提交管理员回复</button></form></div>}
    </section>
  )
}

function AdminState({ title, detail, href, action }: { title: string; detail: string; href: string; action: string }) { return <div className="grid min-h-[65vh] place-items-center text-center"><div><ShieldCheck className="mx-auto text-[#9ef01a]" size={36} /><h1 className="mt-5 text-3xl">{title}</h1><p className="mt-3 text-sm text-white/45">{detail}</p><a href={href} className="interactive mt-6 inline-flex h-11 items-center border border-[#9ef01a] px-4 text-sm text-[#9ef01a]">{action}</a></div></div> }

function UsersTable({ users, onChange }: { users: CurrentUser[]; onChange: (id: number, status: AccountStatus) => void }) { return <table className="w-full min-w-[760px] text-left text-sm"><thead className="mono border-b border-white/15 text-[9px] text-white/35"><tr><th className="p-4">用户</th><th className="p-4">账号</th><th className="p-4">状态</th><th className="p-4 text-right">操作</th></tr></thead><tbody>{users.map((item) => <tr key={item.id} className="border-b border-white/10"><td className="p-4"><div>{item.displayName}</div><div className="mt-1 text-xs text-white/32">{item.email}</div></td><td className="p-4 text-white/55">{item.username}</td><td className="p-4"><Status value={item.status} /></td><td className="p-4"><div className="flex justify-end gap-2">{item.role !== "ADMIN" && <><IconButton label="通过" disabled={item.status === "APPROVED"} onClick={() => onChange(item.id, "APPROVED")}><UserCheck size={15} /></IconButton><IconButton label="拒绝" disabled={item.status === "REJECTED"} onClick={() => onChange(item.id, "REJECTED")}><X size={15} /></IconButton></>}</div></td></tr>)}</tbody></table> }
function QuestionsTable({ questions, onModerate, onDelete, onReply }: { questions: CommunityQuestionResponse[]; onModerate: (id: number, status: "PUBLISHED" | "REJECTED") => void; onDelete: (id: number) => void; onReply: (question: CommunityQuestionResponse) => void }) { return <table className="w-full min-w-[900px] text-left text-sm"><thead className="mono border-b border-white/15 text-[9px] text-white/35"><tr><th className="p-4">问题</th><th className="p-4">作者</th><th className="p-4">状态</th><th className="p-4 text-right">操作</th></tr></thead><tbody>{questions.map((item) => <tr key={item.id} className="border-b border-white/10"><td className="max-w-md p-4"><div>{item.title}</div><div className="mt-1 line-clamp-1 text-xs text-white/32">{item.detail}</div></td><td className="p-4 text-white/55">{item.author}</td><td className="p-4"><Status value={item.moderationStatus} /></td><td className="p-4"><div className="flex justify-end gap-2"><IconButton label="发布" onClick={() => onModerate(item.id, "PUBLISHED")}><Check size={15} /></IconButton><IconButton label="驳回" onClick={() => onModerate(item.id, "REJECTED")}><X size={15} /></IconButton><IconButton label="管理员回复" onClick={() => onReply(item)}><MessageSquareReply size={15} /></IconButton><IconButton label="删除" onClick={() => onDelete(item.id)}><Trash2 size={15} /></IconButton></div></td></tr>)}</tbody></table> }
function RepliesTable({ replies, onDelete }: { replies: AdminReply[]; onDelete: (id: number) => void }) { return <table className="w-full min-w-[760px] text-left text-sm"><thead className="mono border-b border-white/15 text-[9px] text-white/35"><tr><th className="p-4">回复</th><th className="p-4">问题</th><th className="p-4">作者</th><th className="p-4 text-right">操作</th></tr></thead><tbody>{replies.map((item) => <tr key={item.id} className="border-b border-white/10"><td className="max-w-md p-4"><div className="line-clamp-2 leading-6 text-white/65">{item.content}</div></td><td className="p-4 text-white/45">{item.questionTitle}</td><td className="p-4">{item.author}{item.authorRole === "ADMIN" && <span className="ml-2 text-[9px] text-[#9ef01a]">ADMIN</span>}</td><td className="p-4 text-right"><IconButton label="删除回复" onClick={() => onDelete(item.id)}><Trash2 size={15} /></IconButton></td></tr>)}</tbody></table> }
function Status({ value }: { value: string }) { return <span className={`mono border px-2 py-1 text-[9px] ${value === "APPROVED" || value === "PUBLISHED" ? "border-[#9ef01a]/30 text-[#9ef01a]" : value === "PENDING" ? "border-[#ffd166]/30 text-[#ffd166]" : "border-[#ff6b5f]/30 text-[#ff8c83]"}`}>{value}</span> }
function IconButton({ label, onClick, children, disabled = false }: { label: string; onClick: () => void; children: React.ReactNode; disabled?: boolean }) { return <button type="button" title={label} aria-label={label} onClick={onClick} disabled={disabled} className="interactive inline-grid h-9 w-9 place-items-center border border-white/15 text-white/55 hover:text-[#9ef01a] disabled:cursor-not-allowed disabled:opacity-25 disabled:hover:text-white/55">{children}</button> }
