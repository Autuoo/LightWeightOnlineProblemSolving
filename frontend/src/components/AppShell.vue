<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">›_</span>
        <div class="brand-text">
          <div class="brand-name">OJ Console</div>
          <div class="brand-sub">Lightweight Judge</div>
        </div>
      </div>

      <nav class="nav">
        <RouterLink to="/problems" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M4 6h16M4 12h10M4 18h13" /></svg>
          <span>题库</span>
        </RouterLink>
        <RouterLink to="/contests" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico">
            <path d="M8 4h8v4a4 4 0 0 1-8 0V4M6 4H4v2a4 4 0 0 0 4 4M18 4h2v2a4 4 0 0 1-4 4M12 12v4M8 20h8M9 16h6" />
          </svg>
          <span>比赛</span>
        </RouterLink>
        <RouterLink to="/leaderboard" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M8 20V10M12 20V4M16 20v-7" /></svg>
          <span>排行榜</span>
        </RouterLink>
        <RouterLink to="/submissions" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M5 4h14v16H5zM8 9h8M8 13h5" /></svg>
          <span>提交记录</span>
        </RouterLink>
        <RouterLink to="/settings" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico">
            <path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h10M18 18h2" />
            <circle cx="16" cy="6" r="2" />
            <circle cx="10" cy="12" r="2" />
            <circle cx="16" cy="18" r="2" />
          </svg>
          <span>个人设置</span>
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin/contests" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico">
            <path d="M8 4h8v4a4 4 0 0 1-8 0V4M6 4H4v2a4 4 0 0 0 4 4M18 4h2v2a4 4 0 0 1-4 4M12 12v4M8 20h8M9 16h6" />
          </svg>
          <span>比赛管理</span>
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M12 3l8 4v6c0 4-3.4 7.2-8 8-4.6-.8-8-4-8-8V7z" /></svg>
          <span>题目管理</span>
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin/users" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico">
            <path d="M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M19 8v6M22 11h-6" />
          </svg>
          <span>用户管理</span>
        </RouterLink>
      </nav>

      <div class="side-foot">
        <button class="theme-toggle" @click="toggleTheme">
          <svg v-if="theme === 'dark'" viewBox="0 0 24 24" class="ico">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" class="ico">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
          </svg>
          <span>{{ theme === 'dark' ? '深色模式' : '浅色模式' }}</span>
        </button>
        <div class="conn">
          <span class="conn-dot" :class="{ live: connected }" />
          <span class="conn-text">{{ connected ? '实时通道已连接' : '轮询模式' }}</span>
        </div>
        <div class="user">
          <img v-if="auth.user?.avatarUrl" class="avatar avatar-img" :src="avatarSrc" alt="" />
          <span v-else class="avatar">{{ initial }}</span>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.nickname || auth.user?.username }}</div>
            <div class="user-role">{{ auth.isAdmin ? '管理员' : '学生' }}</div>
          </div>
        </div>
        <button class="logout" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main">
      <div v-if="showAiNotice" class="ai-notice">
        <span class="ai-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
          </svg>
        </span>
        <div class="ai-copy">
          <div class="ai-title">AI 助教尚未启用</div>
          <div class="ai-desc">
            填写你自己的模型端点与 API Key 后，才能获得失败分析、复杂度建议与历史回看。
          </div>
        </div>
        <RouterLink class="ai-cta" to="/settings">
          去配置
          <span class="ai-cta-arrow">→</span>
        </RouterLink>
        <button class="ai-close" aria-label="关闭提示" @click="dismissNotice">×</button>
      </div>

      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { hintApi } from '../api'
import { API_BASE } from '../api/config'
import { useAuthStore } from '../stores/auth'
import { useSubmissionStore } from '../stores/submission'
import { useTheme } from '../composables/theme'

const auth = useAuthStore()
const submissionStore = useSubmissionStore()
const router = useRouter()
const { theme, toggle: toggleTheme } = useTheme()

const connected = computed(() => submissionStore.connected)

const initial = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || '?'
  return name.slice(0, 1).toUpperCase()
})

const API_ORIGIN = API_BASE.replace(/\/api$/, '')

const avatarSrc = computed(() =>
  auth.user?.avatarUrl ? `${API_ORIGIN}${auth.user.avatarUrl}` : ''
)

const aiStatus = ref(null)
const aiNoticeDismissed = ref(sessionStorage.getItem('ai-notice-dismissed') === '1')

const showAiNotice = computed(() => {
  const status = aiStatus.value
  return Boolean(status && status.globalEnabled && !status.configured && !aiNoticeDismissed.value)
})

function dismissNotice() {
  aiNoticeDismissed.value = true
  sessionStorage.setItem('ai-notice-dismissed', '1')
}

onMounted(async () => {
  if (!auth.isLoggedIn) return
  try {
    aiStatus.value = await hintApi.status()
  } catch {
    aiStatus.value = null
  }
})

function logout() {
  submissionStore.stopRealtime()
  auth.clear()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 216px;
  flex: 0 0 216px;
  display: flex;
  flex-direction: column;
  padding: 18px 14px;
  border-right: 1px solid var(--line);
  background: linear-gradient(180deg, var(--bg-elev) 0%, var(--bg) 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 22px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--accent-dim);
  border: 1px solid rgba(45, 212, 191, 0.35);
  color: var(--accent);
  font-family: var(--mono);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -1px;
}

.brand-name {
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0.02em;
}

.brand-sub {
  font-size: 11px;
  color: var(--text-faint);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 10px;
  border-radius: 8px;
  color: var(--text-dim);
  font-weight: 500;
  position: relative;
  transition: background 0.18s ease, color 0.18s ease;
}

.nav-item:hover {
  background: var(--bg-elev-2);
  color: var(--text);
}

.nav-item.router-link-active {
  background: var(--accent-dim);
  color: var(--accent);
}

.nav-item.router-link-active::before {
  content: "";
  position: absolute;
  left: -14px;
  top: 9px;
  bottom: 9px;
  width: 2px;
  border-radius: 2px;
  background: var(--accent);
}

.ico {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.side-foot {
  margin-top: auto;
  padding: 14px 6px 0;
  border-top: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.theme-toggle {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--bg-elev-2);
  color: var(--text-dim);
  font-size: 12px;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.theme-toggle:hover {
  color: var(--text);
  border-color: var(--line-strong);
}

.conn {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: var(--text-faint);
}

.conn-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3a4655;
  transition: background 0.25s ease, box-shadow 0.25s ease;
}

.conn-dot.live {
  background: var(--ok);
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.16);
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--bg-elev-3);
  border: 1px solid var(--line-strong);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-dim);
}

.avatar-img {
  object-fit: cover;
  padding: 0;
}

.user-meta {
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 550;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: var(--text-faint);
}

.logout {
  align-self: flex-start;
  font-size: 12px;
  color: var(--text-faint);
  padding: 2px 0;
  transition: color 0.18s ease;
}

.logout:hover {
  color: var(--err);
}

.ai-notice {
  position: sticky;
  top: 0;
  z-index: 6;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 22px;
  color: #f6d98a;
  background: linear-gradient(
    90deg,
    rgba(251, 191, 36, 0.18),
    rgba(251, 191, 36, 0.07) 55%,
    rgba(251, 191, 36, 0.14)
  );
  border-bottom: 1px solid rgba(251, 191, 36, 0.4);
  box-shadow: 0 10px 30px -22px rgba(251, 191, 36, 0.9);
  backdrop-filter: blur(10px);
  animation: notice-in 0.36s cubic-bezier(0.22, 0.68, 0.35, 1);
}

@keyframes notice-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.ai-icon {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: rgba(251, 191, 36, 0.16);
  border: 1px solid rgba(251, 191, 36, 0.32);
}

.ai-icon svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: #fbbf24;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.ai-copy {
  flex: 1;
  min-width: 0;
}

.ai-title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.01em;
  color: #fbbf24;
}

.ai-desc {
  margin-top: 2px;
  font-size: 11.5px;
  line-height: 1.65;
  color: rgba(246, 217, 138, 0.9);
}

.ai-cta {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 650;
  color: #1a1305;
  background: linear-gradient(180deg, #fcd34d, #f5b301);
  box-shadow: 0 5px 16px -8px rgba(251, 191, 36, 0.9);
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}

.ai-cta:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 8px 20px -8px rgba(251, 191, 36, 1);
}

.ai-cta-arrow {
  transition: transform 0.18s ease;
}

.ai-cta:hover .ai-cta-arrow {
  transform: translateX(3px);
}

.ai-close {
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: rgba(246, 217, 138, 0.6);
  font-size: 16px;
  line-height: 1;
  transition: background 0.16s ease, color 0.16s ease;
}

.ai-close:hover {
  background: rgba(251, 191, 36, 0.12);
  color: #fbbf24;
}

@media (max-width: 900px) {
  .ai-desc {
    display: none;
  }
}

.main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  position: relative;
}

[data-theme="light"] .ai-notice {
  color: #7a4306;
  background: linear-gradient(
    90deg,
    rgba(251, 191, 36, 0.34),
    rgba(251, 191, 36, 0.16) 55%,
    rgba(251, 191, 36, 0.26)
  );
  border-bottom-color: rgba(180, 83, 9, 0.45);
  box-shadow: 0 10px 26px -22px rgba(146, 64, 14, 0.45);
}

[data-theme="light"] .ai-icon {
  background: rgba(251, 191, 36, 0.32);
  border-color: rgba(180, 83, 9, 0.4);
}

[data-theme="light"] .ai-icon svg {
  stroke: #b45309;
}

[data-theme="light"] .ai-title {
  color: #713f12;
}

[data-theme="light"] .ai-desc {
  color: #6b4a10;
}

[data-theme="light"] .ai-close {
  color: #7c5a13;
}

[data-theme="light"] .ai-close:hover {
  background: rgba(251, 191, 36, 0.28);
  color: #713f12;
}
</style>