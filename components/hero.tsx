"use client"

import Link from "next/link"
import dynamic from "next/dynamic"
import { motion } from "framer-motion"
import { ArrowDown, ArrowUpRight, MessageSquareText } from "lucide-react"

const SentientSphere = dynamic(() => import("./sentient-sphere").then((mod) => mod.SentientSphere), {
  ssr: false,
  loading: () => (
    <div className="flex h-full w-full items-center justify-center">
      <div className="h-[min(58vw,520px)] w-[min(58vw,520px)] rounded-full border border-white/15 bg-[radial-gradient(circle,rgba(158,240,26,.14),rgba(85,214,190,.04)_42%,transparent_68%)]" />
    </div>
  ),
})

export function Hero() {
  return (
    <section className="relative min-h-[820px] overflow-hidden border-b border-white/10 bg-[#050605] pt-[72px] md:h-screen md:min-h-[760px]">
      <div className="absolute inset-0 motion-reduce:hidden" aria-hidden="true">
        <SentientSphere />
      </div>
      <div className="absolute inset-0 hidden place-items-center motion-reduce:grid" aria-hidden="true">
        <div className="h-[min(58vw,520px)] w-[min(58vw,520px)] rounded-full border border-white/20 bg-[radial-gradient(circle,rgba(158,240,26,.12),transparent_62%)]" />
      </div>

      <div className="site-shell relative z-10 flex min-h-[748px] flex-col justify-between py-12 md:h-[calc(100vh-72px)] md:min-h-[688px] md:py-16">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          className="flex items-start justify-between gap-6"
        >
          <div>
            <p className="eyebrow">SOFTWARE ENGINEERING / CQUPT</p>
            <h1 className="sr-only">AGEON 个人技术站</h1>
          </div>
          <div className="mono hidden text-right text-[10px] leading-5 text-white/40 lg:block">
            JAVA BACKEND<br />AI APPLICATION<br />KNOWLEDGE BASE
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.96, filter: "blur(12px)" }}
          animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
          transition={{ duration: 0.9, delay: 0.12, ease: [0.22, 1, 0.36, 1] }}
          className="pointer-events-none absolute inset-0 z-10 grid place-items-center px-4"
          aria-hidden="true"
        >
          <div className="relative">
            <span className="absolute -inset-x-8 top-1/2 h-px bg-[#9ef01a]/55 shadow-[0_0_28px_rgba(158,240,26,.45)]" />
            <span className="relative block text-center text-[clamp(58px,13vw,176px)] font-black leading-none tracking-normal text-white drop-shadow-[0_0_42px_rgba(158,240,26,.22)]">
              AGEON
            </span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.28 }}
          className="relative z-20 flex flex-col gap-6 md:flex-row md:items-end md:justify-between"
        >
          <div className="max-w-md">
            <div className="mt-6 flex flex-wrap gap-2">
              <Link href="/ai" className="interactive inline-flex h-12 items-center gap-2 border border-[#9ef01a] bg-[#9ef01a] px-4 text-sm font-semibold text-black">
                体验 AI 问答 <ArrowUpRight size={16} />
              </Link>
              <Link href="/community" className="interactive inline-flex h-12 items-center gap-2 border border-white/20 bg-black/30 px-4 text-sm">
                向我提问 <MessageSquareText size={16} />
              </Link>
            </div>
          </div>
          <a href="#explore" className="mono flex items-center gap-3 text-[10px] tracking-[0.16em] text-white/50 hover:text-white">
            EXPLORE <ArrowDown size={15} />
          </a>
        </motion.div>
      </div>
    </section>
  )
}
