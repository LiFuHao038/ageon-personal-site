import type { Metadata } from "next"
import Link from "next/link"
import { ApplyStats } from "@/components/apply-stats"
import { PageHeading } from "@/components/page-heading"

export const metadata: Metadata = {
  title: "投递统计",
  description: "秋招投递漏斗、阶段耗时、分组回应率与临期截止一览。",
}

export default function ApplyStatsPage() {
  return (
    <main className="page-main">
      <div className="site-shell">
        <PageHeading
          index="03"
          eyebrow="STATS"
          title={<>用数据复盘。<span className="text-white/30">校准下一轮投递。</span></>}
          description="漏斗到达数、阶段耗时、分组回应率与周趋势，全部从状态事件表推导，非人工填写。"
          aside={<Link href="/apply" className="mono interactive inline-flex items-center gap-2 border border-white/15 px-4 py-3 text-[10px] text-white/45 hover:text-[#9ef01a]">← 返回投递追踪</Link>}
        />
        <ApplyStats />
      </div>
    </main>
  )
}
