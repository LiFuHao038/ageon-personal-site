"use client"

import Link from "next/link"
import { useCallback, useEffect, useReducer, useRef, useState, type FormEvent } from "react"
import {
  ArrowUp,
  Bot,
  History,
  LoaderCircle,
  LogIn,
  Menu,
  MessageSquarePlus,
  Square,
  Trash2,
  UserRound,
  X,
} from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import {
  AiStreamEventError,
  createAiConversation,
  deleteAiConversation,
  getAiConversation,
  getAiQuota,
  listAiConversations,
  streamAiMessage,
  type AiConversationSummary,
  type AiQuota,
} from "@/lib/ai-api"
import { ApiClientError } from "@/lib/api-client"
import { toAiErrorMessage } from "@/lib/ai-errors"
import { initialAiStreamRuntime, reduceAiStreamRuntime } from "@/lib/ai-stream-state"

type ChatMessage = {
  id: string
  role: "assistant" | "user"
  content: string
  status: "completed" | "failed" | "streaming"
}

const suggestions = [
  "TCP 为什么需要三次握手？",
  "HashMap 的底层原理是什么？",
  "RAG 检索质量应该如何评估？",
]

export function AiChat() {
  const { user, token, loading: authLoading } = useAuth()
  const [conversations, setConversations] = useState<AiConversationSummary[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [quota, setQuota] = useState<AiQuota | null>(null)
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [streamRuntime, dispatchStream] = useReducer(reduceAiStreamRuntime, initialAiStreamRuntime)
  const generating = streamRuntime.generating
  const [error, setError] = useState("")
  const [historyOpen, setHistoryOpen] = useState(false)
  const abortController = useRef<AbortController | null>(null)
  const transcriptRef = useRef<HTMLDivElement | null>(null)

  const openConversation = useCallback(async (conversationId: number, accessToken: string) => {
    setLoading(true)
    setError("")
    try {
      const detail = await getAiConversation(conversationId, accessToken)
      setSelectedId(detail.id)
      setMessages(detail.messages.map((message) => ({
        id: String(message.id),
        role: message.role === "USER" ? "user" : "assistant",
        content: message.content,
        status: message.status === "FAILED" ? "failed" : "completed",
      })))
      setHistoryOpen(false)
    } catch (requestError) {
      setError(messageFromError(requestError, "无法加载这段对话。"))
    } finally {
      setLoading(false)
    }
  }, [])

  const loadWorkspace = useCallback(async (accessToken: string) => {
    setLoading(true)
    setError("")
    try {
      const [items, currentQuota] = await Promise.all([
        listAiConversations(accessToken),
        getAiQuota(accessToken),
      ])
      setConversations(items)
      setQuota(currentQuota)
      if (items.length > 0) await openConversation(items[0].id, accessToken)
      else {
        setSelectedId(null)
        setMessages([])
      }
    } catch (requestError) {
      setError(messageFromError(requestError, "AI 服务暂时无法访问，请确认后端正在运行。"))
    } finally {
      setLoading(false)
    }
  }, [openConversation])

  useEffect(() => {
    if (token && user?.status === "APPROVED") void loadWorkspace(token)
    if (!token) {
      setConversations([])
      setSelectedId(null)
      setMessages([])
      setQuota(null)
    }
  }, [token, user?.status, loadWorkspace])

  useEffect(() => {
    transcriptRef.current?.scrollTo({ top: transcriptRef.current.scrollHeight, behavior: "smooth" })
  }, [messages])

  async function startNewConversation() {
    if (!token || generating) return
    setError("")
    try {
      const created = await createAiConversation(token)
      setConversations((current) => [created, ...current])
      setSelectedId(created.id)
      setMessages([])
      setHistoryOpen(false)
    } catch (requestError) {
      setError(messageFromError(requestError, "无法创建新对话。"))
    }
  }

  async function removeConversation(conversationId: number) {
    if (!token || generating || !window.confirm("确定删除这段对话吗？删除后无法恢复。")) return
    setError("")
    try {
      await deleteAiConversation(conversationId, token)
      const remaining = conversations.filter((item) => item.id !== conversationId)
      setConversations(remaining)
      if (selectedId === conversationId) {
        if (remaining.length > 0) await openConversation(remaining[0].id, token)
        else {
          setSelectedId(null)
          setMessages([])
        }
      }
    } catch (requestError) {
      setError(messageFromError(requestError, "删除对话失败。"))
    }
  }

  async function sendQuestion(question: string) {
    const normalized = question.trim()
    if (!normalized || generating) return
    if (!token) {
      setError("登录后才能使用 AI 问答。")
      return
    }
    if (quota?.remaining === 0) {
      setError("今日 AI 额度已用完，明天会自动恢复。")
      return
    }

    dispatchStream({ type: "start" })
    setError("")
    setInput("")
    let conversationId = selectedId
    try {
      if (conversationId == null) {
        const created = await createAiConversation(token)
        conversationId = created.id
        setSelectedId(created.id)
        setConversations((current) => [created, ...current])
      }

      const userMessageId = `user-${Date.now()}`
      const assistantMessageId = `assistant-${Date.now()}`
      setMessages((current) => [
        ...current,
        { id: userMessageId, role: "user", content: normalized, status: "completed" },
        { id: assistantMessageId, role: "assistant", content: "", status: "streaming" },
      ])

      const controller = new AbortController()
      abortController.current = controller
      await streamAiMessage({
        conversationId,
        content: normalized,
        token,
        signal: controller.signal,
        onModelStatus(status) {
          dispatchStream({ type: "fallback", model: status.model })
        },
        onMessage(delta) {
          dispatchStream({ type: "delta" })
          setMessages((current) => current.map((message) =>
            message.id === assistantMessageId ? { ...message, content: message.content + delta } : message
          ))
        },
        onQuota: setQuota,
        onDone(done) {
          dispatchStream({ type: "done" })
          setMessages((current) => current.map((message) =>
            message.id === assistantMessageId
              ? { ...message, id: String(done.messageId), status: "completed" }
              : message
          ))
          setConversations((current) => current.map((item) =>
            item.id === done.conversationId
              ? { ...item, title: done.title, messageCount: item.messageCount + 2, updatedAt: new Date().toISOString() }
              : item
          ).sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt)))
        },
        onError(streamError) {
          dispatchStream({ type: "error" })
          setMessages((current) => current.map((message) =>
            message.id === assistantMessageId
              ? { ...message, content: toAiErrorMessage(streamError.code, streamError.message), status: "failed" }
              : message
          ))
        },
      })
    } catch (requestError) {
      if (!(requestError instanceof AiStreamEventError)) {
        setError(abortController.current?.signal.aborted
          ? "已停止生成，本次请求不会消耗额度。"
          : messageFromError(requestError, "AI 回复失败，请稍后重试。"))
      }
      if (conversationId != null) await openConversation(conversationId, token)
      setQuota(await getAiQuota(token).catch(() => quota))
    } finally {
      const wasAborted = abortController.current?.signal.aborted ?? false
      abortController.current = null
      dispatchStream({ type: wasAborted ? "abort" : "done" })
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void sendQuestion(input)
  }

  if (authLoading) return <AiLoading label="正在检查登录状态" />
  if (!user || !token) return <AiLoginRequired />
  if (user.status !== "APPROVED") return <AiUnavailable message="账号通过管理员审核后即可使用 AI 问答。" />

  const history = (
    <ConversationHistory
      conversations={conversations}
      selectedId={selectedId}
      quota={quota}
      generating={generating}
      onCreate={() => void startNewConversation()}
      onSelect={(id) => void openConversation(id, token)}
      onDelete={(id) => void removeConversation(id)}
    />
  )

  return (
    <section className="py-10 md:py-14">
      {error && <div className="mb-4 flex items-center justify-between gap-4 border border-[#ff6b5f]/45 bg-[#ff6b5f]/8 p-4 text-sm text-[#ffb3ad]"><span>{error}</span><button type="button" onClick={() => setError("")} aria-label="关闭错误"><X size={16} /></button></div>}

      <div className="grid gap-4 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="hidden h-[680px] border border-white/15 bg-[#080a08]/80 lg:block">{history}</aside>

        {historyOpen && (
          <div className="fixed inset-0 z-[70] lg:hidden">
            <button type="button" className="absolute inset-0 bg-black/75" onClick={() => setHistoryOpen(false)} aria-label="关闭会话列表" />
            <aside className="absolute inset-y-0 left-0 w-[min(88vw,340px)] border-r border-white/15 bg-[#080a08]">{history}</aside>
          </div>
        )}

        <div className="flex h-[680px] min-w-0 flex-col border border-white/15 bg-[#0a0c0a]/88">
          <div className="flex min-h-16 items-center justify-between gap-3 border-b border-white/15 px-4 md:px-5">
            <div className="flex min-w-0 items-center gap-3">
              <button type="button" onClick={() => setHistoryOpen(true)} className="grid h-9 w-9 shrink-0 place-items-center border border-white/15 lg:hidden" aria-label="打开会话列表"><Menu size={17} /></button>
              <span className="grid h-9 w-9 shrink-0 place-items-center bg-[#9ef01a] text-black"><Bot size={18} /></span>
              <div className="min-w-0"><p className="truncate text-sm">{conversations.find((item) => item.id === selectedId)?.title ?? "AGEON Technical Assistant"}</p><p className="mono mt-1 text-[9px] text-white/35">QWEN + KIMI FALLBACK / AUTHENTICATED</p></div>
            </div>
            <span className={`mono shrink-0 text-[9px] ${generating ? "text-[#55d6be]" : "text-[#9ef01a]"}`}>{generating ? "GENERATING" : "READY"}</span>
          </div>

          {streamRuntime.modelNotice && <div className="border-b border-[#55d6be]/25 bg-[#55d6be]/8 px-4 py-2 text-xs text-[#9beadd]" role="status">{streamRuntime.modelNotice}</div>}

          <div ref={transcriptRef} className="flex-1 space-y-6 overflow-y-auto p-4 md:p-6" aria-live="polite">
            {loading ? <AiLoading label="正在加载对话" compact /> : messages.length === 0 ? (
              <div className="grid min-h-full place-items-center py-10 text-center">
                <div className="max-w-md"><Bot size={28} className="mx-auto text-[#9ef01a]" /><h2 className="mt-5 text-2xl">从一个技术问题开始</h2><p className="mt-3 text-sm leading-7 text-white/45">回答由主备模型协同生成，会话记录仅对当前账号可见。</p><div className="mt-6 flex flex-wrap justify-center gap-2">{suggestions.map((suggestion) => <button key={suggestion} type="button" onClick={() => void sendQuestion(suggestion)} className="interactive border border-white/15 px-3 py-2 text-xs text-white/55">{suggestion}</button>)}</div></div>
              </div>
            ) : messages.map((message) => (
              <div key={message.id} className={`flex gap-3 ${message.role === "user" ? "justify-end" : "justify-start"}`}>
                {message.role === "assistant" && <span className="grid h-8 w-8 shrink-0 place-items-center border border-white/15"><Bot size={15} /></span>}
                <div className={`max-w-[84%] whitespace-pre-wrap border p-4 text-sm leading-7 md:max-w-[74%] ${message.role === "user" ? "border-[#9ef01a]/40 bg-[#9ef01a]/10" : message.status === "failed" ? "border-[#ff6b5f]/35 bg-[#ff6b5f]/6 text-[#ffd0cc]" : "border-white/15 bg-white/[0.025]"}`}>
                  {message.content || (message.status === "streaming" ? <span className="inline-flex gap-1" aria-label="正在生成"><i className="h-1.5 w-1.5 animate-pulse bg-[#9ef01a]" /><i className="h-1.5 w-1.5 animate-pulse bg-[#9ef01a] [animation-delay:140ms]" /><i className="h-1.5 w-1.5 animate-pulse bg-[#9ef01a] [animation-delay:280ms]" /></span> : "")}
                </div>
                {message.role === "user" && <span className="grid h-8 w-8 shrink-0 place-items-center bg-white text-black"><UserRound size={15} /></span>}
              </div>
            ))}
          </div>

          <div className="border-t border-white/15 p-4 md:p-5">
            <form onSubmit={submit} className="flex gap-2">
              <label className="flex min-h-12 flex-1 items-center border border-white/15 bg-black/25 px-4 focus-within:border-[#9ef01a]">
                <span className="sr-only">输入问题</span>
                <input id="ai-question-input" name="question" value={input} onChange={(event) => setInput(event.target.value)} maxLength={4000} disabled={generating || quota?.remaining === 0} className="w-full bg-transparent text-sm outline-none placeholder:text-white/35" placeholder={quota?.remaining === 0 ? "今日额度已用完" : "输入你的技术问题..."} />
              </label>
              {generating ? <button type="button" onClick={() => abortController.current?.abort()} className="grid h-12 w-12 place-items-center border border-[#ff6b5f]/60 text-[#ff8b82]" aria-label="停止生成"><Square size={16} fill="currentColor" /></button> : <button type="submit" disabled={!input.trim() || quota?.remaining === 0} className="grid h-12 w-12 place-items-center bg-[#9ef01a] text-black disabled:cursor-not-allowed disabled:opacity-30" aria-label="发送"><ArrowUp size={18} /></button>}
            </form>
            <p className="mono mt-3 text-center text-[9px] text-white/32">AI 可能出错，请核对关键技术结论</p>
          </div>
        </div>
      </div>
    </section>
  )
}

function ConversationHistory(props: {
  conversations: AiConversationSummary[]
  selectedId: number | null
  quota: AiQuota | null
  generating: boolean
  onCreate: () => void
  onSelect: (id: number) => void
  onDelete: (id: number) => void
}) {
  return <div className="flex h-full flex-col">
    <div className="border-b border-white/15 p-4"><button type="button" onClick={props.onCreate} disabled={props.generating} className="interactive flex h-11 w-full items-center justify-center gap-2 bg-[#9ef01a] text-sm font-semibold text-black disabled:opacity-40"><MessageSquarePlus size={16} /> 新建对话</button></div>
    <div className="flex items-center justify-between border-b border-white/10 px-4 py-3"><span className="mono text-[10px] text-white/38">CONVERSATIONS</span><History size={14} className="text-white/35" /></div>
    <div className="min-h-0 flex-1 overflow-y-auto">
      {props.conversations.length === 0 ? <p className="p-5 text-sm leading-7 text-white/38">暂无历史对话</p> : props.conversations.map((conversation) => <div key={conversation.id} className={`group flex items-center border-b border-white/10 ${conversation.id === props.selectedId ? "bg-white/[0.055]" : ""}`}><button type="button" onClick={() => props.onSelect(conversation.id)} disabled={props.generating} className="min-w-0 flex-1 px-4 py-4 text-left disabled:cursor-not-allowed"><span className="block truncate text-sm">{conversation.title}</span><span className="mono mt-1 block text-[9px] text-white/30">{conversation.messageCount} MESSAGES</span></button><button type="button" onClick={() => props.onDelete(conversation.id)} disabled={props.generating} className="mr-3 grid h-8 w-8 shrink-0 place-items-center text-white/30 transition hover:text-[#ff8b82] disabled:opacity-20" aria-label={`删除对话 ${conversation.title}`}><Trash2 size={14} /></button></div>)}
    </div>
    <div className="border-t border-white/15 p-4"><div className="flex items-end justify-between"><div><p className="mono text-[9px] text-white/35">今日剩余</p><p className="mt-1 text-2xl">{props.quota?.remaining ?? "--"}<span className="ml-1 text-sm text-white/35">/ {props.quota?.dailyLimit ?? 20}</span></p></div>{props.quota && <p className="mono text-[9px] text-white/28">{props.quota.date}</p>}</div><div className="mt-3 h-1 bg-white/10"><div className="h-full bg-[#9ef01a] transition-[width]" style={{ width: `${props.quota ? (props.quota.remaining / props.quota.dailyLimit) * 100 : 0}%` }} /></div></div>
  </div>
}

function AiLoading({ label, compact = false }: { label: string; compact?: boolean }) {
  return <div className={`grid place-items-center ${compact ? "min-h-full" : "min-h-[420px] border border-white/15"}`}><span className="mono flex items-center gap-3 text-[10px] text-white/45"><LoaderCircle size={16} className="animate-spin text-[#9ef01a]" /> {label}</span></div>
}

function AiLoginRequired() {
  return <AiUnavailable message="登录并通过管理员审核后，可以保存会话并使用每日 AI 额度。" action={<Link href="/auth" className="interactive inline-flex h-11 items-center gap-2 bg-[#9ef01a] px-5 text-sm font-semibold text-black"><LogIn size={16} /> 登录 / 注册</Link>} />
}

function AiUnavailable({ message, action }: { message: string; action?: React.ReactNode }) {
  return <section className="grid min-h-[480px] place-items-center py-10 text-center"><div className="max-w-md border border-white/15 bg-[#0a0c0a]/85 p-8"><Bot size={28} className="mx-auto text-[#9ef01a]" /><h2 className="mt-5 text-2xl">AI 问答需要账号权限</h2><p className="mt-3 text-sm leading-7 text-white/48">{message}</p>{action && <div className="mt-6 flex justify-center">{action}</div>}</div></section>
}

function messageFromError(error: unknown, fallback: string) {
  if (error instanceof ApiClientError || error instanceof AiStreamEventError) {
    return toAiErrorMessage(error.code, error.message)
  }
  return fallback
}
