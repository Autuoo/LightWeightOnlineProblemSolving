import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppShell from '../components/AppShell.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: AppShell,
    children: [
      { path: '', redirect: '/problems' },
      { path: 'problems', name: 'problems', component: () => import('../views/ProblemList.vue') },
      { path: 'problems/:id', name: 'problem-detail', component: () => import('../views/ProblemDetail.vue') },
      { path: 'leaderboard', name: 'leaderboard', component: () => import('../views/Leaderboard.vue') },
      { path: 'submissions', name: 'submissions', component: () => import('../views/SubmissionList.vue') },
      { path: 'submissions/:id', name: 'submission-detail', component: () => import('../views/SubmissionDetail.vue') },
      {
        path: 'admin/problems',
        name: 'admin-problems',
        component: () => import('../views/AdminProblems.vue'),
        meta: { admin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/problems' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !auth.isAdmin) {
    return { name: 'problems' }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'problems' }
  }
  return true
})

export default router