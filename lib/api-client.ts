export type ApiErrorPayload = {
  code: string
  message: string
  details?: string[]
}

export class ApiClientError extends Error {
  code: string
  status: number
  details: string[]

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.message)
    this.name = "ApiClientError"
    this.code = payload.code
    this.status = status
    this.details = payload.details ?? []
  }
}

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
export const TOKEN_STORAGE_KEY = "ageon-access-token"

export function readAccessToken() {
  if (typeof window === "undefined") return null
  return window.localStorage.getItem(TOKEN_STORAGE_KEY)
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const accessToken = token === undefined ? readAccessToken() : token
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...init.headers,
    },
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => ({
      code: "REQUEST_FAILED",
      message: `请求失败（${response.status}）`,
      details: [],
    })) as ApiErrorPayload
    throw new ApiClientError(response.status, payload)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
