import { ref } from 'vue'

const STORAGE_KEY = 'oj_theme'
const theme = ref('dark')

export function applyTheme(next) {
  const value = next === 'light' ? 'light' : 'dark'
  theme.value = value
  document.documentElement.dataset.theme = value
  localStorage.setItem(STORAGE_KEY, value)
  window.dispatchEvent(new CustomEvent('oj-theme-change', { detail: value }))
}

export function initTheme() {
  const saved = localStorage.getItem(STORAGE_KEY)
  const prefersLight = window.matchMedia?.('(prefers-color-scheme: light)').matches
  applyTheme(saved || (prefersLight ? 'light' : 'dark'))
}

export function useTheme() {
  return {
    theme,
    toggle: () => applyTheme(theme.value === 'dark' ? 'light' : 'dark'),
    setTheme: applyTheme
  }
}