import { apiRequest } from "@/lib/api-client"

export type CommunityReplyItem = {
  id: number
  author: string
  authorRole: "USER" | "ADMIN"
  content: string
  createdAt: string
}

export type CommunityQuestionResponse = {
  id: number
  title: string
  detail: string
  tag: string
  author: string
  moderationStatus: "PENDING" | "PUBLISHED" | "REJECTED"
  replies: number
  status: "待回复" | "讨论中" | "已回复"
  time: string
  likes: number
  createdAt: string
  updatedAt: string
  replyItems: CommunityReplyItem[]
}

export type CreateQuestionPayload = {
  title: string
  detail: string
  tag: string
}

export type CreateReplyPayload = {
  content: string
}

export function listCommunityQuestions(params: { keyword?: string; tag?: string; status?: "WAITING" | "DISCUSSING" | "ANSWERED" } = {}) {
  const search = new URLSearchParams()
  if (params.keyword) search.set("keyword", params.keyword)
  if (params.tag && params.tag !== "全部") search.set("tag", params.tag)
  if (params.status) search.set("status", params.status)
  const query = search.toString()
  return apiRequest<CommunityQuestionResponse[]>(`/api/v1/community/questions${query ? `?${query}` : ""}`, {}, null)
}

export function getCommunityQuestion(questionId: number) {
  return apiRequest<CommunityQuestionResponse>(`/api/v1/community/questions/${questionId}`, {}, null)
}

export function createCommunityQuestion(payload: CreateQuestionPayload) {
  return apiRequest<CommunityQuestionResponse>("/api/v1/community/questions", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function createCommunityReply(questionId: number, payload: CreateReplyPayload) {
  return apiRequest<CommunityQuestionResponse>(`/api/v1/community/questions/${questionId}/replies`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function likeCommunityQuestion(questionId: number) {
  return apiRequest<CommunityQuestionResponse>(`/api/v1/community/questions/${questionId}/likes`, {
    method: "POST",
  })
}
