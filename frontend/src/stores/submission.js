import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { submissionApi, isTerminal } from '../api'
import { useAuthStore } from './auth'

let stompClient = null

export const useSubmissionStore = defineStore('submission', {
  state: () => ({
    detail: null,
    loading: false,
    error: '',
    connected: false,
    polling: false,
    lastEventAt: 0
  }),

  getters: {
    status: (state) => state.detail?.status || 'PENDING',
    isFinished: (state) => isTerminal(state.detail?.status)
  },

  actions: {
    async load(id, { force = false } = {}) {
      if (this.detail?.id !== Number(id) || force) {
        this.detail = null
      }
      this.loading = !this.detail
      this.error = ''
      try {
        this.detail = await submissionApi.detail(id)
        if (!isTerminal(this.detail.status)) {
          this.subscribe(Number(id))
        }
      } catch (error) {
        this.error = error.message
      } finally {
        this.loading = false
      }
    },

    mergeEvent(event) {
      this.lastEventAt = Date.now()
      if (!this.detail || this.detail.id !== event.submissionId) return
      this.detail.status = event.status
      if (event.score !== undefined && event.score !== null) this.detail.score = event.score
      if (event.execTimeMs !== undefined && event.execTimeMs !== null) this.detail.execTimeMs = event.execTimeMs
      if (event.memoryKb !== undefined && event.memoryKb !== null) this.detail.memoryKb = event.memoryKb
      if (event.finishedAt) this.detail.finishedAt = event.finishedAt
      if (isTerminal(event.status)) {
        this.stopRealtime()
        this.refreshCases()
      }
    },

    async refreshCases() {
      if (!this.detail) return
      try {
        const fresh = await submissionApi.detail(this.detail.id)
        this.detail = fresh
      } catch {
        /* 保持当前快照 */
      }
    },

    subscribe(id) {
      this.unsubscribe()
      const auth = useAuthStore()

      stompClient = new Client({
        webSocketFactory: () =>
          new SockJS(
            import.meta.env.VITE_WS_BASE ||
              `${window.location.protocol}//${window.location.hostname}:8081/ws`
          ),
        connectHeaders: auth.token ? { Authorization: `Bearer ${auth.token}` } : {},
        reconnectDelay: 4000,
        debug: () => {},
        onConnect: () => {
          this.connected = true
          stompClient.subscribe(`/topic/submissions/${id}`, (frame) => {
            try {
              this.mergeEvent(JSON.parse(frame.body))
            } catch {
              /* 忽略非法帧 */
            }
          })
        },
        onWebSocketClose: () => {
          this.connected = false
        },
        onStompError: () => {
          this.connected = false
        }
      })

      stompClient.activate()
      this.startPolling(id)
    },

    startPolling(id) {
      this.stopPolling()
      this.polling = true
      const timer = setInterval(async () => {
        if (!this.detail || isTerminal(this.detail.status)) {
          this.stopPolling()
          return
        }
        if (Date.now() - this.lastEventAt < 1600) return
        try {
          const fresh = await submissionApi.detail(id)
          this.detail = fresh
          if (isTerminal(fresh.status)) {
            this.stopPolling()
            this.unsubscribe()
          }
        } catch {
          /* 网络抖动时继续下一轮 */
        }
      }, 1600)
      this.pollTimer = timer
    },

    stopPolling() {
      this.polling = false
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    unsubscribe() {
      if (stompClient) {
        try {
          stompClient.deactivate()
        } catch {
          /* 已断开 */
        }
        stompClient = null
      }
      this.connected = false
    },

    stopRealtime() {
      this.stopPolling()
      this.unsubscribe()
    }
  }
})