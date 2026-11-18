<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>排行榜</h1>
        <p class="sub">按通过题目数排序，同分时提交更少者优先</p>
      </div>
      <div class="summary">
        <div class="sum-item">
          <b class="mono">{{ rows.length }}</b>
          <span>活跃用户</span>
        </div>
        <div class="sum-item">
          <b class="mono">{{ totalSubmissions }}</b>
          <span>提交总数</span>
        </div>
        <div class="sum-item">
          <b class="mono">{{ totalAccepted }}</b>
          <span>AC 提交</span>
        </div>
      </div>
    </header>

    <div class="board rise" style="animation-delay: 40ms">
      <div v-if="loading" class="loading">
        <div v-for="i in 6" :key="i" class="skeleton row-skeleton" />
      </div>

      <template v-else-if="rows.length">
        <div class="podium">
          <div
            v-for="item in podium"
            :key="item.userId"
            class="podium-card"
            :class="`p-${item.rank}`"
          >
            <span class="crown" v-if="item.rank === 1">TOP</span>
            <span class="medal mono">{{ item.rank }}</span>
            <div class="podium-name">{{ item.nickname || item.username }}</div>
            <div class="podium-user mono">{{ item.username }}</div>
            <div class="podium-score">
              <b class="mono">{{ item.solvedCount }}</b>
              <span>题</span>
            </div>
            <div class="podium-rate mono">{{ percent(item.acceptedRate) }} 通过率</div>
          </div>
        </div>

        <table class="list">
          <thead>
            <tr>
              <th class="c-rank">排名</th>
              <th>用户</th>
              <th class="c-solved">通过题目</th>
              <th class="c-submit">提交次数</th>
              <th class="c-rate">提交通过率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.userId" class="row" :class="`r-${item.rank}`">
              <td class="c-rank">
                <span class="rank-badge mono" :class="`b-${item.rank}`">{{ item.rank }}</span>
              </td>
              <td>
                <div class="user-cell">
                  <span class="avatar">{{ initial(item) }}</span>
                  <div class="user-meta">
                    <div class="name">{{ item.nickname || item.username }}</div>
                    <div class="account mono">{{ item.username }}</div>
                  </div>
                </div>
              </td>
              <td class="c-solved mono">{{ item.solvedCount }}</td>
              <td class="c-submit mono">{{ item.submissionCount }}</td>
              <td class="c-rate">
                <div class="rate-cell">
                  <div class="bar">
                    <div class="bar-fill" :style="{ width: percent(item.acceptedRate) }" />
                  </div>
                  <span class="rate-text mono">{{ percent(item.acceptedRate) }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <div v-else class="empty">还没有提交数据，先去做一道题吧</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { leaderboardApi } from '../api'

const rows = ref([])
const loading = ref(true)

const totalSubmissions = computed(() => rows.value.reduce((sum, item) => sum + item.submissionCount, 0))
const totalAccepted = computed(() => rows.value.reduce((sum, item) => sum + item.acceptedCount, 0))

const podium = computed(() => {
  const top = rows.value.slice(0, 3)
  const order = [top[1], top[0], top[2]].filter(Boolean)
  return order
})

function percent(rate) {
  return `${Math.round((rate || 0) * 1000) / 10}%`
}

function initial(item) {
  const name = item.nickname || item.username || '?'
  return name.slice(0, 1).toUpperCase()
}

async function load() {
  loading.value = true
  try {
    rows.value = await leaderboardApi.list(50)
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 34px 40px 48px;
  max-width: 1180px;
}

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 640;
  letter-spacing: -0.015em;
}

.sub {
  margin: 0;
  color: var(--text-faint);
  font-size: 12.5px;
}

.summary {
  display: flex;
  gap: 28px;
}

.sum-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.sum-item b {
  font-size: 18px;
  font-weight: 620;
  letter-spacing: -0.02em;
}

.sum-item span {
  font-size: 11px;
  color: var(--text-faint);
}

.board {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
  overflow: hidden;
}

.podium {
  display: grid;
  grid-template-columns: 1fr 1.15fr 1fr;
  gap: 12px;
  align-items: end;
  padding: 26px 24px 22px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.podium-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--bg-elev);
  transition: transform 0.25s ease, border-color 0.25s ease;
}

.podium-card:hover {
  transform: translateY(-3px);
}

.p-1 {
  border-color: rgba(245, 158, 11, 0.42);
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.09) 0%, var(--bg-elev) 70%);
  padding-top: 26px;
}

.p-2 {
  border-color: rgba(148, 163, 184, 0.32);
}

.p-3 {
  border-color: rgba(180, 120, 70, 0.3);
}

.crown {
  position: absolute;
  top: -9px;
  padding: 2px 8px;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #2a1b00;
  background: var(--warn);
}

.medal {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
  border: 1px solid var(--line-strong);
  color: var(--text-dim);
}

.p-1 .medal {
  background: rgba(245, 158, 11, 0.16);
  border-color: rgba(245, 158, 11, 0.45);
  color: var(--warn);
}

.p-2 .medal {
  background: rgba(148, 163, 184, 0.14);
  border-color: rgba(148, 163, 184, 0.4);
  color: #b6c2d2;
}

.p-3 .medal {
  background: rgba(180, 120, 70, 0.14);
  border-color: rgba(180, 120, 70, 0.4);
  color: #c88a5a;
}

.podium-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.podium-user {
  font-size: 10.5px;
  color: var(--text-faint);
  margin-bottom: 12px;
}

.podium-score {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.podium-score b {
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.03em;
  line-height: 1;
}

.podium-score span {
  font-size: 11px;
  color: var(--text-faint);
}

.podium-rate {
  margin-top: 6px;
  font-size: 10.5px;
  color: var(--text-faint);
}

.list {
  width: 100%;
  border-collapse: collapse;
}

.list th {
  text-align: left;
  padding: 11px 18px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.list td {
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
}

.list tbody tr:last-child td {
  border-bottom: none;
}

.row {
  transition: background 0.16s ease;
}

.row:hover {
  background: var(--bg-elev-2);
}

.r-1 {
  background: rgba(245, 158, 11, 0.05);
}

.r-2 {
  background: rgba(148, 163, 184, 0.045);
}

.r-3 {
  background: rgba(180, 120, 70, 0.045);
}

.c-rank {
  width: 82px;
}

.rank-badge {
  display: inline-grid;
  place-items: center;
  min-width: 24px;
  height: 22px;
  padding: 0 5px;
  border-radius: 6px;
  font-size: 11.5px;
  color: var(--text-faint);
  background: var(--bg-elev-2);
}

.b-1 {
  background: rgba(245, 158, 11, 0.16);
  color: var(--warn);
}

.b-2 {
  background: rgba(148, 163, 184, 0.14);
  color: #b6c2d2;
}

.b-3 {
  background: rgba(180, 120, 70, 0.14);
  color: #c88a5a;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--bg-elev-3);
  border: 1px solid var(--line-strong);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-dim);
}

.name {
  font-size: 13px;
  font-weight: 550;
}

.account {
  font-size: 10.5px;
  color: var(--text-faint);
}

.c-solved,
.c-submit {
  width: 108px;
  font-size: 13px;
}

.c-rate {
  width: 210px;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: var(--bg-elev-3);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--accent) 0%, var(--ok) 100%);
  transition: width 0.7s cubic-bezier(0.22, 0.68, 0.35, 1);
}

.rate-text {
  font-size: 11.5px;
  color: var(--text-dim);
  min-width: 42px;
  text-align: right;
}

.empty {
  padding: 70px 0;
  text-align: center;
  color: var(--text-faint);
  font-size: 13px;
}

.loading {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row-skeleton {
  height: 42px;
}
</style>