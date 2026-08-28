"use client"

import { useState } from "react"
import { CalendarClock, ChevronDown, ChevronUp, Pencil, Trash2 } from "lucide-react"
import { ApiClientError } from "@/lib/api-client"
import { changeApplicationStatus, deleteApplication, FALLBACK_STATUS_OPTIONS, type ApplicationItem, type StatusOption } from "@/lib/application-api"

export const STATUS_COLORS: Record<string, string> = {
  PREPARING: "#9ba39d",
  APPLIED: "#9ef01a",
  WRITTEN_TEST: "#55d6be",
  INTERVIEW_1: "#55d6be",
  INTERVIEW_2: "#7cc7ff",
  INTERVIEW_FINAL: "#ffcf5c",
  OFFER: "#9ef01a",
  REJECTED: "#ff6b5f",
  WITHDRAWN: "#9ba39d",
}

function statusColor(status: string) {
  return STATUS_COLORS[status] ?? "#9ba39d"
}

export function formatDate(value: string | null) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit" })
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

type ApplyCardProps = {
  item: ApplicationItem
  statusMeta?: StatusOption[]
  onEdit: (item: ApplicationItem) => void
  onChanged: () => void
}

export function ApplyCard({ item, statusMeta, onEdit, onChanged }: ApplyCardProps) {
  const [expanded, setExpanded] = useState(false)
  const [advancing, setAdvancing] = useState(false)
  const [error, setError] = useState("")

  const meta = statusMeta && statusMeta.length > 0 ? statusMeta : FALLBACK_STATUS_OPTIONS
  const current = meta.find((option) => option.status === item.status)
  const nextOptions = (current?.allowed ?? [])
    .map((status) => meta.find((option) => option.status === status))
    .filter((option): option is StatusOption => Boolean(option))

  async function advance(status: string) {
    if (advancing) return
    const input = window.prompt("给这次状态变更加一条备注（可留空，点取消则不变更）", "")
    if (input === null) return
    setAdvancing(true)
    setError("")
    try {
      await changeApplicationStatus(item.id, { status, note: input.trim() || undefined })
      onChanged()
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "状态更新失败，请稍后重试。")
    } finally {
      setAdvancing(false)
    }
  }

  async function remove() {
    if (!window.confirm(`确定删除「${item.company} · ${item.position}」吗？该操作不可恢复。`)) return
    setError("")
    try {
      await deleteApplication(item.id)
      onChanged()
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "删除失败，请稍后重试。")
    }
  }

  const daysToDeadline = item.daysToDeadline

  return (
    <article className="panel p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-base font-semibold">{item.company}</h3>
          <p className="mt-1 truncate text-sm text-white/55">{item.position}</p>
        </div>
        <span className="mono mt-1 flex shrink-0 items-center gap-2 text-[10px] text-white/60">
          <span className="h-2 w-2 rounded-full" style={{ background: statusColor(item.status) }} />
          {item.statusLabel}
        </span>
      </div>

      <div className="mono mt-3 grid gap-1 text-[10px] text-white/40">
        {item.daysSinceApplied !== null && <span>投递于 {formatDate(item.appliedAt)} · 已 {item.daysSinceApplied} 天</span>}
        {daysToDeadline !== null && (
          <span className={daysToDeadline < 0 ? "text-[#ff6b5f]" : daysToDeadline <= 7 ? "text-[#ffcf5c]" : ""}>
            {daysToDeadline < 0 ? `已逾期 ${-daysToDeadline} 天` : `距截止还剩 ${daysToDeadline} 天`}（{formatDate(item.deadlineAt)}）
          </span>
        )}
      </div>

      {item.note && <p className="mt-3 truncate border-t border-white/10 pt-3 text-xs text-white/45" title={item.note}>{item.note}</p>}

      {error && <p className="mt-3 border border-[#ff6b5f]/40 bg-[#ff6b5f]/8 p-2 text-xs text-[#ffaaa3]">{error}</p>}

      <div className="mt-3 flex items-center gap-2 border-t border-white/10 pt-3">
        <button type="button" onClick={() => onEdit(item)} className="interactive mono flex h-8 items-center gap-1 border border-white/15 px-2 text-[10px] text-white/60"><Pencil size={12} /> 编辑</button>
        <button type="button" onClick={remove} className="interactive mono flex h-8 items-center gap-1 border border-white/15 px-2 text-[10px] text-white/60 hover:text-[#ff6b5f]"><Trash2 size={12} /> 删除</button>
        <button type="button" onClick={() => setExpanded((value) => !value)} className="interactive mono ml-auto flex h-8 items-center gap-1 border border-white/15 px-2 text-[10px] text-white/60" aria-expanded={expanded}>
          {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />} {expanded ? "收起" : "推进 / 时间线"}
        </button>
      </div>

      {expanded && (
        <div className="mt-3 border-t border-white/10 pt-3">
          {nextOptions.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {nextOptions.map((option) => (
                <button
                  key={option.status}
                  type="button"
                  disabled={advancing}
                  onClick={() => advance(option.status)}
                  className="interactive mono h-8 border px-2 text-[10px] disabled:opacity-40"
                  style={{ borderColor: `${statusColor(option.status)}66`, color: statusColor(option.status) }}
                >
                  → {option.label}
                </button>
              ))}
            </div>
          ) : (
            <p className="mono text-[10px] text-white/35">终态记录，不可继续流转。</p>
          )}

          <div className="mt-4">
            <p className="mono flex items-center gap-2 text-[10px] text-white/35"><CalendarClock size={12} /> 状态时间线</p>
            {item.events.length > 0 ? (
              <ol className="mt-2 grid gap-2">
                {item.events.map((event) => (
                  <li key={event.id} className="border-l border-white/15 pl-3 text-xs">
                    <span className="text-white/65">
                      {event.fromStatusLabel ? `${event.fromStatusLabel} → ` : "创建为 "}{event.toStatusLabel}
                    </span>
                    <span className="mono ml-2 text-[10px] text-white/30">{formatDateTime(event.occurredAt)}</span>
                    {event.note && <p className="mt-1 text-[11px] text-white/40">{event.note}</p>}
                  </li>
                ))}
              </ol>
            ) : (
              <p className="mt-2 text-xs text-white/35">暂无状态事件。</p>
            )}
          </div>
        </div>
      )}
    </article>
  )
}
