import type { Metadata } from "next"
import { ApplyBoard } from "@/components/apply-board"
import { PageHeading } from "@/components/page-heading"

export const metadata: Metadata = {
  title: "投递追踪",
  description: "秋招投递记录、状态时间线与截止提醒，数据按账号私有隔离。",
}

export default function ApplyPage() {
  return (
    <main className="page-main">
      <div className="site-shell">
        <PageHeading
          index="02"
          eyebrow="APPLY TRACKER"
          title={<>记下每一次投递。<span className="text-white/30">盯住每一步进展。</span></>}
          description="登录后管理你的秋招投递：看板推进状态、记录时间线、跟踪网申截止，统计口径全部来自状态事件。"
          aside={<div className="mono border border-white/15 px-4 py-3 text-[10px] leading-5 text-white/45">PRIVATE BY USER<br /><span className="text-[#9ef01a]">SPRING API</span></div>}
        />
        <ApplyBoard />
      </div>
    </main>
  )
}
