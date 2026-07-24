"use client"

import { useMemo, useState } from "react"
import { ArrowLeft, ArrowRight, BookOpen, Check, ChevronDown, CircleDot, Clock3, LockKeyhole, RotateCcw, Sparkles } from "lucide-react"
import { interviewQuestions } from "@/lib/site-data"

type Mode = "library" | "mock"

export function InterviewLibrary() {
  const [mode, setMode] = useState<Mode>("library")
  const [level, setLevel] = useState("全部")
  const [revealed, setRevealed] = useState<number[]>([])
  const [mastered, setMastered] = useState<number[]>([])
  const [mockIndex, setMockIndex] = useState(0)
  const [mockRevealed, setMockRevealed] = useState(false)

  const filtered = useMemo(() => level === "全部" ? interviewQuestions : interviewQuestions.filter((item) => item.level === level), [level])
  const mockQuestions = interviewQuestions.slice(0, 5)
  const mockFinished = mockIndex >= mockQuestions.length

  function toggle(list: number[], value: number, setter: (next: number[]) => void) {
    setter(list.includes(value) ? list.filter((item) => item !== value) : [...list, value])
  }

  function resetMock() {
    setMockIndex(0)
    setMockRevealed(false)
    setMode("mock")
  }

  return (
    <section className="py-10 md:py-14">
      <div className="flex flex-col gap-4 border-b border-white/15 pb-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-2">
          <button type="button" onClick={() => setMode("library")} className={`interactive h-10 border px-4 text-sm ${mode === "library" ? "border-white bg-white text-black" : "border-white/15 text-white/55"}`}><span className="flex items-center gap-2"><BookOpen size={15} /> 题库浏览</span></button>
          <button type="button" onClick={resetMock} className={`interactive h-10 border px-4 text-sm ${mode === "mock" ? "border-[#9ef01a] bg-[#9ef01a] text-black" : "border-white/15 text-white/55"}`}><span className="flex items-center gap-2"><Sparkles size={15} /> 模拟面试</span></button>
        </div>
        <div className="mono flex gap-5 text-[10px] text-white/35"><span>QUESTIONS {interviewQuestions.length}</span><span>MASTERED {mastered.length}</span></div>
      </div>

      {mode === "library" ? (
        <div className="mt-8 grid gap-8 lg:grid-cols-[240px_1fr]">
          <aside className="h-fit border border-white/15">
            <div className="border-b border-white/15 p-4"><p className="mono text-[10px] tracking-[0.14em] text-white/35">MODULES</p></div>
            <button type="button" className="flex w-full items-center justify-between border-b border-white/10 bg-[#9ef01a]/8 p-4 text-left"><span><strong className="block text-sm font-medium">计算机网络</strong><small className="mono mt-1 block text-[9px] text-[#9ef01a]">{interviewQuestions.length} QUESTIONS</small></span><CircleDot size={16} className="text-[#9ef01a]" /></button>
            {["操作系统", "数据库", "Java"].map((item) => <div key={item} className="flex items-center justify-between border-b border-white/10 p-4 text-sm text-white/32 last:border-0"><span>{item}<small className="mono mt-1 block text-[9px]">COMING NEXT</small></span><LockKeyhole size={14} /></div>)}
          </aside>

          <div>
            <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
              {["全部", "基础", "进阶", "高频"].map((item) => <button key={item} type="button" onClick={() => setLevel(item)} className={`h-9 shrink-0 border px-3 text-xs ${level === item ? "border-[#9ef01a] text-[#9ef01a]" : "border-white/15 text-white/45"}`}>{item}</button>)}
            </div>
            <div className="border-t border-white/15">
              {filtered.map((item, index) => {
                const isRevealed = revealed.includes(item.id)
                const isMastered = mastered.includes(item.id)
                return (
                  <article key={item.id} className="border-b border-white/15 py-6 md:py-8">
                    <div className="flex items-start gap-4">
                      <span className="mono mt-1 text-[10px] text-white/28">{String(index + 1).padStart(2, "0")}</span>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap gap-2"><span className="mono text-[9px] text-[#9ef01a]">{item.category}</span><span className="mono text-[9px] text-white/35">{item.level}</span></div>
                        <h2 className="mt-3 text-lg leading-8 md:text-xl">{item.question}</h2>
                        {isRevealed && <div className="mt-5 border-l-2 border-[#9ef01a] bg-white/[0.025] p-4"><p className="text-sm leading-7 text-white/62">{item.answer}</p><div className="mt-4 flex flex-wrap gap-2">{item.keywords.map((keyword) => <span key={keyword} className="mono border border-white/10 px-2 py-1 text-[9px] text-white/38">{keyword}</span>)}</div></div>}
                        <div className="mt-5 flex flex-wrap gap-2">
                          <button type="button" onClick={() => toggle(revealed, item.id, setRevealed)} className="interactive flex h-9 items-center gap-2 border border-white/15 px-3 text-xs">{isRevealed ? "收起答案" : "查看答案"}<ChevronDown size={14} className={isRevealed ? "rotate-180" : ""} /></button>
                          <button type="button" onClick={() => toggle(mastered, item.id, setMastered)} className={`interactive flex h-9 items-center gap-2 border px-3 text-xs ${isMastered ? "border-[#9ef01a] text-[#9ef01a]" : "border-white/15 text-white/45"}`}><Check size={14} />{isMastered ? "已掌握" : "标记掌握"}</button>
                        </div>
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          </div>
        </div>
      ) : (
        <div className="mx-auto mt-10 max-w-3xl border border-white/15 bg-[#0a0c0a]">
          {!mockFinished ? (
            <>
              <div className="flex items-center justify-between border-b border-white/15 p-4 md:p-5"><div className="mono flex items-center gap-2 text-[10px] text-white/38"><Clock3 size={14} /> MOCK SESSION</div><span className="mono text-[10px] text-[#9ef01a]">{mockIndex + 1} / {mockQuestions.length}</span></div>
              <div className="p-6 md:p-10">
                <p className="eyebrow">QUESTION {String(mockIndex + 1).padStart(2, "0")}</p>
                <h2 className="mt-5 text-2xl leading-10 md:text-4xl md:leading-[1.35]">{mockQuestions[mockIndex].question}</h2>
                <div className="mt-10 min-h-36 border border-dashed border-white/15 p-5">
                  {mockRevealed ? <div><p className="mono text-[9px] text-[#9ef01a]">REFERENCE ANSWER</p><p className="mt-3 text-sm leading-7 text-white/62">{mockQuestions[mockIndex].answer}</p></div> : <div className="grid h-24 place-items-center text-center text-sm text-white/32">先口头回答，再查看参考答案</div>}
                </div>
                <div className="mt-6 flex flex-wrap justify-between gap-3"><button type="button" onClick={() => setMockRevealed((value) => !value)} className="h-11 border border-white/15 px-4 text-sm">{mockRevealed ? "隐藏答案" : "查看答案"}</button><div className="flex gap-2"><button type="button" disabled={mockIndex === 0} onClick={() => { setMockIndex((value) => Math.max(0, value - 1)); setMockRevealed(false) }} className="grid h-11 w-11 place-items-center border border-white/15 disabled:opacity-25" aria-label="上一题"><ArrowLeft size={17} /></button><button type="button" onClick={() => { setMockIndex((value) => value + 1); setMockRevealed(false) }} className="flex h-11 items-center gap-2 bg-[#9ef01a] px-5 text-sm font-semibold text-black">下一题 <ArrowRight size={16} /></button></div></div>
              </div>
            </>
          ) : (
            <div className="grid min-h-[460px] place-items-center p-8 text-center"><div><span className="mx-auto grid h-14 w-14 place-items-center bg-[#9ef01a] text-black"><Check size={24} /></span><p className="eyebrow mt-6">SESSION COMPLETE</p><h2 className="mt-3 text-3xl">完成 5 题模拟面试</h2><p className="mt-4 text-sm text-white/45">下一版可以加入语音回答、AI 追问与评分记录。</p><button type="button" onClick={resetMock} className="mx-auto mt-7 flex h-11 items-center gap-2 border border-white/20 px-4 text-sm"><RotateCcw size={15} /> 再来一轮</button></div></div>
          )}
        </div>
      )}
    </section>
  )
}
