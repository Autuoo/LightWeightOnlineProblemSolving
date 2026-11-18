<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>题库</h1>
        <p class="sub">共 {{ total }} 道题，按创建时间倒序</p>
      </div>
      <div class="filters">
        <input
          v-model.trim="keyword"
          class="input search"
          placeholder="搜索题目标题"
          @keyup.enter="search"
        />
        <select v-model="difficulty" class="input select" @change="search">
          <option :value="null">全部难度</option>
          <option :value="1">简单</option>
          <option :value="2">中等</option>
          <option :value="3">困难</option>
        </select>
      </div>
    </header>

    <div class="panel rise" style="animation-delay: 40ms">
      <div v-if="loading" class="loading">
        <div v-for="i in 5" :key="i" class="skeleton row-skeleton" />
      </div>

      <table v-else class="list">
        <thead>
          <tr>
            <th class="c-id">#</th>
            <th>题目</th>
            <th class="c-diff">难度</th>
            <th class="c-limit">限制</th>
            <th class="c-rate">通过率</th>
            <th class="c-pass">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in records" :key="item.id" class="row" @click="open(item.id)">
            <td class="c-id mono">{{ item.id }}</td>
            <td>
              <div class="title">{{ item.title }}</div>
            </td>
            <td class="c-diff">
              <span class="diff" :class="`d-${item.difficulty}`">
                <i class="diff-dot" />
                {{ diffLabel(item.difficulty) }}
              </span>
            </td>
            <td class="c-limit mono">{{ item.timeLimitMs }}ms / {{ item.memoryLimitMb }}MB</td>
            <td class="c-rate">
              <div class="rate" :title="`${item.acceptedCount} / ${item.submitCount} 次提交通过`">
                <div class="rate-bar">
                  <div class="rate-fill" :style="{ width: `${(item.acceptedRate * 100).toFixed(1)}%` }" />
                </div>
                <span class="rate-num mono">{{ (item.acceptedRate * 100).toFixed(0) }}%</span>
              </div>
            </td>
            <td class="c-pass">
              <span v-if="item.passed" class="tag tag-ok">已通过</span>
              <span v-else class="tag tag-mute">未通过</span>
            </td>
          </tr>
          <tr v-if="!records.length">
            <td colspan="6" class="empty">没有符合条件的题目</td>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { problemApi } from '../api'

const router = useRouter()

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const difficulty = ref(null)
const loading = ref(true)

function diffLabel(level) {
  return { 1: '简单', 2: '中等', 3: '困难' }[level] || '未知'
}

async function fetchList() {
  loading.value = true
  try {
    const data = await problemApi.list({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      difficulty: difficulty.value ?? undefined
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
  router.push({ name: 'problem-detail', params: { id } })
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

.filters {
  display: flex;
  gap: 10px;
}

.search {
  width: 230px;
  height: 36px;
}

.select {
  width: 130px;
  height: 36px;
  cursor: pointer;
}

.list {
  width: 100%;
  border-collapse: collapse;
}

.list th {
  text-align: left;
  padding: 12px 18px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.list td {
  padding: 14px 18px;
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

.row:hover .title {
  color: var(--accent);
}

.title {
  font-weight: 550;
  transition: color 0.16s ease;
}

.c-id {
  width: 64px;
  color: var(--text-faint);
  font-size: 12px;
}

.c-diff {
  width: 110px;
}

.c-limit {
  width: 150px;
  font-size: 12px;
  color: var(--text-dim);
}

.c-rate {
  width: 132px;
}

.rate {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: var(--bg-elev-3);
  overflow: hidden;
}

.rate-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--accent) 0%, var(--ok) 100%);
  transition: width 0.6s cubic-bezier(0.22, 0.68, 0.35, 1);
}

.rate-num {
  font-size: 11px;
  color: var(--text-faint);
  min-width: 30px;
  text-align: right;
}

.c-pass {
  width: 100px;
}

.diff {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-dim);
}

.diff-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.d-1 {
  color: var(--ok);
}

.d-2 {
  color: var(--warn);
}

.d-3 {
  color: var(--err);
}

.empty {
  text-align: center;
  color: var(--text-faint);
  padding: 40px 0;
}

.loading {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row-skeleton {
  height: 36px;
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