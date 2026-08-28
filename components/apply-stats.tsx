"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { LogIn, RefreshCw } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { getApplicationStats, type GroupStat, type StatsOverview } from "@/lib/application-api"
import { formatDate } from "@/components/apply-card"

function formatPercent(value: number) {
  const percent = value <= 1 ? value * 100 : value
  return `${percent.toFixed(percent % 1 === 0 ? 0 : 1)}%`
}

function GroupTable({ title, rows }: { title: string; rows: GroupStat[] }) {
  return (
    <div className="panel p-5">
      <h3 className="text-lg">{title}</h3>
      {rows.length === 0 ? (
        <p className="mt-4 text-sm text-white/35">暂无数据。</p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[420px] text-left text-sm">
            <thead>
              <tr className="mono border-b border-white/15 text-[10px] text-white/35">
                <th className="py-2 pr-4 font-normal">分组</th>
                <th className="py-2 pr-4 font-normal">总数</th>
                <th className="py-2 pr-4 font-normal">offer</th>
                <th className="py-2 pr-4 font-normal">未通过</th>
                <th className="py-2 font-normal">回应率</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.key} className="border-b border-white/10 last:border-b-0">
                  <td className="py-2 pr-4">{row.key}</td>
                  <td className="mono py-2 pr-4 text-white/60">{row.total}</td>
                  <td className="mono py-2 pr-4 text-[#9ef01a]">{row.offers}</td>
                  <td className="mono py-2 pr-4 text-[#ff6b5f]">{row.rejected}</td>
                  <td className="mono py-2 text-white/60">{formatPercent(row.responseRate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export function ApplyStats() {
  const { user, loading: authLoading } = useAuth()
  const [stats, setStats] = useState<StatsOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const loadStats = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      setStats(await getApplicationStats())
    } catch {
      setError("后端服务暂时无法访问，请确认 Spring Boot 正在运行。")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!user) return
    void loadStats()
  }, [user, loadStats])

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
            <h2 className="mt-4 text-2xl md:text-3xl">统计数据按账号私有隔离</h2>
            <p className="mx-auto mt-4 max-w-md text-sm leading-7 text-white/50">登录后即可查看你的投递漏斗、阶段耗时与临期截止。</p>
            <Link href="/auth" className="interactive mt-7 inline-flex h-11 items-center gap-2 border border-[#9ef01a] bg-[#9ef01a] px-5 text-sm font-semibold text-black"><LogIn size={16} /> 去登录 / 注册</Link>
          </div>
        </div>
      </section>
    )
  }

  if (loading) {
    return (
      <section className="py-10 md:py-14">
        <div className="grid min-h-72 place-items-center border border-white/15"><div className="mono flex items-center gap-3 text-[11px] text-white/45"><RefreshCw size={16} className="animate-spin" /> LOADING API DATA</div></div>
      </section>
    )
  }

  if (error || !stats) {
    return (
      <section className="py-10 md:py-14">
        <div className="flex flex-col gap-3 border border-[#ff6b5f]/45 bg-[#ff6b5f]/8 p-4 text-sm text-[#ffb3ad] sm:flex-row sm:items-center sm:justify-between">
          <span>{error || "统计数据加载失败。"}</span>
          <button type="button" onClick={() => void loadStats()} className="interactive text-xs">重试</button>
        </div>
      </section>
    )
  }

  const hasData = stats.total > 0
  const funnelMax = Math.max(...stats.funnel.map((stage) => stage.reached), 1)
  const weeklyMax = Math.max(...stats.weekly.map((point) => point.applied), 1)

  const summaryCards = [
    { label: "总投递", value: stats.total },
    { label: "进行中", value: stats.active },
    { label: "offer", value: stats.offers, color: "#9ef01a" },
    { label: "未通过", value: stats.rejected, color: "#ff6b5f" },
  ]

  return (
    <section className="grid gap-10 py-10 md:py-14">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {summaryCards.map((card) => (
          <div key={card.label} className="panel p-5">
            <p className="mono text-[10px] uppercase tracking-[.15em] text-white/35">{card.label}</p>
            <p className="mono mt-3 text-4xl" style={card.color ? { color: card.color } : undefined}>{card.value}</p>
          </div>
        ))}
      </div>

      {!hasData ? (
        <div className="panel grid min-h-48 place-items-center p-8 text-center">
          <div>
            <p className="text-white/60">还没有投递数据，统计图表将在产生投递记录后展示。</p>
            <Link href="/apply" className="mt-3 inline-block text-sm text-[#9ef01a]">去记录第一条投递</Link>
          </div>
        </div>
      ) : (
        <>
          <div className="panel p-5">
            <h3 className="text-lg">状态漏斗</h3>
            <div className="mt-5 grid gap-2">
              {stats.funnel.map((stage) => (
                <div key={stage.status} className="grid grid-cols-[96px_1fr_48px] items-center gap-3">
                  <span className="mono text-right text-[10px] text-white/55">{stage.label}</span>
                  <div className="h-7 bg-white/5">
                    <div className="flex h-full items-center bg-[#9ef01a]/25 pl-2" style={{ width: `${Math.max((stage.reached / funnelMax) * 100, stage.reached > 0 ? 6 : 0)}%`, borderLeft: "2px solid #9ef01a" }} />
                  </div>
                  <span className="mono text-[11px] text-white/60">{stage.reached}</span>
                </div>
              ))}
            </div>
            <p className="mono mt-4 text-[10px] leading-5 text-white/30">口径说明：到达数 = 曾进入该状态的去重记录数，允许跳级，因此不代表逐级转化率。</p>
          </div>

          <div className="panel p-5">
            <h3 className="text-lg">阶段耗时</h3>
            {stats.stageDurations.length === 0 ? (
              <p className="mt-4 text-sm text-white/35">暂无足够的相邻状态事件。</p>
            ) : (
              <div className="mt-4 grid gap-3">
                {stats.stageDurations.map((duration) => (
                  <div key={`${duration.from}-${duration.to}`} className={`flex flex-wrap items-baseline justify-between gap-2 border-b border-white/10 pb-3 last:border-b-0 ${duration.samples < 3 ? "opacity-45" : ""}`}>
                    <span className="text-sm text-white/70">{duration.fromLabel} → {duration.toLabel}</span>
                    <span className="mono text-[11px] text-white/50">
                      平均 {duration.averageDays.toFixed(1)} 天 · {duration.samples} 个样本{duration.samples < 3 ? "（样本过少）" : ""}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <GroupTable title="按公司类型" rows={stats.byCompanyType} />
            <GroupTable title="按城市" rows={stats.byCity} />
          </div>

          <div className="panel p-5">
            <h3 className="text-lg">每周投递趋势</h3>
            {stats.weekly.length === 0 ? (
              <p className="mt-4 text-sm text-white/35">暂无周趋势数据。</p>
            ) : (
              <div className="mt-5 overflow-x-auto">
                <svg viewBox={`0 0 ${Math.max(stats.weekly.length * 72, 240)} 190`} className="h-48 min-w-full" role="img" aria-label="每周投递数柱状图">
                  {stats.weekly.map((point, index) => {
                    const barWidth = 36
                    const x = index * 72 + 18
                    const height = Math.max((point.applied / weeklyMax) * 130, point.applied > 0 ? 4 : 0)
                    const y = 150 - height
                    const label = formatDate(point.weekStart).slice(5)
                    return (
                      <g key={point.weekStart}>
                        <rect x={x} y={y} width={barWidth} height={height} fill="#9ef01a" opacity={0.75} />
                        <text x={x + barWidth / 2} y={y - 6} textAnchor="middle" fill="#f3f5f2" fontSize={11} fontFamily="Cascadia Code, Consolas, monospace">{point.applied}</text>
                        <text x={x + barWidth / 2} y={170} textAnchor="middle" fill="rgba(255,255,255,.35)" fontSize={9} fontFamily="Cascadia Code, Consolas, monospace">{label}</text>
                      </g>
                    )
                  })}
                  <line x1={0} y1={150} x2={Math.max(stats.weekly.length * 72, 240)} y2={150} stroke="rgba(255,255,255,.15)" />
                </svg>
              </div>
            )}
          </div>

          <div className="panel p-5">
            <h3 className="text-lg">临期与逾期截止</h3>
            {stats.upcomingDeadlines.length === 0 ? (
              <p className="mt-4 text-sm text-white/35">暂无临期或逾期的网申截止。</p>
            ) : (
              <div className="mt-4 grid gap-2">
                {stats.upcomingDeadlines.map((deadline) => (
                  <div key={deadline.id} className="flex flex-wrap items-baseline justify-between gap-2 border-b border-white/10 pb-3 last:border-b-0">
                    <span className="text-sm text-white/70">{deadline.company} · {deadline.position}</span>
                    <span className={`mono text-[11px] ${deadline.overdue ? "text-[#ff6b5f]" : deadline.daysLeft <= 7 ? "text-[#ffcf5c]" : "text-white/50"}`}>
                      {formatDate(deadline.deadlineAt)} · {deadline.overdue ? `已逾期 ${-deadline.daysLeft} 天` : `剩 ${deadline.daysLeft} 天`}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </section>
  )
}
