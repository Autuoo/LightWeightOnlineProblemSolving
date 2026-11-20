<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>比赛</h1>
        <p class="sub">参加课程内的算法比赛，比赛期间提交会计入比赛榜单</p>
      </div>
      <div class="filters">
        <input v-model="keyword" class="input search" placeholder="搜索比赛标题" @keyup.enter="load" />
        <button class="btn btn-ghost" :disabled="loading" @click="load">刷新</button>
      </div>
    </header>

    <div v-if="loading" class="cards">
      <div v-for="i in 4" :key="i" class="skeleton card-skeleton" />
    </div>

    <div v-else-if="error" class="empty-state rise">
      <p>{{ error }}</p>
      <button class="btn btn-ghost" @click="load">重试</button>
    </div>

    <div v-else-if="!contests.length" class="empty-state rise">
      <p>暂时没有已发布的比赛</p>
      <p class="empty-sub">管理员发布比赛后会出现在这里</p>
    </div>

    <div v-else class="cards rise">
      <article
        v-for="contest in contests"
        :key="contest.id"
        class="card"
        :class="`phase-${contest.phase.toLowerCase()}`"
      >
        <div class="card-head">
          <span class="phase" :class="`p-${contest.phase.toLowerCase()}`">{{ phaseText(contest.phase) }}</span>
          <span class="mode mono">{{ contest.mode }}</span>
        </div>
        <h2>{{ contest.title }}</h2>
        <p class="time mono">{{ formatRange(contest) }}</p>
        <div class="stats">
          <span><b class="mono">{{ contest.problemCount }}</b> 题</span>
          <span><b class="mono">{{ contest.participantCount }}</b> 人</span>
          <span v-if="contest.registered" class="joined">已报名</span>
          <span v-else-if="contest.phase !== 'ENDED'" class="open">可报名</span>
        </div>
        <div class="card-foot">
          <span v-if="isLive(contest)" class="countdown mono">{{ countdownText(contest) }}</span>
          <span v-else class="hint">{{ phaseHint(contest) }}</span>
          <div class="actions">
            <button
              v-if="!contest.registered && contest.phase !== 'ENDED'"
              class="btn btn-primary small"
              @click="openRegister(contest)"
            >
              {{ contest.phase === 'UPCOMING' ? '报名' : '参加' }}
            </button>
            <RouterLink class="btn btn-ghost small" :to="`/contests/${contest.id}`">
              查看
            </RouterLink>
          </div>
        </div>
      </article>
    </div>

    <div v-if="registerTarget" class="modal-overlay" @click.self="closeRegister">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="register-title">
        <h3 id="register-title">报名「{{ registerTarget.title }}」</h3>
        <p class="modal-hint">比赛结束前均可报名，开始后也能随时参赛。</p>
        <label v-if="registerTarget.joinType === 'PASSWORD'" class="field">
          <span>报名密码</span>
          <input v-model="password" class="input" type="password" placeholder="请输入比赛报名密码" />
        </label>
        <p v-if="registerError" class="error" role="alert">{{ registerError }}</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" :disabled="registering" @click="closeRegister">取消</button>
          <button class="btn btn-primary" :disabled="registering" @click="register">
            <span v-if="registering" class="spinner" />
            {{ registering ? '报名中…' : '确认报名' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { contestApi } from '../api'

const contests = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const now = ref(Date.now())

const registerTarget = ref(null)
const password = ref('')
const registering = ref(false)
const registerError = ref('')
let ticker = null

function phaseText(phase) {
  return { UPCOMING: '即将开始', RUNNING: '进行中', FROZEN: '封榜中', ENDED: '已结束' }[phase] || phase
}

function phaseHint(contest) {
  if (contest.phase === 'UPCOMING') return '开始前可报名，开始后也可参赛'
  if (contest.phase === 'ENDED') return '比赛已结束，可查看题目与榜单'
  return ''
}

function isLive(contest) {
  return contest.phase === 'RUNNING' || contest.phase === 'FROZEN'
}

function targetTime(contest) {
  return new Date(contest.phase === 'UPCOMING' ? contest.startAt : contest.endAt).getTime()
}

function countdownText(contest) {
  const diff = Math.max(0, targetTime(contest) - now.value)
  const day = Math.floor(diff / 86400000)
  const hour = Math.floor((diff % 86400000) / 3600000)
  const minute = Math.floor((diff % 3600000) / 60000)
  const second = Math.floor((diff % 60000) / 1000)
  const parts = day > 0
    ? `${day} 天 ${hour} 时 ${minute} 分`
    : `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  return `${contest.phase === 'UPCOMING' ? '距离开始' : '距离结束'} ${parts}`
}

function formatRange(contest) {
  const format = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    const pad = (n) => String(n).padStart(2, '0')
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${format(contest.startAt)} → ${format(contest.endAt)}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    contests.value = await contestApi.list({ keyword: keyword.value || undefined })
  } catch (exception) {
    error.value = exception.message
    contests.value = []
  } finally {
    loading.value = false
  }
}

function openRegister(contest) {
  registerTarget.value = contest
  password.value = ''
  registerError.value = ''
}

function closeRegister() {
  if (!registering.value) {
    registerTarget.value = null
  }
}

async function register() {
  if (!registerTarget.value || registering.value) return
  registering.value = true
  registerError.value = ''
  try {
    await contestApi.register(registerTarget.value.id, { password: password.value || null })
    registerTarget.value.registered = true
    registerTarget.value.participantCount += 1
    registerTarget.value = null
  } catch (exception) {
    registerError.value = exception.message
  } finally {
    registering.value = false
  }
}

onMounted(() => {
  load()
  ticker = setInterval(() => { now.value = Date.now() }, 1000)
})

onBeforeUnmount(() => {
  if (ticker) clearInterval(ticker)
})
</script>

<style scoped>
.page {
  padding: 28px 36px 48px;
  max-width: 1280px;
}

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

h1 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 650;
}

.sub {
  margin: 0;
  color: var(--text-dim);
  font-size: 13px;
}

.filters {
  display: flex;
  gap: 8px;
}

.search {
  width: 220px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.card-skeleton {
  height: 210px;
  border-radius: 12px;
}

.card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.card:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  box-shadow: 0 18px 36px -28px rgba(0, 0, 0, 0.6);
}

.phase-running {
  border-color: rgba(45, 212, 191, 0.35);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.phase {
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.p-running {
  color: #0d9488;
  background: rgba(45, 212, 191, 0.16);
}

.p-upcoming {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.14);
}

.p-frozen,
.p-ended {
  color: var(--text-dim);
  background: var(--bg-elev-2);
}

.p-ended {
  color: var(--text-faint);
}

.mode {
  font-size: 11px;
  color: var(--text-faint);
}

.card h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 620;
}

.time {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-faint);
}

.stats {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.stats b {
  color: var(--text);
  font-size: 14px;
}

.joined {
  color: var(--ok);
  font-weight: 600;
}

.open {
  color: var(--accent);
  font-weight: 600;
}

.card-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.countdown {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}

.hint {
  font-size: 11.5px;
  color: var(--text-faint);
}

.actions {
  display: flex;
  gap: 8px;
}

.small {
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-dim);
}

.empty-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-faint);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(4, 8, 12, 0.62);
  backdrop-filter: blur(4px);
}

.modal {
  width: min(430px, 100%);
  padding: 22px 24px;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: var(--bg-elev);
  box-shadow: 0 40px 90px -40px rgba(0, 0, 0, 0.9);
}

.modal h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.modal-hint {
  margin: 0 0 16px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-dim);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--text-dim);
}

.error {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--err);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

@media (max-width: 860px) {
  .page {
    padding: 22px 18px 40px;
  }

  .cards {
    grid-template-columns: 1fr;
  }
}
</style>