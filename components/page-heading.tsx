import type { ReactNode } from "react"

type PageHeadingProps = {
  index: string
  eyebrow: string
  title: ReactNode
  description: string
  aside?: ReactNode
}

export function PageHeading({ index, eyebrow, title, description, aside }: PageHeadingProps) {
  return (
    <header className="grid gap-8 border-b border-white/10 pb-12 lg:grid-cols-[1fr_auto] lg:items-end">
      <div>
        <p className="eyebrow">{index} / {eyebrow}</p>
        <h1 className="page-title">{title}</h1>
        <p className="page-lead">{description}</p>
      </div>
      {aside}
    </header>
  )
}
