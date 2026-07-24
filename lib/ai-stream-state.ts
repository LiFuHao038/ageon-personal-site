export type AiStreamRuntime = {
  generating: boolean
  modelNotice: string
}

export type AiStreamRuntimeAction =
  | { type: "start" }
  | { type: "fallback"; model: string }
  | { type: "delta" }
  | { type: "done" | "error" | "abort" }

export const initialAiStreamRuntime: AiStreamRuntime = {
  generating: false,
  modelNotice: "",
}

export function reduceAiStreamRuntime(
  state: AiStreamRuntime,
  action: AiStreamRuntimeAction,
): AiStreamRuntime {
  if (action.type === "start") return { generating: true, modelNotice: "" }
  if (action.type === "fallback") {
    return { ...state, modelNotice: "主模型繁忙，正在切换备用模型…" }
  }
  if (action.type === "delta") return { ...state, modelNotice: "" }
  return initialAiStreamRuntime
}
