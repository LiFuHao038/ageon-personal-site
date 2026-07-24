import { describe, expect, it } from "vitest"
import { initialAiStreamRuntime, reduceAiStreamRuntime } from "../lib/ai-stream-state"
import { toAiErrorMessage } from "../lib/ai-errors"

describe("AI stream runtime", () => {
  it("shows fallback separately and clears it on the first delta", () => {
    const started = reduceAiStreamRuntime(initialAiStreamRuntime, { type: "start" })
    const fallback = reduceAiStreamRuntime(started, {
      type: "fallback",
      model: "kimi/kimi-k3",
    })

    expect(fallback).toEqual({
      generating: true,
      modelNotice: "主模型繁忙，正在切换备用模型…",
    })
    expect(reduceAiStreamRuntime(fallback, { type: "delta" }).modelNotice).toBe("")
  })

  it("always restores the send state after done, error, or abort", () => {
    const generating = { generating: true, modelNotice: "switching" }

    for (const type of ["done", "error", "abort"] as const) {
      expect(reduceAiStreamRuntime(generating, { type })).toEqual(initialAiStreamRuntime)
    }
  })
})

describe("AI error messages", () => {
  it("maps backend errors to safe Chinese messages", () => {
    expect(toAiErrorMessage("AI_MODEL_BUSY", "fallback")).toBe("模型服务繁忙，请稍后再试")
    expect(toAiErrorMessage("AI_MODEL_AUTH_ERROR", "fallback")).toBe("模型服务配置错误，请联系管理员")
    expect(toAiErrorMessage("AI_REQUEST_IN_PROGRESS", "fallback")).toBe("已有回答正在生成，请稍后再试")
    expect(toAiErrorMessage("UNKNOWN", "fallback")).toBe("fallback")
  })
})
