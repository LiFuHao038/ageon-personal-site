import type { Metadata } from "next"
import { InterviewLibrary } from "@/components/interview-library"
import { PageHeading } from "@/components/page-heading"

export const metadata: Metadata = {
  title: "面试题库",
  description: "计算机网络面试题与模拟面试练习。",
}

export default function InterviewPage() {
  return (
    <main className="page-main">
      <div className="site-shell">
        <PageHeading
          index="04"
          eyebrow="INTERVIEW LAB"
          title={<>先理解。<span className="text-white/30">再模拟表达。</span></>}
          description="第一版开放计算机网络题库。可以逐题复习，也可以进入五题模拟面试流程。"
          aside={<div className="mono border border-white/15 px-4 py-3 text-[10px] leading-5 text-white/45">ACTIVE MODULE<br /><span className="text-[#9ef01a]">COMPUTER NETWORK</span></div>}
        />
        <InterviewLibrary />
      </div>
    </main>
  )
}
