export type ProfileFolder = {
  id: string
  title: string
  eyebrow: string
  summary: string
  items: string[]
  accent: string
}

export const profileFolders: ProfileFolder[] = [
  { id: "stack", title: "技术栈", eyebrow: "BUILD", summary: "偏向 Java 后端，也在持续补齐现代前端与 AI 应用能力。", items: ["Java / Spring Boot", "MySQL / Redis", "React / Next.js", "Python / AI 应用"], accent: "#9ef01a" },
  { id: "learning", title: "正在学习", eyebrow: "LEARN", summary: "围绕工程基础和大模型应用构建更完整的知识结构。", items: ["计算机网络", "操作系统", "RAG 与 Agent", "系统设计"], accent: "#55d6be" },
  { id: "music", title: "音乐", eyebrow: "LISTEN", summary: "写代码和整理知识时，音乐是稳定的背景节奏。", items: ["流行", "轻音乐", "影视原声", "专注歌单"], accent: "#ffd166" },
  { id: "games", title: "游戏", eyebrow: "PLAY", summary: "喜欢有策略、协作或完整世界观的游戏体验。", items: ["策略与经营", "多人协作", "开放世界", "独立游戏"], accent: "#ff7a66" },
  { id: "goals", title: "近期目标", eyebrow: "NEXT", summary: "把个人站点做成持续更新的技术作品，而不是一次性页面。", items: ["打磨投递追踪", "接入 Kimi 问答", "沉淀面试复盘", "完成公网部署"], accent: "#b8a1ff" },
]
