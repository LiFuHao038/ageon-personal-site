import type { Metadata } from "next"
import { AiChat } from "@/components/ai-chat"
import { PageHeading } from "@/components/page-heading"

export const metadata: Metadata = {
  title: "AI 问答",
  description: "基于 Kimi 的个人技术问答助手。",
}

export default function AiPage() {
  return (
    <main className="page-main">
      <div className="site-shell">
        <PageHeading
          index="03"
          eyebrow="AI ASSISTANT"
          title={<>问技术。<span className="text-white/30">保留上下文。</span></>}
          description="通过 Spring Boot 安全调用 Kimi，支持流式回复、会话历史和每日额度。"
          aside={<div className="mono flex items-center gap-3 border border-white/15 px-4 py-3 text-[10px] text-white/45"><span className="h-2 w-2 animate-pulse bg-[#9ef01a]" /> KIMI STREAM READY</div>}
        />
        <AiChat />
      </div>
    </main>
  )
}
