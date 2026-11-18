<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>提交记录</h1>
        <p class="sub">我的全部评测历史</p>
      </div>
      <select v-model="status" class="input select" @change="search">
        <option :value="''">全部状态</option>
        <option value="AC">通过</option>
        <option value="WA">答案错误</option>
        <option value="TLE">超时</option>
        <option value="RE">运行错误</option>
        <option value="CE">编译错误</option>
      </select>
    </header>

    <div class="panel rise" style="animation-delay: 40ms">
      <div v-if="loading" class="loading">
        <div v-for="i in 6" :key="i" class="skeleton row-skeleton" />
      </div>

      <table v-else class="list">
        <thead>
          <tr>
            <th class="c-id">#</th>
            <th>题目</th>
            <th class="c-status">结果</th>
            <th class="c-lang">语言</th>
            <th class="c-score">得分</th>
            <th class="c-time">耗时</th>
            <th class="c-created">提交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in records" :key="item.id" class="row" @click="open(item.id)">
            <td class="c-id mono">{{ item.id }}</td>
            <td class="title-cell">{{ item.problemTitle || `题目 ${item.problemId}` }}</td>
            <td class="c-status"><StatusBadge :status="item.status" /></td>
            <td class="c-lang mono">{{ item.language }}</td>
            <td class="c-score mono">{{ item.score ?? 0 }}</td>
            <td class="c-time mono">{{ item.execTimeMs != null ? item.execTimeMs + ' ms' : '—' }}</td>
            <td class="c-created mono">{{ formatTime(item.createdAt) }}</td>
          </tr>
          <tr v-if="!records.length">
            <td colspan="7" class="empty">还没有提交记录</td>
          </tr>
        </tbody>
      </table>

      <footer v-if="total > size" class="pager">
        <button class="btn btn-ghost" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
        <span class="pager-text mono">{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost" :disabled="page >= Math.ceil(total / size)" @click="go(page + 1)">
          下一页
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '../components/StatusBadge.vue'
import { submissionApi } from '../api'

const router = useRouter()

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const status = ref('')
const loading = ref(true)

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function fetchList() {
  loading.value = true
  try {
    const data = await submissionApi.list({
      page: page.value,
      size: size.value,
      status: status.value || undefined
    })
    records.value = data.records
    total.value = data.total
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchList()
}

function go(next) {
  page.value = next
  fetchList()
}

function open(id) {
  router.push({ name: 'submission-detail', params: { id } })
}

onMounted(fetchList)
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

.select {
  width: 140px;
  height: 36px;
  cursor: pointer;
}

.list {
  width: 100%;
  border-collapse: collapse;
}

.list th {
  text-align: left;
  padding: 12px 16px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.list td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--line);
}

.list tbody tr:last-child td {
  border-bottom: none;
}

.row {
  cursor: pointer;
  transition: background 0.16s ease;
}

.row:hover {
  background: var(--bg-elev-2);
}

.title-cell {
  font-weight: 550;
}

.c-id {
  width: 64px;
  color: var(--text-faint);
  font-size: 12px;
}

.c-status {
  width: 150px;
}

.c-lang {
  width: 96px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.c-score {
  width: 72px;
  font-size: 12.5px;
}

.c-time {
  width: 96px;
  font-size: 12px;
  color: var(--text-dim);
}

.c-created {
  width: 120px;
  font-size: 11.5px;
  color: var(--text-faint);
}

.empty {
  text-align: center;
  color: var(--text-faint);
  padding: 40px 0;
}

.loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row-skeleton {
  height: 34px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.pager-text {
  font-size: 12px;
  color: var(--text-faint);
}
</style>