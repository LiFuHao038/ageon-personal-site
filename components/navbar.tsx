"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { LogIn, LogOut, Menu, ShieldCheck, UserRound, X } from "lucide-react"
import { siteNav } from "@/lib/site-data"
import { useAuth } from "@/components/auth-provider"

export function Navbar() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const { user, logout } = useAuth()

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-white/10 bg-[#070807]/88 backdrop-blur-xl">
      <nav className="site-shell flex h-[72px] items-center justify-between" aria-label="主导航">
        <Link href="/" className="group flex items-center gap-3" onClick={() => setOpen(false)}>
          <span className="grid h-8 w-8 place-items-center border border-white/20 bg-[#9ef01a] text-xs font-black text-black transition-transform group-hover:rotate-6">
            LF
          </span>
          <span className="mono text-[11px] tracking-[0.16em] text-white/65">BUILD / LEARN / SHARE</span>
        </Link>

        <div className="hidden items-center gap-1 md:flex">
          {siteNav.map((item, index) => {
            const active = pathname === item.href
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`interactive flex h-10 items-center gap-2 border px-3 text-sm ${
                  active ? "border-[#9ef01a] bg-[#9ef01a] text-black" : "border-transparent text-white/65 hover:text-white"
                }`}
              >
                <span className="mono text-[10px] opacity-60">0{index + 1}</span>
                {item.label}
              </Link>
            )
          })}
        </div>

        <div className="hidden items-center gap-2 md:flex">
          {user ? (
            <>
              {user.role === "ADMIN" && <Link href="/admin" className="interactive grid h-10 w-10 place-items-center border border-white/15" aria-label="管理端"><ShieldCheck size={16} /></Link>}
              <span className="flex h-10 items-center gap-2 border border-white/15 px-3 text-xs text-white/65"><UserRound size={15} className="text-[#9ef01a]" />{user.displayName}</span>
              <button type="button" onClick={logout} className="interactive grid h-10 w-10 place-items-center border border-white/15" aria-label="退出登录"><LogOut size={15} /></button>
            </>
          ) : (
            <Link href="/auth" className="interactive flex h-10 items-center gap-2 border border-white/15 px-3 text-xs"><LogIn size={15} /> 登录 / 注册</Link>
          )}
        </div>

        <button
          type="button"
          className="grid h-10 w-10 place-items-center border border-white/15 md:hidden"
          onClick={() => setOpen((value) => !value)}
          aria-expanded={open}
          aria-label={open ? "关闭菜单" : "打开菜单"}
        >
          {open ? <X size={18} /> : <Menu size={18} />}
        </button>
      </nav>

      {open && (
        <div className="border-t border-white/10 bg-[#070807] md:hidden">
          <div className="site-shell grid py-3">
            {siteNav.map((item, index) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className="flex min-h-14 items-center justify-between border-b border-white/10 text-lg"
              >
                {item.label}
                <span className="mono text-xs text-[#9ef01a]">0{index + 1}</span>
              </Link>
            ))}
            <Link href={user?.role === "ADMIN" ? "/admin" : "/auth"} onClick={() => setOpen(false)} className="flex min-h-14 items-center justify-between border-b border-white/10 text-lg">
              {user ? user.displayName : "登录 / 注册"}<UserRound size={17} className="text-[#9ef01a]" />
            </Link>
            {user && <button type="button" onClick={() => { logout(); setOpen(false) }} className="flex min-h-14 items-center justify-between text-left text-lg">退出登录<LogOut size={17} /></button>}
          </div>
        </div>
      )}
    </header>
  )
}
