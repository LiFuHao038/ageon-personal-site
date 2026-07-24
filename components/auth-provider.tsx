"use client"

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react"
import { getCurrentUser, loginUser, registerUser, type AuthResponse, type CurrentUser } from "@/lib/auth-api"
import { TOKEN_STORAGE_KEY } from "@/lib/api-client"

const USER_STORAGE_KEY = "ageon-current-user"
const ACCESS_TOKEN_STORAGE_KEY: typeof TOKEN_STORAGE_KEY = "ageon-access-token"

type AuthContextValue = {
  user: CurrentUser | null
  token: string | null
  loading: boolean
  login: (identifier: string, password: string) => Promise<AuthResponse>
  register: (payload: Parameters<typeof registerUser>[0]) => Promise<AuthResponse>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedToken = window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)
    if (!storedToken) {
      setLoading(false)
      return
    }
    setToken(storedToken)
    void getCurrentUser(storedToken)
      .then((currentUser) => {
        setUser(currentUser)
        window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser))
      })
      .catch(() => {
        window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY)
        window.localStorage.removeItem(USER_STORAGE_KEY)
        setToken(null)
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  async function login(identifier: string, password: string) {
    const response = await loginUser({ identifier, password })
    if (response.token) {
      window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, response.token)
      window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user))
      setToken(response.token)
      setUser(response.user)
    }
    return response
  }

  async function register(payload: Parameters<typeof registerUser>[0]) {
    return registerUser(payload)
  }

  function logout() {
    window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY)
    window.localStorage.removeItem(USER_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }

  const value = useMemo(() => ({ user, token, loading, login, register, logout }), [user, token, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth must be used within AuthProvider")
  return context
}
