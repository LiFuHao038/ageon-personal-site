import { apiRequest } from "@/lib/api-client"
import type { AccountStatus, CurrentUser } from "@/lib/auth-api"

// 社区前端入口已下线，问题/回复类型随管理端保留在此（后端 community 包仍对外提供管理接口）。
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

export type AdminOverview = { pendingUsers: number; pendingQuestions: number; publishedQuestions: number; totalReplies: number }
export type ModerationStatus = "PENDING" | "PUBLISHED" | "REJECTED"
export type AdminReply = { id: number; questionId: number; questionTitle: string; author: string; authorRole: "USER" | "ADMIN"; content: string; createdAt: string }

export function getAdminOverview() { return apiRequest<AdminOverview>("/api/v1/admin/overview") }
export function listAdminUsers(status?: AccountStatus) { return apiRequest<CurrentUser[]>(`/api/v1/admin/users${status ? `?status=${status}` : ""}`) }
export function updateAdminUserStatus(id: number, status: AccountStatus) { return apiRequest<CurrentUser>(`/api/v1/admin/users/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }) }
export function listAdminQuestions(moderationStatus?: ModerationStatus) { return apiRequest<CommunityQuestionResponse[]>(`/api/v1/admin/questions${moderationStatus ? `?moderationStatus=${moderationStatus}` : ""}`) }
export function moderateAdminQuestion(id: number, moderationStatus: ModerationStatus) { return apiRequest<CommunityQuestionResponse>(`/api/v1/admin/questions/${id}/moderation`, { method: "PATCH", body: JSON.stringify({ moderationStatus }) }) }
export function deleteAdminQuestion(id: number) { return apiRequest<void>(`/api/v1/admin/questions/${id}`, { method: "DELETE" }) }
export function createAdminReply(questionId: number, content: string) { return apiRequest<CommunityQuestionResponse>(`/api/v1/admin/questions/${questionId}/replies`, { method: "POST", body: JSON.stringify({ content }) }) }
export function listAdminReplies() { return apiRequest<AdminReply[]>("/api/v1/admin/replies") }
export function deleteAdminReply(id: number) { return apiRequest<void>(`/api/v1/admin/replies/${id}`, { method: "DELETE" }) }
