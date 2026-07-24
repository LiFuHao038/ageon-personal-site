import { apiRequest } from "@/lib/api-client"
import type { AccountStatus, CurrentUser } from "@/lib/auth-api"
import type { CommunityQuestionResponse } from "@/lib/community-api"

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
