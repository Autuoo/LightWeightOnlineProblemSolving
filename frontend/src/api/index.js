import http from './http'

export const authApi = {
  login: (payload) => http.post('/auth/login', payload),
  me: () => http.get('/auth/me')
}

export const problemApi = {
  list: (params) => http.get('/problems', { params }),
  detail: (id) => http.get(`/problems/${id}`)
}

export const submissionApi = {
  submit: (payload) => http.post('/submissions', payload),
  detail: (id) => http.get(`/submissions/${id}`),
  list: (params) => http.get('/submissions', { params })
}

export const leaderboardApi = {
  list: (limit = 20) => http.get('/leaderboard', { params: { limit } })
}

export const adminApi = {
  createProblem: (payload) => http.post('/admin/problems', payload),
  updateProblem: (id, payload) => http.put(`/admin/problems/${id}`, payload),
  replaceTestCases: (id, payload) => http.post(`/admin/problems/${id}/test-cases`, payload)
}

export const TERMINAL_STATUSES = ['AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'REJECTED']

export function isTerminal(status) {
  return TERMINAL_STATUSES.includes(status)
}