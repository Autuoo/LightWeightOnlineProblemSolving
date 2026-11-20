<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>比赛管理</h1>
        <p class="sub">创建比赛、设置题目与时间、发布后学生可在「比赛」中报名</p>
      </div>
      <div class="head-actions">
        <input v-model="keyword" class="input search" placeholder="搜索比赛标题" @keyup.enter="loadList" />
        <button class="btn btn-ghost" :disabled="loading" @click="loadList">刷新</button>
      </div>
    </header>

    <div class="grid">
      <section class="panel list-panel rise">
        <div class="panel-head">
          <h2>比赛列表</h2>
          <span class="count mono">{{ total }} 场</span>
        </div>

        <div v-if="loading" class="loading">
          <div v-for="i in 4" :key="i" class="skeleton row-skeleton" />
        </div>

        <div v-else-if="!rows.length" class="empty">
          <p>还没有比赛</p>
          <p class="empty-sub">在右侧创建第一场比赛</p>
        </div>

        <div v-else class="rows">
          <div class="row head-row">
            <span>比赛</span>
            <span>状态</span>
            <span>时间</span>
            <span>题/人</span>
            <span>操作</span>
          </div>
          <div v-for="contest in rows" :key="contest.id" class="row">
            <span class="title-cell">
              <b>{{ contest.title }}</b>
              <em v-if="!contest.published">草稿</em>
            </span>
            <span>
              <i class="phase-dot" :class="`dot-${contest.phase.toLowerCase()}`" />
              {{ phaseText(contest.phase) }}
            </span>
            <span class="mono time-cell">{{ formatRange(contest) }}</span>
            <span class="mono">{{ contest.problemCount }} / {{ contest.participantCount }}</span>
            <span class="ops">
              <button class="btn btn-ghost mini" @click="edit(contest)">编辑</button>
              <RouterLink class="btn btn-ghost mini" :to="`/contests/${contest.id}/standings?admin=1`">榜单</RouterLink>
              <button class="btn btn-ghost mini danger" :disabled="deletingId === contest.id" @click="remove(contest)">
                {{ deletingId === contest.id ? '删除中' : '删除' }}
              </button>
            </span>
          </div>
        </div>

        <div class="pager">
          <button class="btn btn-ghost mini" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
          <span class="mono">第 {{ page }} 页 · 共 {{ total }} 场</span>
          <button
            class="btn btn-ghost mini"
            :disabled="rows.length < size || loading"
            @click="goPage(page + 1)"
          >
            下一页
          </button>
        </div>
      </section>

      <section class="panel form-panel rise" style="animation-delay: 40ms">
        <div class="panel-head">
          <h2>{{ form.id ? `编辑比赛 #${form.id}` : '新建比赛' }}</h2>
          <button v-if="form.id" class="btn btn-ghost mini" @click="resetForm">新建</button>
        </div>

        <div class="form-body">
          <label class="field">
            <span>比赛标题</span>
            <input v-model="form.title" class="input" placeholder="例如：算法热身赛" />
          </label>

          <label class="field">
            <span>比赛说明（可选）</span>
            <textarea v-model="form.description" class="textarea" rows="3" placeholder="比赛规则、提示等" />
          </label>

          <div class="row2">
            <label class="field">
              <span>赛制</span>
              <select v-model="form.mode" class="input">
                <option value="ICPC">ICPC（通过题数 + 罚时）</option>
                <option value="OI">OI（预留）</option>
              </select>
            </label>
            <label class="field">
              <span>报名方式</span>
              <select v-model="form.joinType" class="input">
                <option value="PUBLIC">公开报名</option>
                <option value="PASSWORD">密码报名</option>
              </select>
            </label>
          </div>

          <label v-if="form.joinType === 'PASSWORD'" class="field">
            <span>报名密码</span>
            <input
              v-model="form.password"
              class="input"
              type="password"
              :placeholder="form.id ? '留空表示不修改密码' : '设置报名密码'"
            />
          </label>

          <div class="row3">
            <label class="field">
              <span>开始时间</span>
              <input v-model="form.startAt" class="input" type="datetime-local" />
            </label>
            <label class="field">
              <span>结束时间</span>
              <input v-model="form.endAt" class="input" type="datetime-local" />
            </label>
            <label class="field freeze-field">
              <span>封榜时间（可选）</span>
              <input v-model="form.freezeAt" class="input" type="datetime-local" />
            </label>
          </div>

          <label class="check">
            <input v-model="form.published" type="checkbox" />
            <span>发布给学生（草稿状态不可见）</span>
          </label>

          <div class="picker">
            <div class="picker-head">
              <span>比赛题目</span>
              <span class="mono">{{ selected.length }} 题 · 按勾选顺序编号 A/B/C</span>
            </div>
            <div class="picker-search">
              <input v-model.trim="problemKeyword" class="input" placeholder="搜索题目名称" />
              <button v-if="problemKeyword" type="button" class="picker-clear" @click="problemKeyword = ''">清空</button>
            </div>
            <div class="picker-list">
              <label v-for="problem in filteredProblems" :key="problem.id" class="pick">
                <input
                  type="checkbox"
                  :checked="selected.includes(problem.id)"
                  @change="toggleProblem(problem.id)"
                />
                <span class="pick-index mono">{{ selected.includes(problem.id) ? selected.indexOf(problem.id) + 1 : '—' }}</span>
                <span class="pick-title">{{ problem.title }}</span>
                <span class="pick-meta mono">难度 {{ problem.difficulty }}</span>
              </label>
              <div v-if="!filteredProblems.length" class="empty-sub">
                {{ problems.length ? '没有匹配的题目' : '题库为空，先去题目管理创建题目' }}
              </div>
            </div>
          </div>

          <p v-if="error" class="error" role="alert">{{ error }}</p>
          <p v-if="ok" class="ok" role="status">{{ ok }}</p>

          <div class="form-actions">
            <button class="btn btn-primary" :disabled="saving" @click="save">
              <span v-if="saving" class="spinner" />
              {{ saving ? '保存中…' : form.id ? '保存修改' : '创建比赛' }}
            </button>
            <button class="btn btn-ghost" :disabled="saving" @click="resetForm">清空</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { adminApi, adminContestApi } from '../api'

const rows = ref([])
const problems = ref([])
const page = ref(1)
const size = 20
const total = ref(0)
const keyword = ref('')
const problemKeyword = ref('')
const loading = ref(true)
const saving = ref(false)
const deletingId = ref(null)
const error = ref('')
const ok = ref('')

const form = reactive({
  id: null,
  title: '',
  description: '',
  mode: 'ICPC',
  startAt: '',
  endAt: '',
  freezeAt: '',
  joinType: 'PUBLIC',
  password: '',
  published: true
})
const selected = ref([])
const filteredProblems = computed(() => {
  const kw = problemKeyword.value.trim().toLowerCase()
  if (!kw) return problems.value
  return problems.value.filter((problem) => (problem.title || '').toLowerCase().includes(kw))
})

function phaseText(phase) {
  return { DRAFT: '草稿', UPCOMING: '即将开始', RUNNING: '进行中', FROZEN: '封榜中', ENDED: '已结束' }[phase] || phase
}

function toLocal(value) {
  return value ? value.slice(0, 16) : ''
}

function toIso(value) {
  if (!value) return null
  return value.length === 16 ? `${value}:00` : value
}

function formatRange(contest) {
  const pad = (n) => String(n).padStart(2, '0')
  if (!contest.startAt || !contest.endAt) return '—'
  const start = new Date(contest.startAt)
  const end = new Date(contest.endAt)
  const startText = `${pad(start.getMonth() + 1)}-${pad(start.getDate())} ${pad(start.getHours())}:${pad(start.getMinutes())}`
  const sameDay = start.toDateString() === end.toDateString()
  const endText = sameDay
    ? `${pad(end.getHours())}:${pad(end.getMinutes())}`
    : `${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}`
  return `${startText} → ${endText}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await adminContestApi.list({
      page: page.value,
      size,
      keyword: keyword.value || undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (exception) {
    error.value = exception.message
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function loadProblems() {
  try {
    const data = await adminApi.listProblems({ page: 1, size: 200 })
    problems.value = data.records || []
  } catch {
    problems.value = []
  }
}

function goPage(next) {
  page.value = next
  loadList()
}

function resetForm() {
  form.id = null
  form.title = ''
  form.description = ''
  form.mode = 'ICPC'
  form.startAt = ''
  form.endAt = ''
  form.freezeAt = ''
  form.joinType = 'PUBLIC'
  form.password = ''
  form.published = true
  selected.value = []
  error.value = ''
  ok.value = ''
}

async function edit(contest) {
  error.value = ''
  ok.value = ''
  try {
    const detail = await adminContestApi.detail(contest.id)
    form.id = detail.id
    form.title = detail.title
    form.description = detail.description || ''
    form.mode = detail.mode
    form.startAt = toLocal(detail.startAt)
    form.endAt = toLocal(detail.endAt)
    form.freezeAt = toLocal(detail.freezeAt)
    form.joinType = detail.joinType
    form.password = ''
    form.published = detail.published
    selected.value = [...(detail.problems || [])]
      .sort((a, b) => (a.orderNo || 0) - (b.orderNo || 0))
      .map((item) => item.problemId)
    document.querySelector('.form-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch (exception) {
    error.value = exception.message
  }
}

function toggleProblem(problemId) {
  const index = selected.value.indexOf(problemId)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(problemId)
  }
}

async function save() {
  if (saving.value) return
  error.value = ''
  ok.value = ''
  if (!form.title.trim()) {
    error.value = '请填写比赛标题'
    return
  }
  if (!form.startAt || !form.endAt) {
    error.value = '请选择开始时间和结束时间'
    return
  }
  if (new Date(form.startAt).getTime() >= new Date(form.endAt).getTime()) {
    error.value = '开始时间必须早于结束时间'
    return
  }
  if (form.joinType === 'PASSWORD' && !form.id && !form.password.trim()) {
    error.value = '密码报名必须设置报名密码'
    return
  }
  if (!selected.value.length) {
    error.value = '请至少选择一道比赛题目'
    return
  }

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description || null,
      mode: form.mode,
      startAt: toIso(form.startAt),
      endAt: toIso(form.endAt),
      freezeAt: toIso(form.freezeAt),
      joinType: form.joinType,
      password: form.password || null,
      published: form.published
    }
    if (form.id) {
      await adminContestApi.update(form.id, payload)
    } else {
      const created = await adminContestApi.create(payload)
      form.id = created.id
    }
    await adminContestApi.assignProblems(form.id, {
      problems: selected.value.map((problemId, index) => ({
        problemId,
        orderNo: index + 1
      }))
    })
    ok.value = '比赛已保存'
    await loadList()
  } catch (exception) {
    error.value = exception.message
  } finally {
    saving.value = false
  }
}

async function remove(contest) {
  const confirmed = window.confirm(
    `确定删除比赛《${contest.title}》吗？\n报名记录与题目映射会删除，比赛内提交会保留为普通提交记录。`
  )
  if (!confirmed) return
  deletingId.value = contest.id
  error.value = ''
  ok.value = ''
  try {
    await adminContestApi.remove(contest.id)
    if (form.id === contest.id) resetForm()
    ok.value = `已删除比赛《${contest.title}》`
    await loadList()
  } catch (exception) {
    error.value = exception.message
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  loadList()
  loadProblems()
})
</script>

<style scoped>
.page {
  padding: 26px 32px 48px;
}

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

h1 {
  margin: 0 0 6px;
  font-size: 25px;
  font-weight: 650;
}

.sub {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-dim);
}

.head-actions {
  display: flex;
  gap: 8px;
}

.search {
  width: 210px;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.panel {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.panel-head h2 {
  margin: 0;
  font-size: 14px;
}

.count {
  font-size: 11px;
  color: var(--text-faint);
}

.rows {
  overflow-x: auto;
}

.row {
  display: grid;
  grid-template-columns: minmax(130px, 1.4fr) 78px 140px 58px 172px;
  gap: 8px;
  align-items: center;
  padding: 9px 12px;
  font-size: 12px;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}

.row:last-child {
  border-bottom: none;
}

.head-row {
  font-size: 10.5px;
  color: var(--text-faint);
  background: var(--bg-sunken);
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.title-cell b {
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 550;
}

.title-cell em {
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 9.5px;
  font-style: normal;
  color: var(--text-faint);
  background: var(--bg-elev-2);
}

.phase-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 5px;
  border-radius: 50%;
  background: var(--text-faint);
}

.dot-running {
  background: #2dd4bf;
}

.dot-upcoming {
  background: #3b82f6;
}

.dot-frozen {
  background: #f59e0b;
}

.time-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-faint);
  font-size: 10.5px;
}

.ops {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.mini {
  height: 27px;
  padding: 0 9px;
  font-size: 11px;
  white-space: nowrap;
}

.danger {
  color: var(--err);
  border-color: rgba(248, 113, 113, 0.35);
}

.danger:hover {
  background: rgba(248, 113, 113, 0.12);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 12px;
  font-size: 10.5px;
  color: var(--text-faint);
}

.form-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.textarea {
  resize: vertical;
  min-height: 66px;
  padding: 9px 11px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: var(--bg-sunken);
  color: var(--text);
  font: inherit;
  font-size: 12px;
  line-height: 1.65;
}

.textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.row3 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.freeze-field {
  grid-column: 1 / -1;
}

.field .input {
  width: 100%;
  min-width: 0;
}

.check {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--text-dim);
}

.picker {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  font-size: 11px;
  color: var(--text-dim);
  background: var(--bg-sunken);
  border-bottom: 1px solid var(--line);
}

.picker-head .mono {
  font-size: 10px;
  color: var(--text-faint);
}

.picker-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.picker-search .input {
  flex: 1;
  min-width: 0;
  height: 32px;
  font-size: 12px;
}

.picker-clear {
  flex-shrink: 0;
  padding: 0;
  font-size: 11px;
  color: var(--accent);
}

.picker-list {
  max-height: 230px;
  overflow-y: auto;
}

.pick {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  font-size: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--line);
}

.pick:last-child {
  border-bottom: none;
}

.pick:hover {
  background: var(--bg-hover);
}

.pick-index {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: var(--bg-elev-2);
  font-size: 9.5px;
  color: var(--text-dim);
}

.pick-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pick-meta {
  font-size: 10px;
  color: var(--text-faint);
}

.error {
  margin: 0;
  font-size: 12px;
  color: var(--err);
}

.ok {
  margin: 0;
  font-size: 12px;
  color: var(--ok);
}

.form-actions {
  display: flex;
  gap: 8px;
}

.empty {
  padding: 34px 16px;
  text-align: center;
  color: var(--text-dim);
  font-size: 13px;
}

.empty-sub {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-faint);
}

.row-skeleton {
  height: 34px;
  margin: 6px 12px;
}

.loading {
  padding: 8px 0;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .page {
    padding: 20px 16px 40px;
  }

  .row {
    grid-template-columns: minmax(130px, 1fr) 78px 64px 160px;
  }

  .row > span:nth-child(3) {
    display: none;
  }

  .row3,
  .row2 {
    grid-template-columns: 1fr;
  }
}
</style>