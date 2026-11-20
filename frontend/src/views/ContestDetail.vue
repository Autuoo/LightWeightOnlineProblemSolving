<template>
  <div class="page">
    <RouterLink to="/contests" class="back">← 返回比赛列表</RouterLink>

    <div v-if="loading" class="loading">
      <div class="skeleton" style="height: 140px" />
      <div class="skeleton" style="height: 260px; margin-top: 16px" />
    </div>

    <div v-else-if="error" class="empty-state">
      <p>{{ error }}</p>
      <button class="btn btn-ghost" @click="load()">重试</button>
    </div>

    <template v-else-if="contest">
      <header class="hero rise">
        <div class="hero-main">
          <div class="phase-row">
            <span class="phase" :class="`p-${contest.phase.toLowerCase()}`">{{ phaseText(contest.phase) }}</span>
            <span class="mode mono">{{ contest.mode }}</span>
          </div>
          <h1>{{ contest.title }}</h1>
          <p class="time mono">{{ formatRange(contest) }}</p>
          <p v-if="contest.description" class="desc">{{ contest.description }}</p>
        </div>
        <div class="hero-side">
          <div class="stat"><b class="mono">{{ contest.participantCount }}</b><span>参赛</span></div>
          <div class="stat"><b class="mono">{{ contest.problemCount }}</b><span>题目</span></div>
          <div v-if="isLive" class="countdown mono">{{ countdownText }}</div>
          <button v-if="contest.canRegister" class="btn btn-primary" @click="showRegister = true">
            {{ contest.phase === 'UPCOMING' ? '报名比赛' : '参加比赛' }}
          </button>
          <span v-else-if="contest.registered" class="joined">已报名</span>
          <span v-else-if="contest.phase === 'ENDED'" class="hint">比赛已结束，题目已公开</span>
        </div>
      </header>

      <div class="toolbar rise" style="animation-delay: 40ms">
        <RouterLink class="btn btn-primary" :to="`/contests/${contest.id}/standings`">查看榜单</RouterLink>
        <RouterLink
          v-if="contest.registered"
          class="btn btn-ghost"
          :to="`/submissions?contestId=${contest.id}`"
        >
          我的比赛提交
        </RouterLink>
        <span v-if="contest.phase === 'UPCOMING'" class="hint">比赛开始后题目才会公开，可先报名</span>
        <span v-else-if="!contest.registered && contest.phase !== 'ENDED'" class="hint">你尚未参加本场比赛，点击「参加比赛」后即可进入题目</span>
        <span v-else-if="contest.phase === 'FROZEN'" class="hint">已封榜，公开榜单不再显示封榜后的提交</span>
      </div>

      <section class="problems rise" style="animation-delay: 80ms">
        <div class="section-head">
          <h2>比赛题目</h2>
          <div class="section-actions">
            <span class="section-sub mono">{{ visibleProblemCount }} 题</span>
            <button class="btn btn-primary" :disabled="startDisabled" @click="startContest">
              {{ startButtonText }}
            </button>
          </div>
        </div>

        <p v-if="startHint" class="start-hint">{{ startHint }}</p>

        <table v-if="contest.problems.length" class="problem-list">
          <thead>
            <tr>
              <th class="c-alias">编号</th>
              <th>题目</th>
              <th class="c-diff">难度</th>
              <th class="c-status">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in contest.problems" :key="item.problemId">
              <td class="c-alias"><span class="alias mono">{{ item.alias }}</span></td>
              <td>
                <RouterLink class="title" :to="`/contests/${contest.id}/problems/${item.problemId}`">
                  {{ item.title }}
                </RouterLink>
              </td>
              <td class="c-diff">
                <span class="diff" :class="`d-${item.difficulty}`">{{ diffLabel(item.difficulty) }}</span>
              </td>
              <td class="c-status">
                <span v-if="item.passed" class="passed">已通过</span>
                <span v-else class="todo">未通过</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty-problems">
          <template v-if="contest.phase === 'UPCOMING'">
            <p>本场比赛共 <b class="mono">{{ contest.problemCount }}</b> 题</p>
            <p class="empty-sub">题目将在比赛开始后公开，届时点击「开始比赛」即可进入作答</p>
          </template>
          <template v-else>
            <p>本场比赛暂未配置题目</p>
            <p class="empty-sub">请联系管理员在后台为比赛添加题目</p>
          </template>
        </div>
      </section>
    </template>

    <div v-if="showRegister" class="modal-overlay" @click.self="closeRegister">
      <section class="modal" role="dialog" aria-modal="true">
        <h3>报名「{{ contest?.title }}」</h3>
        <p class="modal-hint">报名后即可在比赛开始时进入作答。</p>
        <label v-if="contest?.joinType === 'PASSWORD'" class="field">
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { contestApi } from '../api'

const route = useRoute()
const router = useRouter()
const contest = ref(null)
const loading = ref(true)
const error = ref('')
const now = ref(Date.now())
const showRegister = ref(false)
const password = ref('')
const registering = ref(false)
const registerError = ref('')
const startHint = ref('')
let ticker = null
let clock = null
let refreshing = false
let lastRefreshAt = 0

const isLive = computed(() => contest.value && ['RUNNING', 'FROZEN'].includes(contest.value.phase))

const visibleProblemCount = computed(() => {
  if (!contest.value) return 0
  return contest.value.phase === 'UPCOMING' ? contest.value.problemCount : contest.value.problems.length
})

const startButtonText = computed(() => {
  const current = contest.value
  if (!current) return '开始比赛'
  if (current.phase === 'ENDED') return '查看题目'
  if (current.phase !== 'UPCOMING' && !current.registered) return '参加比赛'
  return '开始比赛'
})

const startDisabled = computed(() => {
  const current = contest.value
  if (!current) return true
  return current.phase !== 'UPCOMING' && !current.problems.length
})

const countdownText = computed(() => {
  if (!contest.value) return ''
  const target = new Date(contest.value.phase === 'UPCOMING' ? contest.value.startAt : contest.value.endAt).getTime()
  const diff = Math.max(0, target - now.value)
  const day = Math.floor(diff / 86400000)
  const hour = Math.floor((diff % 86400000) / 3600000)
  const minute = Math.floor((diff % 3600000) / 60000)
  const second = Math.floor((diff % 60000) / 1000)
  const parts = day > 0
    ? `${day} 天 ${hour} 时 ${minute} 分`
    : `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  return `${contest.value.phase === 'UPCOMING' ? '距离开始' : '距离结束'} ${parts}`
})

function phaseText(phase) {
  return { UPCOMING: '即将开始', RUNNING: '进行中', FROZEN: '封榜中', ENDED: '已结束' }[phase] || phase
}

function diffLabel(level) {
  return { 1: '简单', 2: '中等', 3: '困难' }[level] || '未知'
}

function formatRange(item) {
  const format = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    const pad = (n) => String(n).padStart(2, '0')
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${format(item.startAt)} → ${format(item.endAt)}`
}

async function load(silent = false) {
  if (!silent) loading.value = true
  error.value = ''
  try {
    contest.value = await contestApi.detail(route.params.id)
    startHint.value = ''
  } catch (exception) {
    if (!silent) {
      error.value = exception.message
      contest.value = null
    }
  } finally {
    if (!silent) loading.value = false
  }
}

function needsPhaseRefresh() {
  const current = contest.value
  if (!current) return false
  if (current.phase === 'UPCOMING') {
    return now.value >= new Date(current.startAt).getTime()
  }
  if (current.phase === 'RUNNING') {
    if (now.value >= new Date(current.endAt).getTime()) return true
    return current.freezeAt ? now.value >= new Date(current.freezeAt).getTime() : false
  }
  if (current.phase === 'FROZEN') {
    return now.value >= new Date(current.endAt).getTime()
  }
  return false
}

function closeRegister() {
  if (!registering.value) showRegister.value = false
}

function startContest() {
  const current = contest.value
  if (!current) return
  if (current.phase === 'UPCOMING') {
    startHint.value = '比赛尚未开始，倒计时结束后即可进入作答'
    return
  }
  if (!current.problems.length) {
    startHint.value = '本场比赛暂未配置题目'
    return
  }
  if (!current.registered && current.phase !== 'ENDED') {
    startHint.value = ''
    showRegister.value = true
    return
  }
  router.push(`/contests/${current.id}/problems/${current.problems[0].problemId}`)
}

async function register() {
  if (!contest.value || registering.value) return
  registering.value = true
  registerError.value = ''
  try {
    await contestApi.register(contest.value.id, { password: password.value || null })
    showRegister.value = false
    await load()
    if (contest.value && contest.value.problems.length) {
      router.push(`/contests/${contest.value.id}/problems/${contest.value.problems[0].problemId}`)
    }
  } catch (exception) {
    registerError.value = exception.message
  } finally {
    registering.value = false
  }
}

onMounted(() => {
  load()
  clock = setInterval(() => {
    now.value = Date.now()
    if (!refreshing && needsPhaseRefresh() && now.value - lastRefreshAt > 10000) {
      refreshing = true
      lastRefreshAt = now.value
      load(true).finally(() => { refreshing = false })
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (clock) clearInterval(clock)
  if (ticker) clearInterval(ticker)
})
</script>

<style scoped>
.page {
  padding: 26px 36px 48px;
  max-width: 1180px;
}

.back {
  display: inline-block;
  margin-bottom: 14px;
  font-size: 12.5px;
  color: var(--text-dim);
}

.back:hover {
  color: var(--accent);
}

.hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 26px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--bg-elev);
}

.hero-main {
  min-width: 0;
}

.phase-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
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

.mode {
  font-size: 11px;
  color: var(--text-faint);
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 650;
}

.time {
  margin: 0;
  font-size: 12px;
  color: var(--text-faint);
}

.desc {
  margin: 12px 0 0;
  max-width: 720px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text-dim);
  white-space: pre-wrap;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  min-width: 170px;
}

.stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--text-dim);
  font-size: 12px;
}

.stat b {
  color: var(--text);
  font-size: 20px;
}

.countdown {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
  white-space: nowrap;
}

.joined {
  color: var(--ok);
  font-weight: 600;
  font-size: 12px;
}

.hint {
  font-size: 11.5px;
  color: var(--text-faint);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 16px 0;
}

.problems {
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--bg-elev);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 15px;
}

.section-sub {
  font-size: 11px;
  color: var(--text-faint);
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.start-hint {
  margin: 0 0 12px;
  padding: 9px 12px;
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-size: 12.5px;
}

.problem-list {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.problem-list th {
  padding: 9px 10px;
  text-align: left;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
}

.problem-list td {
  padding: 11px 10px;
  border-bottom: 1px solid var(--line);
}

.problem-list tr:last-child td {
  border-bottom: none;
}

.c-alias {
  width: 70px;
}

.c-diff {
  width: 90px;
}

.c-status {
  width: 90px;
  text-align: right;
}

.alias {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: var(--bg-elev-3);
  font-weight: 700;
  color: var(--text);
}

.title {
  color: var(--text);
  font-weight: 550;
}

.title:hover {
  color: var(--accent);
}

.diff {
  font-size: 11.5px;
}

.d-1 {
  color: #0d9488;
}

.d-2 {
  color: #b45309;
}

.d-3 {
  color: #b91c1c;
}

.passed {
  color: var(--ok);
  font-weight: 600;
}

.todo {
  color: var(--text-faint);
}

.empty-problems {
  padding: 34px 12px;
  text-align: center;
  color: var(--text-dim);
  font-size: 13px;
}

.empty-sub {
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--text-faint);
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-dim);
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

@media (max-width: 820px) {
  .page {
    padding: 20px 18px 40px;
  }

  .hero {
    flex-direction: column;
  }

  .hero-side {
    align-items: flex-start;
  }
}
</style>