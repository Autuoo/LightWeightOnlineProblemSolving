<template>
  <div class="page">
    <RouterLink :to="adminView ? '/admin/contests' : `/contests/${route.params.id}`" class="back">
      ← {{ adminView ? '返回比赛管理' : '返回比赛' }}
    </RouterLink>

    <div v-if="loading" class="loading">
      <div class="skeleton" style="height: 60px" />
      <div class="skeleton" style="height: 320px; margin-top: 14px" />
    </div>

    <div v-else-if="error" class="empty-state">
      <p>{{ error }}</p>
      <button class="btn btn-ghost" @click="load()">重试</button>
    </div>

    <template v-else-if="contest && standings">
      <header class="head rise">
        <div>
          <div class="phase-row">
            <span class="phase" :class="`p-${contest.phase.toLowerCase()}`">{{ phaseText(contest.phase) }}</span>
            <span v-if="adminView" class="admin-tag">管理员视图</span>
            <span class="mode mono">{{ contest.mode }}</span>
          </div>
          <h1>{{ contest.title }} · 榜单</h1>
          <p class="sub mono">{{ formatRange(contest) }}</p>
        </div>
        <div class="head-side">
          <span class="updated mono">更新于 {{ formatTime(standings.updatedAt) }}</span>
          <button class="btn btn-ghost" @click="load()">刷新</button>
        </div>
      </header>

      <div v-if="standings.frozen" class="freeze-banner rise" role="status">
        <span class="freeze-icon">❄</span>
        <div>
          <b>已封榜</b>
          <p>公开榜单只统计 {{ formatTime(standings.freezeAt) }} 之前的提交；封榜后的提交会在赛后或管理员视图中显示。</p>
        </div>
      </div>

      <section class="board rise" style="animation-delay: 40ms">
        <div v-if="!standings.items.length" class="empty">
          <p>还没有参赛记录</p>
          <p class="empty-sub">比赛开始后，提交会实时计入榜单</p>
        </div>
        <div v-else class="table-wrap">
          <table class="standings">
            <thead>
              <tr>
                <th class="c-rank">排名</th>
                <th class="c-user">选手</th>
                <th v-if="isOi" class="c-solved">总分</th>
                <template v-else>
                  <th class="c-solved">通过</th>
                  <th class="c-penalty">罚时</th>
                </template>
                <th v-for="alias in aliases" :key="alias" class="c-problem mono">{{ alias }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in standings.items" :key="item.userId">
                <td class="c-rank">
                  <span class="rank-badge mono" :class="rankClass(item.rank)">{{ item.rank }}</span>
                </td>
                <td class="c-user">
                  <div class="user-cell">
                    <span class="avatar">{{ initial(item) }}</span>
                    <div class="user-meta">
                      <div class="name">{{ item.nickname || item.username }}</div>
                      <div class="account mono">{{ item.username }}</div>
                    </div>
                  </div>
                </td>
                <td v-if="isOi" class="c-solved mono">{{ item.score }}</td>
                <template v-else>
                  <td class="c-solved mono">{{ item.solved }}</td>
                  <td class="c-penalty mono">{{ item.penalty }}</td>
                </template>
                <td
                  v-for="alias in aliases"
                  :key="alias"
                  class="c-problem mono"
                  :class="cellClass(item.problems[alias])"
                >
                  {{ item.problems[alias] || '·' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <p v-if="isOi" class="footnote">
        OI 赛制：每题取最高得分，总分 = 各题得分之和；总分相同比通过题数，再比最后一次有效提交时间。
        <span v-if="standings.frozen">封榜期间数据可能滞后。</span>
      </p>
      <p v-else class="footnote">
        罚时 = 首次通过时间（分钟）+ 20 × 首次通过前的非 AC 提交数；编译错误不计罚时。
        <span v-if="standings.frozen">封榜期间数据可能滞后。</span>
      </p>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { adminContestApi, contestApi } from '../api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const contest = ref(null)
const standings = ref(null)
const loading = ref(true)
const error = ref('')
let polling = null

const adminView = computed(() => route.query.admin === '1' && auth.isAdmin)
const aliases = computed(() => (contest.value?.problems || []).map((item) => item.alias))
const isOi = computed(() => contest.value?.mode === 'OI')

function phaseText(phase) {
  return { UPCOMING: '即将开始', RUNNING: '进行中', FROZEN: '封榜中', ENDED: '已结束' }[phase] || phase
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

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function initial(item) {
  const name = item.nickname || item.username || '?'
  return name.slice(0, 1).toUpperCase()
}

function rankClass(rank) {
  return rank <= 3 ? `rank-${rank}` : ''
}

function cellClass(value) {
  if (value === undefined || value === null || value === '') return 'cell-empty'
  if (isOi.value) {
    return Number(value) > 0 ? 'cell-ac' : 'cell-fail'
  }
  return value.startsWith('+') ? 'cell-ac' : 'cell-fail'
}

async function load(silent = false) {
  if (!silent) loading.value = true
  error.value = ''
  try {
    if (adminView.value) {
      const [contestData, standingsData] = await Promise.all([
        adminContestApi.detail(route.params.id),
        adminContestApi.standings(route.params.id)
      ])
      contest.value = contestData
      standings.value = standingsData
    } else {
      const [contestData, standingsData] = await Promise.all([
        contestApi.detail(route.params.id),
        contestApi.standings(route.params.id)
      ])
      contest.value = contestData
      standings.value = standingsData
    }
  } catch (exception) {
    if (!silent) {
      error.value = exception.message
      contest.value = null
      standings.value = null
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  polling = setInterval(() => {
    if (contest.value && ['RUNNING', 'FROZEN'].includes(contest.value.phase)) {
      load(true)
    }
  }, 10000)
})

onBeforeUnmount(() => {
  if (polling) clearInterval(polling)
})
</script>

<style scoped>
.page {
  padding: 26px 36px 48px;
  max-width: 1240px;
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

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.phase-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
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

.admin-tag {
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 10.5px;
  color: var(--accent);
  background: rgba(45, 212, 191, 0.12);
}

.mode {
  font-size: 11px;
  color: var(--text-faint);
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 650;
}

.sub {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-faint);
}

.head-side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.updated {
  font-size: 10.5px;
  color: var(--text-faint);
}

.freeze-banner {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
  padding: 12px 16px;
  border: 1px solid rgba(180, 83, 9, 0.35);
  border-radius: 10px;
  background: rgba(245, 158, 11, 0.1);
  color: #92400e;
}

.freeze-icon {
  font-size: 20px;
  line-height: 1;
  color: #0ea5e9;
}

.freeze-banner b {
  color: #713f12;
}

.freeze-banner p {
  margin: 3px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: #8a5a12;
}

.board {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

.standings {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.standings th {
  padding: 11px 10px;
  text-align: left;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-faint);
  background: var(--bg-sunken);
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}

.standings td {
  padding: 10px;
  border-bottom: 1px solid var(--line);
}

.standings tr:last-child td {
  border-bottom: none;
}

.c-rank {
  width: 70px;
}

.c-solved {
  width: 70px;
  text-align: center;
}

.c-penalty {
  width: 80px;
  text-align: right;
}

.c-problem {
  width: 52px;
  text-align: center;
  font-weight: 600;
}

.cell-ac {
  color: var(--ok);
}

.cell-fail {
  color: var(--err);
}

.cell-empty {
  color: var(--text-faint);
}

.rank-badge {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: var(--bg-elev-2);
  font-size: 11px;
  color: var(--text-dim);
}

.rank-1 {
  background: #fbbf24;
  color: #1a1305;
}

.rank-2 {
  background: #cbd5e1;
  color: #1f2937;
}

.rank-3 {
  background: #d97706;
  color: #fff;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--bg-elev-3);
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}

.user-meta .name {
  font-size: 12.5px;
  color: var(--text);
}

.user-meta .account {
  font-size: 10.5px;
  color: var(--text-faint);
}

.empty {
  padding: 52px 20px;
  text-align: center;
  color: var(--text-dim);
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

.footnote {
  margin: 12px 2px 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-faint);
}

@media (max-width: 860px) {
  .page {
    padding: 20px 18px 40px;
  }

  .head {
    align-items: flex-start;
  }
}
</style>