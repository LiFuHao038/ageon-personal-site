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
}

export type CommunityQuestion = {
  id: number
  title: string
  detail: string
  tag: string
  author: string
  replies: number
  status: "已回复" | "讨论中" | "待回复"
  time: string
}

export type InterviewQuestion = {
  id: number
  category: string
  level: "基础" | "进阶" | "高频"
  question: string
  answer: string
  keywords: string[]
}

export const siteNav: SiteNavItem[] = [
  { label: "首页", href: "/", short: "ABOUT" },
  { label: "提问社区", href: "/community", short: "DISCUSS" },
  { label: "AI 问答", href: "/ai", short: "ASK AI" },
  { label: "面试题库", href: "/interview", short: "PRACTICE" },
]

export const projects: Project[] = [
  {
    title: "AI 编程小助手",
    description: "围绕代码生成、解释与调试构建的流式 AI 助手。",
    tags: ["Spring Boot", "LangChain4j", "SSE", "Redis"],
    status: "持续开发",
    accent: "#9ef01a",
  },
  {
    title: "AI 零代码应用生成平台",
    description: "通过自然语言生成应用结构，并管理生成与预览流程。",
    tags: ["Java", "MySQL", "RAG", "Prompt"],
    status: "项目作品",
    accent: "#55d6be",
  },
]

export const communityQuestions: CommunityQuestion[] = [
  {
    id: 101,
    title: "Spring Boot 项目中 SSE 连接如何避免超时？",
    detail: "正在实现 AI 流式输出，想了解连接管理和异常重试的常见方案。",
    tag: "Java 后端",
    author: "CodeLearner",
    replies: 6,
    status: "已回复",
    time: "18 分钟前",
  },
  {
    id: 102,
    title: "RAG 检索结果相关性不高应该先调哪一层？",
    detail: "文档切片、Embedding 和召回数量之间应该如何排查？",
    tag: "AI 应用",
    author: "NorthStar",
    replies: 3,
    status: "讨论中",
    time: "1 小时前",
  },
  {
    id: 103,
    title: "TCP 四次挥手中的 TIME_WAIT 有什么作用？",
    detail: "除了保证最后一个 ACK 到达，还有哪些工程层面的意义？",
    tag: "计算机网络",
    author: "Packet_01",
    replies: 0,
    status: "待回复",
    time: "今天",
  },
  {
    id: 104,
    title: "Redis 缓存穿透和缓存击穿如何区分？",
    detail: "希望结合实际业务场景理解两者的解决方案。",
    tag: "数据库",
    author: "BackendNewbie",
    replies: 8,
    status: "已回复",
    time: "昨天",
  },
]

export const interviewQuestions: InterviewQuestion[] = [
  {
    id: 1,
    category: "计算机网络",
    level: "高频",
    question: "TCP 为什么需要三次握手，而不是两次？",
    answer: "三次握手不仅确认双方的收发能力，还能避免历史失效连接请求导致服务端建立错误连接。第三次确认让服务端知道客户端已经收到自己的初始序列号与确认信息。",
    keywords: ["序列号", "双向通信", "历史连接"],
  },
  {
    id: 2,
    category: "计算机网络",
    level: "基础",
    question: "HTTP 与 HTTPS 的主要区别是什么？",
    answer: "HTTPS 在 HTTP 与 TCP 之间加入 TLS，通过证书校验身份，并对传输内容加密和校验完整性。它能降低窃听、篡改和中间人攻击风险。",
    keywords: ["TLS", "证书", "加密"],
  },
  {
    id: 3,
    category: "计算机网络",
    level: "进阶",
    question: "浏览器输入 URL 后发生了什么？",
    answer: "典型流程包括 URL 解析、缓存检查、DNS 解析、建立 TCP 或 QUIC 连接、TLS 握手、发送 HTTP 请求、服务端处理响应，以及浏览器解析资源并完成渲染。",
    keywords: ["DNS", "连接", "渲染"],
  },
  {
    id: 4,
    category: "计算机网络",
    level: "高频",
    question: "TCP 如何实现可靠传输？",
    answer: "TCP 通过序列号、确认应答、超时重传、快速重传、校验和、滑动窗口、流量控制与拥塞控制共同实现可靠的有序字节流。",
    keywords: ["ACK", "重传", "滑动窗口"],
  },
  {
    id: 5,
    category: "计算机网络",
    level: "基础",
    question: "GET 和 POST 的区别应该如何理解？",
    answer: "两者首先是 HTTP 方法语义：GET 用于获取资源且应当安全、幂等，POST 通常用于提交处理且不保证幂等。请求参数位置和长度限制更多是客户端与服务器实现差异，不是协议本质。",
    keywords: ["语义", "安全", "幂等"],
  },
  {
    id: 6,
    category: "计算机网络",
    level: "进阶",
    question: "什么是 TCP 粘包和拆包？如何处理？",
    answer: "TCP 是字节流协议，不保留应用消息边界。一次读取可能得到半条或多条消息。应用层可以使用固定长度、分隔符、消息头长度字段或成熟协议定义消息边界。",
    keywords: ["字节流", "消息边界", "长度字段"],
  },
]

export const skillGroups = [
  { label: "BACKEND", value: "Java / Spring Boot / MySQL / Redis" },
  { label: "AI APP", value: "LangChain4j / RAG / SSE / Prompt" },
  { label: "FOUNDATION", value: "计算机网络 / 数据结构 / 操作系统" },
]
