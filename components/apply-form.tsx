"use client"

import { useEffect, useState, type FormEvent } from "react"
import { Link2, RefreshCw, X } from "lucide-react"
import { ApiClientError } from "@/lib/api-client"
import { createApplication, previewApplicationSource, updateApplication, type ApplicationItem } from "@/lib/application-api"

const COMPANY_TYPES = ["国企", "民企", "外企", "银行", "互联网", "其他"]
const CHANNELS = ["官网", "内推", "宣讲会", "公众号", "猎头", "其他"]

function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${now.getFullYear()}-${month}-${day}`
}

type ApplyFormProps = {
  open: boolean
  initial: ApplicationItem | null
  onClose: () => void
  onSaved: () => void
}

export function ApplyForm({ open, initial, onClose, onSaved }: ApplyFormProps) {
  const [company, setCompany] = useState("")
  const [position, setPosition] = useState("")
  const [city, setCity] = useState("")
  const [companyType, setCompanyType] = useState("")
  const [channel, setChannel] = useState("")
  const [sourceUrl, setSourceUrl] = useState("")
  const [deadlineAt, setDeadlineAt] = useState("")
  const [appliedAt, setAppliedAt] = useState(today())
  const [note, setNote] = useState("")
  const [logoUrl, setLogoUrl] = useState("")
  const [parsing, setParsing] = useState(false)
  const [parseHint, setParseHint] = useState("")
  const [parseFailed, setParseFailed] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")

  useEffect(() => {
    if (!open) return
    setCompany(initial?.company ?? "")
    setPosition(initial?.position ?? "")
    setCity(initial?.city ?? "")
    setCompanyType(initial?.companyType ?? "")
    setChannel(initial?.channel ?? "")
    setSourceUrl(initial?.sourceUrl ?? "")
    setDeadlineAt(initial?.deadlineAt ?? "")
    setAppliedAt(initial?.appliedAt ?? today())
    setNote(initial?.note ?? "")
    setLogoUrl(initial?.sourceLogoUrl ?? "")
    setParseHint("")
    setParseFailed(false)
    setError("")
  }, [open, initial])

  async function parseSource() {
    const url = sourceUrl.trim()
    if (!url || parsing) return
    setParsing(true)
    setParseHint("")
    setParseFailed(false)
    try {
      const preview = await previewApplicationSource(url)
      if (preview.error) {
        setParseFailed(true)
        setParseHint(`解析失败：${preview.error}（不影响提交）`)
      } else {
        if (preview.title && !company.trim()) setCompany(preview.title)
        if (preview.logoUrl) setLogoUrl(preview.logoUrl)
        setParseHint(preview.title ? `已解析：${preview.title}` : "解析完成，但未获取到标题。")
      }
    } catch (requestError) {
      setParseFailed(true)
      setParseHint(`${requestError instanceof ApiClientError ? requestError.message : "解析请求失败"}（不影响提交）`)
    } finally {
      setParsing(false)
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    if (!company.trim() || !position.trim()) {
      setError("公司与岗位为必填项。")
      return
    }
    setSubmitting(true)
    setError("")
    const payload = {
      company: company.trim(),
      position: position.trim(),
      city: city.trim() || null,
      companyType: companyType || null,
      channel: channel || null,
      sourceUrl: sourceUrl.trim() || null,
      deadlineAt: deadlineAt || null,
      appliedAt: appliedAt || null,
      note: note.trim() || null,
    }
    try {
      if (initial) {
        await updateApplication(initial.id, payload)
      } else {
        await createApplication(payload)
      }
      onSaved()
      onClose()
    } catch (requestError) {
      setError(requestError instanceof ApiClientError ? requestError.message : "保存失败，请检查登录状态与后端服务。")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={`fixed inset-0 z-[80] ${open ? "" : "pointer-events-none"}`} aria-hidden={!open}>
      <div
        className={`absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity duration-300 ${open ? "opacity-100" : "opacity-0"}`}
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={initial ? "编辑投递" : "新建投递"}
        className={`absolute right-0 top-0 flex h-full w-full max-w-md flex-col border-l border-white/20 bg-[#0b0d0b] transition-transform duration-300 ${open ? "translate-x-0" : "translate-x-full"}`}
      >
        <div className="flex items-center justify-between border-b border-white/10 p-5">
          <div>
            <p className="eyebrow">{initial ? "EDIT RECORD" : "NEW RECORD"}</p>
            <h2 className="mt-2 text-2xl">{initial ? "编辑投递" : "新建投递"}</h2>
          </div>
          <button type="button" onClick={onClose} className="interactive grid h-10 w-10 place-items-center border border-white/15" aria-label="关闭"><X size={18} /></button>
        </div>

        <form onSubmit={submit} className="grid flex-1 content-start gap-5 overflow-y-auto p-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-2 text-sm"><span className="text-white/55">公司 *</span><input value={company} onChange={(event) => setCompany(event.target.value)} required maxLength={60} className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" placeholder="公司名" /></label>
            <label className="grid gap-2 text-sm"><span className="text-white/55">岗位 *</span><input value={position} onChange={(event) => setPosition(event.target.value)} required maxLength={80} className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" placeholder="如 后端开发工程师" /></label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-2 text-sm"><span className="text-white/55">城市</span><input value={city} onChange={(event) => setCity(event.target.value)} maxLength={40} className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" placeholder="如 杭州" /></label>
            <label className="grid gap-2 text-sm"><span className="text-white/55">公司类型</span><select value={companyType} onChange={(event) => setCompanyType(event.target.value)} className="h-12 border border-white/15 bg-[#0b0d0b] px-4 outline-none focus:border-[#9ef01a]"><option value="">未选择</option>{COMPANY_TYPES.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
          </div>

          <label className="grid gap-2 text-sm"><span className="text-white/55">投递渠道</span><select value={channel} onChange={(event) => setChannel(event.target.value)} className="h-12 border border-white/15 bg-[#0b0d0b] px-4 outline-none focus:border-[#9ef01a]"><option value="">未选择</option>{CHANNELS.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>

          <div className="grid gap-2 text-sm">
            <span className="text-white/55">网申链接</span>
            <div className="flex gap-2">
              <input value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} type="url" maxLength={500} className="h-12 min-w-0 flex-1 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" placeholder="https://…" />
              <button type="button" onClick={parseSource} disabled={!sourceUrl.trim() || parsing} className="interactive flex h-12 shrink-0 items-center gap-2 border border-[#9ef01a]/60 px-4 text-sm text-[#9ef01a] disabled:opacity-40">
                {parsing ? <RefreshCw size={15} className="animate-spin" /> : <Link2 size={15} />} 解析链接
              </button>
            </div>
            {logoUrl && (
              <span className="flex items-center gap-2 text-xs text-white/45">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={logoUrl} alt="网站 logo" className="h-5 w-5 border border-white/15 object-cover" /> 已获取网站图标
              </span>
            )}
            {parseHint && <span className={`text-xs ${parseFailed ? "text-[#ffaaa3]" : "text-white/40"}`}>{parseHint}</span>}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-2 text-sm"><span className="text-white/55">投递日期</span><input value={appliedAt} onChange={(event) => setAppliedAt(event.target.value)} type="date" className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" /></label>
            <label className="grid gap-2 text-sm"><span className="text-white/55">截止日期</span><input value={deadlineAt} onChange={(event) => setDeadlineAt(event.target.value)} type="date" className="h-12 border border-white/15 bg-black/25 px-4 outline-none focus:border-[#9ef01a]" /></label>
          </div>

          <label className="grid gap-2 text-sm"><span className="text-white/55">备注</span><textarea value={note} onChange={(event) => setNote(event.target.value)} maxLength={1000} rows={4} className="resize-none border border-white/15 bg-black/25 p-4 outline-none focus:border-[#9ef01a]" placeholder="内推人、笔试安排、面经链接…" /></label>

          {error && <div className="border border-[#ff6b5f]/40 bg-[#ff6b5f]/8 p-3 text-sm text-[#ffaaa3]">{error}</div>}

          <div className="flex justify-end gap-2 pb-4">
            <button type="button" onClick={onClose} className="interactive h-11 border border-white/15 px-4 text-sm">取消</button>
            <button type="submit" disabled={submitting} className="interactive h-11 bg-[#9ef01a] px-5 text-sm font-semibold text-black disabled:opacity-50">{submitting ? "保存中..." : initial ? "保存修改" : "创建投递"}</button>
          </div>
        </form>
      </aside>
    </div>
  )
}
