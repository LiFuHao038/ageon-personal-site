"use client"

import Link from "next/link"
import { type FormEvent, useCallback, useEffect, useState } from "react"
import { ArrowLeft, Heart, LoaderCircle, MessageCircle, RefreshCw, ShieldCheck } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiClientError } from "@/lib/api-client"
import { createCommunityReply, getCommunityQuestion, likeCommunityQuestion, type CommunityQuestionResponse } from "@/lib/community-api"

export function CommunityDetail({ questionId }: { questionId: number }) {
  const { user } = useAuth()
  const [question, setQuestion] = useState<CommunityQuestionResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")

  const loadQuestion = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      setQuestion(await getCommunityQuestion(questionId))
    } catch (requestError) {
      setError(requestError instanceof ApiClientError && requestError.status === 404 ? "这个问题不存在或尚未公开。" : "问题详情加载失败，请确认后端服务正在运行。")
    } finally {
      setLoading(false)
    }
  }, [questionId])

  useEffect(() => { void loadQuestion() }, [loadQuestion])

  async function submitReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const content = String(new FormData(form).get("content") || "").trim()
    if (!content || submitting) return
    setSubmitting(true)
    setError("")
    try {
      setQuestion(await createCommunityReply(questionId, { content }))
      form.reset()
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "回复失败，请稍后再试。")
    } finally {
      setSubmitting(false)
    }
  }

  async function like() {
    if (!user || !question) return
    const key = `ageon-liked-question-${question.id}`
    if (window.sessionStorage.getItem(key)) {
      setError("本次浏览会话已经为这个问题点过赞。")
      return
    }
    try {
      setQuestion(await likeCommunityQuestion(question.id))
      window.sessionStorage.setItem(key, "1")
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "点赞失败，请稍后再试。")
    }
  }

  if (loading) return <div className="grid min-h-[55vh] place-items-center"><LoaderCircle className="animate-spin text-[#9ef01a]" /></div>

  if (!question) return (
    <div className="grid min-h-[55vh] place-items-center text-center"><div><p className="text-white/55">{error}</p><button type="button" onClick={loadQuestion} className="interactive mt-5 inline-flex h-10 items-center gap-2 border border-white/15 px-4 text-sm"><RefreshCw size={15} /> 重试</button></div></div>
  )

  return (
    <section className="py-8 md:py-12">
      <Link href="/community" className="interactive inline-flex h-10 items-center gap-2 border border-white/15 px-3 text-xs text-white/55"><ArrowLeft size={15} /> 返回社区</Link>

      <article className="mt-7 border-y border-white/15 bg-[#080a08]/55 py-8 backdrop-blur-sm md:py-12">
        <div className="flex flex-wrap items-center gap-3"><span className="mono text-[10px] text-[#9ef01a]">{question.tag}</span><span className="mono text-[10px] text-white/30">{question.time}</span><span className="mono border border-white/10 px-2 py-1 text-[9px] text-white/45">{question.status}</span></div>
        <h1 className="mt-5 max-w-4xl text-3xl leading-tight md:text-6xl">{question.title}</h1>
        <p className="mt-7 max-w-3xl whitespace-pre-wrap text-sm leading-8 text-white/62 md:text-base">{question.detail}</p>
        <div className="mt-8 flex flex-wrap items-center gap-4 border-t border-white/10 pt-5 text-white/40">
          <span className="mono text-[10px]">BY {question.author.toUpperCase()}</span>
          <span className="mono flex items-center gap-2 text-[10px]"><MessageCircle size={14} /> {question.replies}</span>
          <button type="button" disabled={!user} onClick={like} className="interactive mono flex h-9 items-center gap-2 border border-white/15 px-3 text-[10px] disabled:cursor-not-allowed disabled:opacity-35"><Heart size={14} /> {question.likes}</button>
        </div>
      </article>

      {error && <div className="mt-5 border border-[#ff6b5f]/40 bg-[#ff6b5f]/8 p-4 text-sm text-[#ffb3ad]">{error}</div>}

      <div className="mt-10 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div>
          <div className="flex items-end justify-between border-b border-white/15 pb-4"><h2 className="text-2xl">完整回复</h2><span className="mono text-[10px] text-white/35">{question.replyItems.length} REPLIES</span></div>
          <div className="grid">
            {question.replyItems.length ? question.replyItems.map((reply) => (
              <article key={reply.id} className="border-b border-white/12 py-6">
                <div className="flex flex-wrap items-center gap-3 text-xs"><span>{reply.author}</span>{reply.authorRole === "ADMIN" && <span className="mono inline-flex items-center gap-1 border border-[#9ef01a]/35 px-2 py-1 text-[9px] text-[#9ef01a]"><ShieldCheck size={11} /> 管理员回复</span>}<time className="mono text-[9px] text-white/28">{new Date(reply.createdAt).toLocaleString("zh-CN")}</time></div>
                <p className="mt-4 whitespace-pre-wrap text-sm leading-8 text-white/66">{reply.content}</p>
              </article>
            )) : <div className="py-16 text-center text-sm text-white/38">还没有回复，登录后参与讨论。</div>}
          </div>
        </div>

        <aside className="h-fit border border-white/15 bg-[#090b09]/80 p-5 backdrop-blur-sm">
          <p className="eyebrow">JOIN DISCUSSION</p>
          {user ? (
            <form onSubmit={submitReply} className="mt-5 grid gap-4"><div className="text-xs text-white/38">以 <span className="text-white/70">{user.displayName}</span> 身份回复</div><textarea name="content" required maxLength={2000} rows={7} className="resize-none border border-white/15 bg-black/25 p-3 text-sm leading-7 outline-none focus:border-[#9ef01a]" placeholder="写下你的观点或补充..." /><button disabled={submitting} className="interactive h-11 bg-[#9ef01a] text-sm font-semibold text-black disabled:opacity-45">{submitting ? "提交中..." : "提交回复"}</button></form>
          ) : (
            <div className="mt-5"><p className="text-sm leading-7 text-white/48">登录后参与讨论，也可以为有帮助的问题点赞。</p><Link href="/auth" className="interactive mt-5 flex h-11 items-center justify-center border border-[#9ef01a] text-sm text-[#9ef01a]">登录后参与讨论</Link></div>
          )}
        </aside>
      </div>
    </section>
  )
}
