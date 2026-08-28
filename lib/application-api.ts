import { apiRequest } from "@/lib/api-client"

export type ApplicationStatus =
  | "PREPARING"
  | "APPLIED"
  | "WRITTEN_TEST"
  | "INTERVIEW_1"
  | "INTERVIEW_2"
  | "INTERVIEW_FINAL"
  | "OFFER"
  | "REJECTED"
  | "WITHDRAWN"

export type ApplicationEventItem = {
  id: number
  fromStatus: ApplicationStatus | null
  fromStatusLabel: string | null
  toStatus: ApplicationStatus
  toStatusLabel: string
  occurredAt: string
  note: string | null
}

export type ApplicationItem = {
  id: number
  company: string
  position: string
  city: string | null
  companyType: string | null
  channel: string | null
  status: ApplicationStatus
  statusLabel: string
  sourceUrl: string | null
  sourceTitle: string | null
  sourceLogoUrl: string | null
  sourceError: string | null
  sourceFetchedAt: string | null
  deadlineAt: string | null
  appliedAt: string
  note: string | null
  daysSinceApplied: number | null
  daysToDeadline: number | null
  createdAt: string
  updatedAt: string
  events: ApplicationEventItem[]
}

export type StatusOption = {
  status: string
  label: string
  terminal: boolean
  allowed: string[]
}

export type FunnelStage = {
  status: string
  label: string
  reached: number
}

export type StageDuration = {
  from: string
  fromLabel: string
  to: string
  toLabel: string
  averageDays: number
  samples: number
}

export type GroupStat = {
  key: string
  total: number
  offers: number
  rejected: number
  responseRate: number
}

export type WeeklyPoint = {
  weekStart: string
  applied: number
}

export type DeadlineItem = {
  id: number
  company: string
  position: string
  deadlineAt: string
  daysLeft: number
  overdue: boolean
}

export type StatsOverview = {
  total: number
  active: number
  offers: number
  rejected: number
  funnel: FunnelStage[]
  stageDurations: StageDuration[]
  byCompanyType: GroupStat[]
  byCity: GroupStat[]
  weekly: WeeklyPoint[]
  upcomingDeadlines: DeadlineItem[]
}

export type ApplicationPayload = {
  company: string
  position: string
  city?: string | null
  companyType?: string | null
  channel?: string | null
  sourceUrl?: string | null
  deadlineAt?: string | null
  appliedAt?: string | null
  note?: string | null
}

export type SourcePreview = {
  url: string
  title: string | null
  logoUrl: string | null
  error: string | null
}

// 后端 meta/statuses 接口不可用时的兜底：与 cn.ageon.apply.ApplicationStatus.canTransitionTo 保持一致
export const FALLBACK_STATUS_OPTIONS: StatusOption[] = [
  { status: "PREPARING", label: "准备投递", terminal: false, allowed: ["APPLIED", "WRITTEN_TEST", "INTERVIEW_1", "INTERVIEW_2", "INTERVIEW_FINAL", "OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "APPLIED", label: "已投递", terminal: false, allowed: ["WRITTEN_TEST", "INTERVIEW_1", "INTERVIEW_2", "INTERVIEW_FINAL", "OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "WRITTEN_TEST", label: "笔试/测评", terminal: false, allowed: ["INTERVIEW_1", "INTERVIEW_2", "INTERVIEW_FINAL", "OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "INTERVIEW_1", label: "一面", terminal: false, allowed: ["INTERVIEW_2", "INTERVIEW_FINAL", "OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "INTERVIEW_2", label: "二面", terminal: false, allowed: ["INTERVIEW_FINAL", "OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "INTERVIEW_FINAL", label: "终面/HR面", terminal: false, allowed: ["OFFER", "REJECTED", "WITHDRAWN"] },
  { status: "OFFER", label: "已获得 offer", terminal: true, allowed: ["WITHDRAWN"] },
  { status: "REJECTED", label: "未通过", terminal: true, allowed: [] },
  { status: "WITHDRAWN", label: "已撤回", terminal: true, allowed: [] },
]

export function listApplications(params: { status?: string; keyword?: string; companyType?: string } = {}) {
  const search = new URLSearchParams()
  if (params.status) search.set("status", params.status)
  if (params.keyword) search.set("keyword", params.keyword)
  if (params.companyType) search.set("companyType", params.companyType)
  const query = search.toString()
  return apiRequest<ApplicationItem[]>(`/api/v1/applications${query ? `?${query}` : ""}`)
}

export function getApplication(id: number) {
  return apiRequest<ApplicationItem>(`/api/v1/applications/${id}`)
}

export function createApplication(payload: ApplicationPayload) {
  return apiRequest<ApplicationItem>("/api/v1/applications", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function updateApplication(id: number, payload: ApplicationPayload) {
  return apiRequest<ApplicationItem>(`/api/v1/applications/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  })
}

export async function deleteApplication(id: number): Promise<void> {
  await apiRequest<void>(`/api/v1/applications/${id}`, { method: "DELETE" })
}

export function changeApplicationStatus(id: number, payload: { status: string; occurredAt?: string; note?: string }) {
  return apiRequest<ApplicationItem>(`/api/v1/applications/${id}/status`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function previewApplicationSource(url: string) {
  return apiRequest<SourcePreview>("/api/v1/applications/source-preview", {
    method: "POST",
    body: JSON.stringify({ url }),
  })
}

export function getApplicationStatusMeta() {
  return apiRequest<StatusOption[]>("/api/v1/applications/meta/statuses")
}

export function getApplicationStats() {
  return apiRequest<StatsOverview>("/api/v1/applications/stats/overview")
}
