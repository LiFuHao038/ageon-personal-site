export type SiteNavItem = {
  label: string
  href: string
  short: string
}

export type Project = {
  title: string
  description: string
  tags: string[]
  status: string
  accent: string
  repoUrl?: string
  demoUrl?: string
}

export const siteNav: SiteNavItem[] = [
  { label: "首页", href: "/", short: "ABOUT" },
  { label: "投递追踪", href: "/apply", short: "TRACK" },
  { label: "AI 问答", href: "/ai", short: "ASK AI" },
]

export const projects: Project[] = [
  {
    title: "AI 编程小助手",
    description: "围绕代码生成、解释与调试构建的流式 AI 助手。",
    tags: ["Spring Boot", "LangChain4j", "SSE", "Redis"],
    status: "持续开发",
    accent: "#9ef01a",
    repoUrl: "",
    demoUrl: "",
  },
  {
    title: "AI 零代码应用生成平台",
    description: "通过自然语言生成应用结构，并管理生成与预览流程。",
    tags: ["Java", "MySQL", "RAG", "Prompt"],
    status: "项目作品",
    accent: "#55d6be",
    repoUrl: "",
    demoUrl: "",
  },
]

export const skillGroups = [
  { label: "BACKEND", value: "Java / Spring Boot / MySQL / Redis" },
  { label: "AI APP", value: "LangChain4j / RAG / SSE / Prompt" },
  { label: "FOUNDATION", value: "计算机网络 / 数据结构 / 操作系统" },
]
