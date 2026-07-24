const AI_ERROR_MESSAGES: Record<string, string> = {
  AI_MODEL_BUSY: "模型服务繁忙，请稍后再试",
  AI_MODEL_AUTH_ERROR: "模型服务配置错误，请联系管理员",
  AI_MODEL_REQUEST_REJECTED: "模型配置或请求参数不正确",
  AI_REQUEST_IN_PROGRESS: "已有回答正在生成，请稍后再试",
}

export function toAiErrorMessage(code: string, fallback: string) {
  return AI_ERROR_MESSAGES[code] ?? fallback
}
