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
        <RouterLink to="/leaderboard" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M8 20V10M12 20V4M16 20v-7" /></svg>
          <span>排行榜</span>
        </RouterLink>
        <RouterLink to="/submissions" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M5 4h14v16H5zM8 9h8M8 13h5" /></svg>
          <span>提交记录</span>
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="nav-item">
          <svg viewBox="0 0 24 24" class="ico"><path d="M12 3l8 4v6c0 4-3.4 7.2-8 8-4.6-.8-8-4-8-8V7z" /></svg>
          <span>题目管理</span>
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
          <span class="avatar">{{ initial }}</span>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.nickname || auth.user?.username }}</div>
            <div class="user-role">{{ auth.isAdmin ? '管理员' : '学生' }}</div>
          </div>
        </div>
        <button class="logout" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../stores/auth'
import { useSubmissionStore } from '../stores/submission'
import { useTheme } from '../composables/theme'

const auth = useAuthStore()
const submissionStore = useSubmissionStore()
const router = useRouter()
const { theme, toggle: toggleTheme } = useTheme()

const { connected } = storeToRefs(submissionStore)

const initial = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || '?'
  return name.slice(0, 1).toUpperCase()
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

.main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  position: relative;
}
</style>