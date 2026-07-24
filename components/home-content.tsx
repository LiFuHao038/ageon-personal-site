import Link from "next/link"
import { ArrowRight, ArrowUpRight, Bot, BrainCircuit, Code2, MessagesSquare } from "lucide-react"
import { projects, siteNav } from "@/lib/site-data"
import { ProfileFolders } from "@/components/profile-folders"

const routeIcons = [Code2, MessagesSquare, Bot, BrainCircuit]

export function HomeContent() {
  return (
    <div id="explore">
      <section className="border-b border-white/10 py-20 md:py-28">
        <div className="site-shell">
          <div className="mb-10 flex items-end justify-between"><p className="eyebrow">01 / PROFILE FOLDERS</p><span className="mono hidden text-[9px] text-white/30 sm:block">HOVER / FOCUS / OPEN</span></div>
          <ProfileFolders />
        </div>
      </section>

      <section className="border-b border-white/10 py-20 md:py-28">
        <div className="site-shell">
          <div className="mb-12 flex items-end justify-between gap-6">
            <div>
              <p className="eyebrow">02 / PROJECTS</p>
              <h2 className="mt-4 text-4xl font-medium md:text-6xl">项目作品</h2>
            </div>
            <span className="mono hidden text-[10px] text-white/35 sm:block">SELECTED / 02</span>
          </div>

          <div className="border-t border-white/15">
            {projects.map((project, index) => (
              <article key={project.title} className="group grid gap-5 border-b border-white/15 py-7 md:grid-cols-[70px_1fr_auto] md:items-center md:py-10">
                <span className="mono text-xs text-white/35">0{index + 1}</span>
                <div>
                  <div className="flex flex-wrap items-center gap-3">
                    <h3 className="text-2xl transition-transform group-hover:translate-x-2 md:text-4xl">{project.title}</h3>
                    <span className="mono border border-white/15 px-2 py-1 text-[9px] text-white/45">{project.status}</span>
                  </div>
                  <p className="mt-3 max-w-xl text-sm leading-7 text-white/50">{project.description}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {project.tags.map((tag) => <span key={tag} className="mono text-[10px] text-white/38">#{tag}</span>)}
                  </div>
                </div>
                <div className="grid h-11 w-11 place-items-center border border-white/15 group-hover:border-[#9ef01a] group-hover:text-[#9ef01a]">
                  <ArrowUpRight size={18} />
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="py-20 md:py-28">
        <div className="site-shell">
          <p className="eyebrow">03 / ENTER</p>
          <div className="mt-8 grid border-l border-t border-white/15 md:grid-cols-2 lg:grid-cols-4">
            {siteNav.map((item, index) => {
              const Icon = routeIcons[index]
              return (
                <Link key={item.href} href={item.href} className="interactive group flex min-h-56 flex-col justify-between border-b border-r border-white/15 bg-[#0b0d0b]/70 p-6 hover:bg-[#111510]">
                  <div className="flex items-start justify-between">
                    <Icon size={23} strokeWidth={1.5} />
                    <span className="mono text-[10px] text-white/35">0{index + 1}</span>
                  </div>
                  <div>
                    <span className="mono text-[10px] tracking-[0.15em] text-[#9ef01a]">{item.short}</span>
                    <div className="mt-3 flex items-center justify-between text-2xl">
                      {item.label}<ArrowRight className="transition-transform group-hover:translate-x-1" size={20} />
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        </div>
      </section>
    </div>
  )
}
