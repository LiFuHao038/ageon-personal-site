import type { Metadata } from "next"
import { CommunityBoard } from "@/components/community-board"
import { PageHeading } from "@/components/page-heading"

export const metadata: Metadata = {
  title: "提问社区",
  description: "围绕 Java、AI 应用和计算机基础展开公开讨论。",
}

export default function CommunityPage() {
  return (
    <main className="page-main">
      <div className="site-shell">
        <PageHeading
          index="02"
          eyebrow="COMMUNITY"
          title={<>把问题留下。<span className="text-white/30">一起拆解。</span></>}
          description="公开阅读已发布问题；登录用户可提问、回复与点赞，新问题会先进入管理员审核。"
          aside={<div className="mono border border-white/15 px-4 py-3 text-[10px] leading-5 text-white/45">MODERATED<br /><span className="text-[#9ef01a]">SPRING API</span></div>}
        />
        <CommunityBoard />
      </div>
    </main>
  )
}
