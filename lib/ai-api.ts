import { EventStreamContentType, fetchEventSource, type EventSourceMessage } from "@microsoft/fetch-event-source"
import { API_BASE_URL, ApiClientError, apiRequest, type ApiErrorPayload } from "@/lib/api-client"

export type AiConversationSummary = {
  id: number
  title: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

export type AiMessage = {
  id: number
  role: "USER" | "ASSISTANT"
  status: "COMPLETED" | "FAILED"
  content: string
  createdAt: string
}

export type AiConversationDetail = {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messages: AiMessage[]
}

export type AiQuota = {
  date: string
  dailyLimit: number
  used: number
  remaining: number
  resetsAt: string
}

export type AiStreamDone = {
  messageId: number
  conversationId: number
  title: string
}

export type AiStreamError = {
  code: string
  message: string
}

export type AiModelStatus = {
  status: "fallback"
  model: string
}

export class AiStreamEventError extends Error {
  code: string

  constructor(payload: AiStreamError) {
    super(payload.message)
    this.name = "AiStreamEventError"
    this.code = payload.code
  }
}

export function listAiConversations(token: string) {
  return apiRequest<AiConversationSummary[]>("/api/v1/ai/conversations", {}, token)
}

export function createAiConversation(token: string) {
  return apiRequest<AiConversationSummary>("/api/v1/ai/conversations", { method: "POST" }, token)
}

export function getAiConversation(id: number, token: string) {
  return apiRequest<AiConversationDetail>(`/api/v1/ai/conversations/${id}`, {}, token)
}

export function deleteAiConversation(id: number, token: string) {
  return apiRequest<void>(`/api/v1/ai/conversations/${id}`, { method: "DELETE" }, token)
}

export function getAiQuota(token: string) {
  return apiRequest<AiQuota>("/api/v1/ai/quota", {}, token)
}

export async function streamAiMessage(options: {
  conversationId: number
  content: string
  token: string
  signal: AbortSignal
  onMessage: (delta: string) => void
  onModelStatus: (status: AiModelStatus) => void
  onQuota: (quota: AiQuota) => void
  onDone: (done: AiStreamDone) => void
  onError: (error: AiStreamError) => void
}) {
  await fetchEventSource(`${API_BASE_URL}/api/v1/ai/conversations/${options.conversationId}/messages/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${options.token}`,
    },
    body: JSON.stringify({ content: options.content }),
    signal: options.signal,
    openWhenHidden: true,
    async onopen(response) {
      if (!response.ok) {
        const payload = await response.json().catch(() => ({
          code: "REQUEST_FAILED",
          message: `请求失败（${response.status}）`,
          details: [],
        })) as ApiErrorPayload
        throw new ApiClientError(response.status, payload)
      }
      if (!response.headers.get("content-type")?.includes(EventStreamContentType)) {
        throw new ApiClientError(502, {
          code: "AI_STREAM_INVALID",
          message: "AI 服务返回了无法识别的响应",
        })
      }
    },
    onmessage(message: EventSourceMessage) {
      if (message.event === "message") options.onMessage(JSON.parse(message.data).delta)
      if (message.event === "model_status") options.onModelStatus(JSON.parse(message.data))
      if (message.event === "quota") options.onQuota(JSON.parse(message.data))
      if (message.event === "done") options.onDone(JSON.parse(message.data))
      if (message.event === "error") {
        const payload = JSON.parse(message.data) as AiStreamError
        options.onError(payload)
        throw new AiStreamEventError(payload)
      }
    },
    onerror(error) {
      throw error
    },
  })
}
