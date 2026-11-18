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

export const hintApi = {
  status: () => http.get('/ai/status'),
  history: (submissionId) => http.get(`/submissions/${submissionId}/hints`)
}

export const aiConfigApi = {
  get: () => http.get('/me/ai-config'),
  save: (payload) => http.put('/me/ai-config', payload),
  test: () => http.post('/me/ai-config/test')
}

export const adminApi = {
  listProblems: (params) => http.get('/admin/problems', { params }),
  detailProblem: (id) => http.get(`/admin/problems/${id}`),
  createProblem: (payload) => http.post('/admin/problems', payload),
  updateProblem: (id, payload) => http.put(`/admin/problems/${id}`, payload),
  replaceTestCases: (id, payload) => http.post(`/admin/problems/${id}/test-cases`, payload)
}

export const adminUserApi = {
  list: (params) => http.get('/admin/users', { params }),
  updateStatus: (id, status) => http.patch(`/admin/users/${id}/status`, { status }),
  resetPassword: (id) => http.post(`/admin/users/${id}/reset-password`),
  create: (payload) => http.post('/admin/users', payload),
  aiConfig: (id) => http.get(`/admin/users/${id}/ai-config`),
  clearAiConfig: (id) => http.delete(`/admin/users/${id}/ai-config`)
}

export const TERMINAL_STATUSES = ['AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'REJECTED']

export function isTerminal(status) {
  return TERMINAL_STATUSES.includes(status)
}