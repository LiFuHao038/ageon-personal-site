import Link from "next/link"
import { ArrowUpRight } from "lucide-react"

export function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-[#090a09]">
      <div className="site-shell flex flex-col gap-8 py-10 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="eyebrow">NEXT STEP</p>
          <Link href="/community" className="mt-3 inline-flex items-center gap-3 text-2xl hover:text-[#9ef01a]">
            留下一个问题 <ArrowUpRight size={22} />
          </Link>
        </div>
        <div className="mono flex flex-wrap gap-x-6 gap-y-2 text-[11px] text-white/45">
          <a href="https://github.com/" target="_blank" rel="noreferrer">GITHUB</a>
          <span>CHONGQING / CN</span>
          <span>© {new Date().getFullYear()} LFH</span>
        </div>
      </div>
    </footer>
  )
}
