import type React from "react"
import type { Metadata, Viewport } from "next"
import { Navbar } from "@/components/navbar"
import { SiteFooter } from "@/components/site-footer"
import { AuthProvider } from "@/components/auth-provider"
import { ParticleBackground } from "@/components/particle-background"
import "./globals.css"

export const metadata: Metadata = {
  title: {
    default: "AGEON Personal Site",
    template: "%s - AGEON",
  },
  description: "Personal technology site with projects, community questions, AI chat, and interview practice.",
  icons: {
    icon: "/icon.svg",
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
    <html lang="zh-CN">
      <body>
        <AuthProvider>
          <ParticleBackground />
          <div className="noise-overlay" />
          <div className="site-content"><Navbar />{children}<SiteFooter /></div>
        </AuthProvider>
      </body>
    </html>
  )
}
