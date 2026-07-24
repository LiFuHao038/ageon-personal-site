import { apiRequest } from "@/lib/api-client"

export type AccountStatus = "PENDING" | "APPROVED" | "REJECTED" | "DISABLED"
export type UserRole = "USER" | "ADMIN"

export type CurrentUser = {
  id: number
  displayName: string
  username: string
  email: string
  role: UserRole
  status: AccountStatus
}

export type AuthResponse = {
  token?: string
  user: CurrentUser
  message: string
}

export function registerUser(payload: {
  displayName: string
  username: string
  email: string
  password: string
  acceptedTerms: boolean
}) {
  return apiRequest<AuthResponse>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  }, null)
}

export function loginUser(payload: { identifier: string; password: string }) {
  return apiRequest<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  }, null)
}

export function getCurrentUser(token: string) {
  return apiRequest<CurrentUser>("/api/v1/auth/me", {}, token)
}
