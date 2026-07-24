"use client"

import Link from "next/link"
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react"
import { ArrowUpRight, Heart, LogIn, MessageCircle, Plus, RefreshCw, Search, SlidersHorizontal, X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiClientError } from "@/lib/api-client"
import { createCommunityQuestion, listCommunityQuestions, type CommunityQuestionResponse } from "@/lib/community-api"

const tags = ["全部", "Java 后端", "AI 应用", "计算机网络", "数据库"]

export function CommunityBoard() {
  const { user } = useAuth()
  const [questions, setQuestions] = useState<CommunityQuestionResponse[]>([])
  const [query, setQuery] = useState("")
  const [tag, setTag] = useState("全部")
  const [composerOpen, setComposerOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState("")
  const [error, setError] = useState("")

  const loadQuestions = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      setQuestions(await listCommunityQuestions({ keyword: query.trim(), tag }))
    } catch {
      setError("后端服务暂时无法访问，请确认 Spring Boot 正在运行。")
    } finally {
      setLoading(false)
    }
  }, [query, tag])

  useEffect(() => { void loadQuestions() }, [loadQuestions])

  const answeredCount = useMemo(() => questions.filter((item) => item.status === "已回复").length, [questions])

  function openComposer() {
    if (!user) {
      setError("登录并通过管理员审核后，才可以发布问题。")
      return
    }
    setComposerOpen(true)
  }

  async function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const title = String(formData.get("title") || "").trim()
    const detail = String(formData.get("detail") || "").trim()
    const questionTag = String(formData.get("tag") || "Java 后端")
    if (!title || !detail || submitting) return

    setSubmitting(true)
    setError("")
    try {
      await createCommunityQuestion({ title, detail, tag: questionTag })
      setComposerOpen(false)
      setNotice("问题已提交，管理员审核发布后会出现在社区列表。")
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "发布失败，请检查登录状态与后端服务。")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="py-10 md:py-14">
      {(error || notice) && (
        <div className={`mb-5 flex flex-col gap-3 border p-4 text-sm sm:flex-row sm:items-center sm:justify-between ${error ? "border-[#ff6b5f]/45 bg-[#ff6b5f]/8 text-[#ffb3ad]" : "border-[#9ef01a]/35 bg-[#9ef01a]/8 text-[#cfff84]"}`}>
          <span>{error || notice}</span>
          {error && !user ? <Link href="/auth" className="interactive inline-flex h-9 items-center justify-center gap-2 border border-current px-3 text-xs"><LogIn size={14} /> 登录 / 注册</Link> : <button type="button" onClick={() => { setError(""); setNotice("") }} className="text-xs">关闭</button>}
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
        <label className="flex h-12 items-center gap-3 border border-white/15 bg-[#0c0e0c]/90 px-4 focus-within:border-[#9ef01a]">
          <Search size={17} className="text-white/40" />
          <input value={query} onChange={(event) => setQuery(event.target.value)} className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-white/28" placeholder="搜索问题、标签或关键词" />
        </label>
        <button type="button" onClick={openComposer} className="interactive flex h-12 items-center justify-center gap-2 border border-[#9ef01a] bg-[#9ef01a] px-5 text-sm font-semibold text-black"><Plus size={17} /> 发布问题</button>
      </div>

      <div className="mt-4 flex gap-2 overflow-x-auto pb-2" aria-label="问题标签筛选">
        {tags.map((item) => <button key={item} type="button" onClick={() => setTag(item)} className={`interactive h-9 shrink-0 border px-3 text-xs ${tag === item ? "border-white bg-white text-black" : "border-white/15 text-white/55"}`}>{item}</button>)}
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_260px]">
        <div className="border-t border-white/15">
          {loading ? (
            <div className="grid min-h-72 place-items-center border-b border-white/15"><div className="mono flex items-center gap-3 text-[11px] text-white/45"><RefreshCw size={16} className="animate-spin" /> LOADING API DATA</div></div>
          ) : questions.length > 0 ? questions.map((question) => (
            <article key={question.id} className="group border-b border-white/15 py-6 md:py-8">
              <div className="flex flex-wrap items-center gap-3">
                <span className="mono text-[10px] text-[#9ef01a]">{question.tag}</span>
                <span className="mono text-[10px] text-white/30">{question.time}</span>
                <span className={`mono px-2 py-1 text-[9px] ${question.status === "已回复" ? "bg-[#9ef01a]/12 text-[#9ef01a]" : "bg-white/6 text-white/45"}`}>{question.status}</span>
              </div>
              <div className="mt-4 grid gap-5 sm:grid-cols-[1fr_auto] sm:items-center">
                <div>
                  <Link href={`/community/${question.id}`} className="interactive inline-block"><h2 className="text-xl transition-transform group-hover:translate-x-1 md:text-2xl">{question.title}</h2></Link>
                  <p className="mt-2 max-w-2xl line-clamp-2 text-sm leading-7 text-white/48">{question.detail}</p>
                </div>
                <div className="flex items-center gap-4 text-white/40">
                  <span className="mono flex items-center gap-2 text-[11px]"><MessageCircle size={15} /> {question.replies}</span>
                  <span className="mono flex items-center gap-2 text-[11px]"><Heart size={14} /> {question.likes}</span>
                  <Link href={`/community/${question.id}`} className="interactive grid h-9 w-9 place-items-center border border-white/15 hover:text-[#9ef01a]" aria-label={`查看 ${question.title}`}><ArrowUpRight size={16} /></Link>
                </div>
              </div>
              <p className="mono mt-4 text-[10px] text-white/28">BY {question.author.toUpperCase()}</p>
            </article>
          )) : (
            <div className="grid min-h-72 place-items-center border-b border-white/15 text-center"><div><Search className="mx-auto text-white/25" /><p className="mt-4 text-white/60">没有找到相关问题</p><button type="button" onClick={() => { setQuery(""); setTag("全部") }} className="mt-3 text-sm text-[#9ef01a]">清空筛选</button></div></div>
          )}
        </div>

        <aside className="h-fit border border-white/15 bg-[#080a08]/75 p-5 backdrop-blur-sm">
          <div className="flex items-center gap-2 text-sm"><SlidersHorizontal size={16} /> 社区状态</div>
          <dl className="mono mt-6 grid gap-4 text-[10px]"><div className="flex justify-between border-b border-white/10 pb-3"><dt className="text-white/35">QUESTIONS</dt><dd>{questions.length}</dd></div><div className="flex justify-between border-b border-white/10 pb-3"><dt className="text-white/35">ANSWERED</dt><dd>{answeredCount}</dd></div><div className="flex justify-between"><dt className="text-white/35">MODE</dt><dd className="text-[#9ef01a]">MODERATED</dd></div></dl>
        </aside>
      </div>

      {composerOpen && (
        <div className="fixed inset-0 z-[80] grid place-items-center bg-black/80 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="composer-title">
          <div className="w-full max-w-2xl border border-white/20 bg-[#0b0d0b] p-5 md:p-7">
            <div className="flex items-center justify-between"><div><p className="eyebrow">NEW THREAD</p><h2 id="composer-title" className="mt-2 text-2xl">发布问题</h2></div><button type="button" onClick={() => setComposerOpen(false)} className="grid h-10 w-10 place-items-center border border-white/15" aria-label="关闭"><X size={18} /></button></div>
            <form onSubmit={publish} className="mt-7 grid gap-5">
              <label className="grid gap-2 text-sm"><span className="text-white/55">标题</span><input name="title" required maxLength={100} className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" placeholder="用一句话描述你的问题" /></label>
              <label className="grid gap-2 text-sm"><span className="text-white/55">补充说明</span><textarea name="detail" required maxLength={1000} rows={5} className="resize-none border border-white/15 bg-black/25 p-4 outline-none focus:border-[#9ef01a]" placeholder="说明背景、尝试过的方法或期望结果" /></label>
              <label className="grid gap-2 text-sm"><span className="text-white/55">标签</span><select name="tag" className="h-12 border border-white/15 bg-[#0b0d0b] px-4 outline-none focus:border-[#9ef01a]">{tags.slice(1).map((item) => <option key={item}>{item}</option>)}</select></label>
              <p className="text-xs leading-6 text-white/38">发布后进入管理员审核队列，通过后公开展示。</p>
              <div className="flex justify-end gap-2"><button type="button" onClick={() => setComposerOpen(false)} className="h-11 border border-white/15 px-4 text-sm">取消</button><button type="submit" disabled={submitting} className="h-11 bg-[#9ef01a] px-5 text-sm font-semibold text-black disabled:opacity-50">{submitting ? "提交中..." : "提交审核"}</button></div>
            </form>
          </div>
        </div>
      )}
    </section>
  )
}
