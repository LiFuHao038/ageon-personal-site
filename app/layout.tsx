import type React from "react"
import type { Metadata, Viewport } from "next"
import { Navbar } from "@/components/navbar"
import { SiteFooter } from "@/components/site-footer"
import { AuthProvider } from "@/components/auth-provider"
import { ParticleBackground } from "@/components/particle-background"
import "./globals.css"

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"
const siteDescription = "个人技术站：秋招投递追踪、投递统计看板与 AI 问答。"

const personJsonLd = {
  "@context": "https://schema.org",
  "@type": "Person",
  name: "李富浩",
  url: siteUrl,
  jobTitle: "Java 后端 / AI 应用工程师",
}

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "AGEON Personal Site",
    template: "%s - AGEON",
  },
  description: siteDescription,
  icons: {
    icon: "/icon.svg",
  },
  openGraph: {
    title: "AGEON 个人技术站",
    description: siteDescription,
    url: siteUrl,
    locale: "zh_CN",
    siteName: "AGEON",
  },
}

export const viewport: Viewport = {
  themeColor: "#070807",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="zh-CN" data-scroll-behavior="smooth">
      <body>
        <AuthProvider>
          <ParticleBackground />
          <div className="noise-overlay" />
          <div className="site-content"><Navbar />{children}<SiteFooter /></div>
        </AuthProvider>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(personJsonLd) }}
        />
      </body>
    </html>
  )
}
