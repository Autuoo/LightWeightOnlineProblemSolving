import { defineStore } from 'pinia'
import { authApi } from '../api'

const TOKEN_KEY = 'oj_token'
const USER_KEY = 'oj_user'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    loading: false,
    error: ''
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },

  actions: {
    async login(username, password) {
      this.loading = true
      this.error = ''
      try {
        const data = await authApi.login({ username, password })
        this.token = data.token
        this.user = data.user
        localStorage.setItem(TOKEN_KEY, data.token)
        localStorage.setItem(USER_KEY, JSON.stringify(data.user))
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async refresh() {
      if (!this.token) return
      try {
        this.user = await authApi.me()
        localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      } catch {
        this.clear()
      }
    },

    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})