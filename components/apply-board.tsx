"use client"

import Link from "next/link"
import { useCallback, useEffect, useMemo, useState } from "react"
import { BarChart3, KanbanSquare, LogIn, Plus, RefreshCw, Search, Table2 } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApplyCard, formatDate, STATUS_COLORS } from "@/components/apply-card"
import { ApplyForm } from "@/components/apply-form"
import { FALLBACK_STATUS_OPTIONS, getApplicationStatusMeta, listApplications, type ApplicationItem, type StatusOption } from "@/lib/application-api"

export function ApplyBoard() {
  const { user, loading: authLoading } = useAuth()
  const [applications, setApplications] = useState<ApplicationItem[]>([])
  const [statusMeta, setStatusMeta] = useState<StatusOption[]>(FALLBACK_STATUS_OPTIONS)
  const [view, setView] = useState<"board" | "table">("board")
  const [keyword, setKeyword] = useState("")
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<ApplicationItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const loadApplications = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      setApplications(await listApplications({ keyword: keyword.trim() || undefined }))
    } catch {
      setError("后端服务暂时无法访问，请确认 Spring Boot 正在运行。")
    } finally {
      setLoading(false)
    }
  }, [keyword])

  useEffect(() => {
    if (!user) return
    void loadApplications()
    getApplicationStatusMeta()
      .then((meta) => { if (Array.isArray(meta) && meta.length > 0) setStatusMeta(meta) })
      .catch(() => { /* 接口不可用时使用内置兜底状态列表 */ })
  }, [user, loadApplications])

  const grouped = useMemo(() => {
    const map = new Map<string, ApplicationItem[]>()
    for (const option of statusMeta) map.set(option.status, [])
    for (const item of applications) {
      const list = map.get(item.status) ?? []
      list.push(item)
      map.set(item.status, list)
    }
    return map
  }, [applications, statusMeta])

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function openEdit(item: ApplicationItem) {
    setEditing(item)
    setFormOpen(true)
  }

  if (authLoading) {
    return (
      <section className="py-10 md:py-14">
        <div className="grid min-h-72 place-items-center border border-white/15"><div className="mono flex items-center gap-3 text-[11px] text-white/45"><RefreshCw size={16} className="animate-spin" /> CHECKING SESSION</div></div>
      </section>
    )
  }

  if (!user) {
    return (
      <section className="py-10 md:py-14">
        <div className="panel grid min-h-72 place-items-center p-8 text-center">
          <div>
            <p className="eyebrow">LOGIN REQUIRED</p>
            <h2 className="mt-4 text-2xl md:text-3xl">投递数据按账号私有隔离</h2>
            <p className="mx-auto mt-4 max-w-md text-sm leading-7 text-white/50">登录后即可记录投递、推进状态并查看统计看板。账号需通过管理员审核。</p>
            <Link href="/auth" className="interactive mt-7 inline-flex h-11 items-center gap-2 border border-[#9ef01a] bg-[#9ef01a] px-5 text-sm font-semibold text-black"><LogIn size={16} /> 去登录 / 注册</Link>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="py-10 md:py-14">
      {error && (
        <div className="mb-5 flex flex-col gap-3 border border-[#ff6b5f]/45 bg-[#ff6b5f]/8 p-4 text-sm text-[#ffb3ad] sm:flex-row sm:items-center sm:justify-between">
          <span>{error}</span>
          <button type="button" onClick={() => void loadApplications()} className="interactive text-xs">重试</button>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
        <label className="flex h-12 items-center gap-3 border border-white/15 bg-[#0c0e0c]/90 px-4 focus-within:border-[#9ef01a]">
          <Search size={17} className="text-white/40" />
          <input value={keyword} onChange={(event) => setKeyword(event.target.value)} className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-white/28" placeholder="搜索公司、岗位或备注" />
        </label>
        <div className="flex gap-2">
          <div className="flex border border-white/15">
            <button type="button" onClick={() => setView("board")} className={`interactive flex h-12 items-center gap-2 px-4 text-sm ${view === "board" ? "bg-white text-black" : "text-white/55"}`}><KanbanSquare size={16} /> 看板</button>
            <button type="button" onClick={() => setView("table")} className={`interactive flex h-12 items-center gap-2 px-4 text-sm ${view === "table" ? "bg-white text-black" : "text-white/55"}`}><Table2 size={16} /> 表格</button>
          </div>
          <Link href="/apply/stats" className="interactive flex h-12 items-center gap-2 border border-white/15 px-4 text-sm text-white/70 hover:text-[#9ef01a]"><BarChart3 size={16} /> 统计</Link>
          <button type="button" onClick={openCreate} className="interactive flex h-12 items-center justify-center gap-2 border border-[#9ef01a] bg-[#9ef01a] px-5 text-sm font-semibold text-black"><Plus size={17} /> 新建投递</button>
        </div>
      </div>

      <div className="mt-8">
        {loading ? (
          <div className="grid min-h-72 place-items-center border border-white/15"><div className="mono flex items-center gap-3 text-[11px] text-white/45"><RefreshCw size={16} className="animate-spin" /> LOADING API DATA</div></div>
        ) : applications.length === 0 ? (
          <div className="grid min-h-72 place-items-center border border-white/15 text-center">
            <div>
              <Search className="mx-auto text-white/25" />
              <p className="mt-4 text-white/60">{keyword ? "没有匹配的投递记录" : "还没有投递记录"}</p>
              {keyword ? (
                <button type="button" onClick={() => setKeyword("")} className="mt-3 text-sm text-[#9ef01a]">清空搜索</button>
              ) : (
                <button type="button" onClick={openCreate} className="mt-3 text-sm text-[#9ef01a]">创建第一条投递</button>
              )}
            </div>
          </div>
        ) : view === "board" ? (
          <div className="flex gap-4 overflow-x-auto pb-4">
            {statusMeta.map((option) => {
              const items = grouped.get(option.status) ?? []
              return (
                <div key={option.status} className="w-64 shrink-0">
                  <div className="flex items-center justify-between border border-white/15 bg-[#080a08]/75 px-3 py-2">
                    <span className="mono flex items-center gap-2 text-[10px] text-white/60">
                      <span className="h-2 w-2 rounded-full" style={{ background: STATUS_COLORS[option.status] ?? "#9ba39d" }} />
                      {option.label}
                    </span>
                    <span className="mono text-[10px] text-white/35">{items.length}</span>
                  </div>
                  <div className="mt-3 grid gap-3">
                    {items.length > 0 ? items.map((item) => (
                      <ApplyCard key={item.id} item={item} statusMeta={statusMeta} onEdit={openEdit} onChanged={() => void loadApplications()} />
                    )) : (
                      <div className="border border-dashed border-white/10 p-4 text-center text-xs text-white/25">暂无记录</div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="overflow-x-auto border border-white/15">
            <table className="w-full min-w-[880px] text-left text-sm">
              <thead>
                <tr className="mono border-b border-white/15 text-[10px] text-white/35">
                  <th className="px-4 py-3 font-normal">公司</th>
                  <th className="px-4 py-3 font-normal">岗位</th>
                  <th className="px-4 py-3 font-normal">城市</th>
                  <th className="px-4 py-3 font-normal">类型</th>
                  <th className="px-4 py-3 font-normal">状态</th>
                  <th className="px-4 py-3 font-normal">投递日</th>
                  <th className="px-4 py-3 font-normal">截止</th>
                  <th className="px-4 py-3 font-normal">操作</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((item) => (
                  <tr key={item.id} className="border-b border-white/10 last:border-b-0">
                    <td className="px-4 py-3 font-medium">{item.company}</td>
                    <td className="px-4 py-3 text-white/60">{item.position}</td>
                    <td className="px-4 py-3 text-white/45">{item.city ?? "—"}</td>
                    <td className="px-4 py-3 text-white/45">{item.companyType ?? "—"}</td>
                    <td className="px-4 py-3">
                      <span className="mono flex items-center gap-2 text-[10px] text-white/65">
                        <span className="h-2 w-2 rounded-full" style={{ background: STATUS_COLORS[item.status] ?? "#9ba39d" }} />
                        {item.statusLabel}
                      </span>
                    </td>
                    <td className="mono px-4 py-3 text-[11px] text-white/45">{formatDate(item.appliedAt)}</td>
                    <td className="mono px-4 py-3 text-[11px]">
                      {item.daysToDeadline === null ? (
                        <span className="text-white/30">—</span>
                      ) : item.daysToDeadline < 0 ? (
                        <span className="text-[#ff6b5f]">已逾期 {-item.daysToDeadline} 天</span>
                      ) : (
                        <span className={item.daysToDeadline <= 7 ? "text-[#ffcf5c]" : "text-white/45"}>{formatDate(item.deadlineAt)} · 剩 {item.daysToDeadline} 天</span>
                      )}
                    </td>
                    <td className="px-4 py-3"><button type="button" onClick={() => openEdit(item)} className="interactive mono border border-white/15 px-2 py-1 text-[10px] text-white/60">编辑</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ApplyForm open={formOpen} initial={editing} onClose={() => setFormOpen(false)} onSaved={() => void loadApplications()} />
    </section>
  )
}
