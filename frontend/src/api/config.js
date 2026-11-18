const host = window.location.hostname || '127.0.0.1'

const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'

export const API_BASE = import.meta.env.VITE_API_BASE || `${protocol}//${host}:8081/api`

export const WS_BASE = import.meta.env.VITE_WS_BASE || `${protocol}//${host}:8081/ws`