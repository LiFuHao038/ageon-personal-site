"use client"

import { useState } from "react"
import { AnimatePresence, motion, useReducedMotion } from "framer-motion"
import { ChevronRight, FolderOpen } from "lucide-react"
import { profileFolders } from "@/lib/profile-folders"

export function ProfileFolders() {
  const [activeId, setActiveId] = useState(profileFolders[0].id)
  const reducedMotion = useReducedMotion()
  const active = profileFolders.find((folder) => folder.id === activeId) ?? profileFolders[0]

  return (
    <div className="profile-folders grid gap-8 lg:grid-cols-[1.15fr_.85fr] lg:items-start">
      <div className="grid border-l border-t border-white/15 sm:grid-cols-2">
        {profileFolders.map((folder, index) => {
          const selected = folder.id === active.id
          return (
            <motion.button
              key={folder.id}
              type="button"
              onClick={() => setActiveId(folder.id)}
              onFocus={() => setActiveId(folder.id)}
              onMouseEnter={() => setActiveId(folder.id)}
              whileHover={reducedMotion ? undefined : { y: -4 }}
              className={`profile-folder group relative min-h-40 overflow-hidden border-b border-r border-white/15 p-5 text-left ${index === profileFolders.length - 1 ? "sm:col-span-2" : ""}`}
              aria-pressed={selected}
            >
              <span className="absolute inset-x-5 top-4 h-16 translate-y-3 border border-white/8 bg-white/[0.025] transition-transform group-hover:translate-y-1" />
              <span className="absolute inset-x-3 top-8 h-20 border border-white/10 bg-[#0d100d]" />
              <span className="relative flex h-full flex-col justify-between">
                <span className="flex items-start justify-between"><span className="mono text-[9px]" style={{ color: folder.accent }}>{folder.eyebrow}</span><FolderOpen size={17} style={{ color: selected ? folder.accent : "rgba(255,255,255,.3)" }} /></span>
                <span className="flex items-end justify-between gap-4"><strong className="text-xl font-medium md:text-2xl">{folder.title}</strong><ChevronRight size={17} className={`transition-transform ${selected ? "translate-x-1" : "text-white/25"}`} /></span>
              </span>
            </motion.button>
          )
        })}
      </div>

      <div className="min-h-80 border-y border-white/15 py-6 lg:border lg:p-7">
        <AnimatePresence mode="wait">
          <motion.div key={active.id} initial={reducedMotion ? false : { opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={reducedMotion ? undefined : { opacity: 0, y: -8 }} transition={{ duration: 0.22 }}>
            <span className="mono text-[10px]" style={{ color: active.accent }}>{active.eyebrow} / {active.title}</span>
            <p className="mt-5 max-w-md text-lg leading-8 text-white/68">{active.summary}</p>
            <div className="mt-8 border-t border-white/12">{active.items.map((item, index) => <div key={item} className="flex items-center justify-between border-b border-white/12 py-4"><span className="text-sm text-white/72">{item}</span><span className="mono text-[9px] text-white/25">0{index + 1}</span></div>)}</div>
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}
