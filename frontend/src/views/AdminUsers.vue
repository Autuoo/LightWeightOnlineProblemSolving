<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>用户管理</h1>
        <p class="sub">查看账号状态、禁用登录、重置密码。禁用后该账号已登录的会话会立即失效。</p>
      </div>
      <div class="head-actions">
        <input
          v-model.trim="keyword"
          class="input search"
          placeholder="搜索用户名或昵称"
          @keyup.enter="load(1)"
        />
        <button class="btn" :disabled="loading" @click="load(1)">搜索</button>
        <button class="btn btn-primary" @click="toggleCreate">
          {{ showCreate ? '收起' : '新建用户' }}
        </button>
      </div>
    </header>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="error" class="err">{{ error }}</p>

    <section class="panel rise">
      <div v-if="showCreate" class="create-row">
        <input
          v-model.trim="form.username"
          class="input"
          placeholder="用户名（3-32 位字母/数字/下划线）"
        />
        <input v-model.trim="form.nickname" class="input" placeholder="昵称（可选）" />
        <input
          v-model="form.password"
          class="input"
          type="password"
          placeholder="密码（留空用默认密码）"
        />
        <button class="btn btn-primary" :disabled="creating" @click="createUser">
          {{ creating ? '创建中' : '创建' }}
        </button>
        <button class="btn btn-ghost" :disabled="creating" @click="toggleCreate">取消</button>
      </div>

      <div v-if="aiView" class="ai-view">
        <div class="ai-view-main">
          <span class="ai-view-title">{{ aiView.username }} 的 AI 配置</span>
          <span v-if="!aiView.data.hasApiKey" class="ai-view-text">未配置</span>
          <span v-else class="ai-view-text">
            {{ aiView.data.enabled ? '已启用' : '已停用' }} · 端点 {{ aiView.data.baseUrl }} ·
            模型 {{ aiView.data.model }} · Key {{ aiView.data.apiKeyMasked }}
          </span>
        </div>
        <div class="ai-view-ops">
          <button
            v-if="aiView.data.hasApiKey"
            class="btn op confirm"
            :disabled="busy === aiView.id"
            @click="clearAiConfig"
          >
            清空配置
          </button>
          <button class="btn btn-ghost op" @click="aiView = null">关闭</button>
        </div>
      </div>


      <div class="table-head">
        <span>用户名</span>
        <span>昵称</span>
        <span>角色</span>
        <span>状态</span>
        <span>创建时间</span>
        <span class="right">操作</span>
      </div>

      <div v-if="loading" class="state">加载中…</div>
      <div v-else-if="!users.length" class="state">没有匹配的用户</div>

      <div v-for="user in users" v-else :key="user.id" class="table-row">
        <span class="mono username">{{ user.username }}</span>
        <span class="nick">{{ user.nickname || '—' }}</span>
        <span>
          <i class="role" :class="{ admin: user.role === 'ADMIN' }">{{ user.role }}</i>
        </span>
        <span>
          <i class="status" :class="user.status === 1 ? 'on' : 'off'">
            {{ user.status === 1 ? '启用' : '禁用' }}
          </i>
        </span>
        <span class="mono time">{{ formatTime(user.createdAt) }}</span>
        <span class="ops">
          <button
            class="btn btn-ghost op"
            :disabled="busy === user.id || user.id === auth.user?.id"
            :title="user.id === auth.user?.id ? '不能禁用当前登录账号' : ''"
            @click="toggleStatus(user)"
          >
            {{ user.status === 1 ? '禁用' : '启用' }}
          </button>

          <button
            v-if="resetTarget !== user.id"
            class="btn btn-ghost op"
            :disabled="busy === user.id"
            @click="resetTarget = user.id"
          >
            重置密码
          </button>
          <template v-else>
            <button class="btn op confirm" :disabled="busy === user.id" @click="confirmReset(user)">
              确认重置
            </button>
            <button class="btn btn-ghost op" @click="resetTarget = null">取消</button>
          </template>

          <button class="btn btn-ghost op" :disabled="busy === user.id" @click="viewAiConfig(user)">
            AI 配置
          </button>
          <RouterLink class="btn btn-ghost op" :to="`/admin/users/${user.id}`">
            编辑
          </RouterLink>
          <button
            class="btn btn-ghost op danger"
            :disabled="busy === user.id || user.id === auth.user?.id"
            :title="user.id === auth.user?.id ? '不能删除当前登录账号' : ''"
            @click="removeUser(user)"
          >
            删除
          </button>
        </span>
      </div>

      <div class="pager">
        <button class="btn btn-ghost" :disabled="loading || page <= 1" @click="load(page - 1)">
          上一页
        </button>
        <span class="mono pager-info">第 {{ page }} 页 · 共 {{ total }} 个账号</span>
        <button
          class="btn btn-ghost"
          :disabled="loading || page * size >= total"
          @click="load(page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { adminUserApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const size = 20
const users = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const busy = ref(null)
const error = ref('')
const notice = ref('')
const resetTarget = ref(null)
const showCreate = ref(false)
const creating = ref(false)
const form = ref({ username: '', nickname: '', password: '' })
const aiView = ref(null)


function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
}

async function load(nextPage = page.value) {
  loading.value = true
  error.value = ''

  resetTarget.value = null
  try {
    const data = await adminUserApi.list({ page: nextPage, size, keyword: keyword.value })
    users.value = data.records || []
    page.value = Number(data.page || nextPage)
    total.value = Number(data.total || 0)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(user) {
  busy.value = user.id
  error.value = ''
  notice.value = ''
  try {
    const next = user.status === 1 ? 0 : 1
    await adminUserApi.updateStatus(user.id, next)
    notice.value = `${user.username} 已${next === 0 ? '禁用' : '启用'}`
    await load()
  } catch (err) {
    error.value = err.message || '操作失败'
  } finally {
    busy.value = null
  }
}

async function confirmReset(user) {
  busy.value = user.id
  error.value = ''
  try {
    const data = await adminUserApi.resetPassword(user.id)
    notice.value = `${user.username} 的密码已重置为 ${data.password}`
    resetTarget.value = null
  } catch (err) {
    error.value = err.message || '重置失败'
  } finally {
    busy.value = null
  }
}

function toggleCreate() {
  showCreate.value = !showCreate.value
  if (!showCreate.value) {
    form.value = { username: '', nickname: '', password: '' }
  }
  error.value = ''
}

async function createUser() {
  const username = form.value.username.trim()
  if (!username) {
    error.value = '请输入用户名'
    return
  }
  creating.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = await adminUserApi.create({
      username,
      nickname: form.value.nickname.trim() || null,
      password: form.value.password || null
    })
    notice.value = `已创建 ${data.username}，初始密码 ${data.password}`
    showCreate.value = false
    form.value = { username: '', nickname: '', password: '' }
    await load(1)
  } catch (err) {
    error.value = err.message || '创建失败'
  } finally {
    creating.value = false
  }
}


async function removeUser(user) {
  if (user.id === auth.user?.id) return
  const confirmed = window.confirm(
    `确定删除用户「${user.username}」吗？\n该用户的提交记录、比赛报名与 AI 配置会一并删除，且不可恢复。`
  )
  if (!confirmed) return
  busy.value = user.id
  error.value = ''
  notice.value = ''
  try {
    await adminUserApi.remove(user.id)
    notice.value = `已删除用户 ${user.username}`

    if (aiView.value?.id === user.id) aiView.value = null
    await load()
  } catch (err) {
    error.value = err.message || '删除失败'
  } finally {
    busy.value = null
  }
}

async function viewAiConfig(user) {
  error.value = ''
  notice.value = ''
  try {
    const data = await adminUserApi.aiConfig(user.id)
    aiView.value = { id: user.id, username: user.username, data }
  } catch (err) {
    error.value = err.message || '读取 AI 配置失败'
  }
}

async function clearAiConfig() {
  if (!aiView.value) return
  busy.value = aiView.value.id
  error.value = ''
  try {
    await adminUserApi.clearAiConfig(aiView.value.id)
    aiView.value.data = {
      ...aiView.value.data,
      enabled: false,
      baseUrl: '',
      hasApiKey: false,
      apiKeyMasked: ''
    }
    notice.value = `${aiView.value.username} 的 AI 配置已清空`
  } catch (err) {
    error.value = err.message || '清空失败'
  } finally {
    busy.value = null
  }
}

onMounted(() => load(1))
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
  gap: 24px;
  margin-bottom: 18px;
}

h1 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 630;
  letter-spacing: -0.015em;
}

.sub {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-faint);
}

.head-actions {
  display: flex;
  gap: 8px;
}

.search {
  width: 220px;
}

.notice {
  margin: 0 0 12px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--ok);
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.err {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--err);
}

.panel {
  padding: 0;
  overflow-x: auto;
}

.create-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1.4fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
  background: rgba(45, 212, 191, 0.03);
}

.ai-view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--line);
  background: rgba(45, 212, 191, 0.04);
  font-size: 12px;
}

.ai-view-main {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.ai-view-title {
  font-weight: 600;
  color: var(--text-dim);
}

.ai-view-text {
  color: var(--text-faint);
  overflow-wrap: anywhere;
}

.ai-view-ops {
  display: flex;
  gap: 6px;
}


.danger {
  color: var(--err);
  border-color: rgba(239, 68, 68, 0.35);
}

.danger:hover {
  background: rgba(239, 68, 68, 0.12);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 1fr 0.9fr 0.55fr 0.55fr 1fr 2.9fr;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  min-width: 980px;
}

.table-head {
  font-size: 11px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.table-row {
  font-size: 12.5px;
  border-bottom: 1px solid var(--line);
  transition: background 0.16s ease;
}

.table-row:last-of-type {
  border-bottom: 0;
}

.table-row:hover {
  background: rgba(255, 255, 255, 0.018);
}

.username {
  color: var(--text);
}

.nick {
  color: var(--text-dim);
}

.role,
.status {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  font-style: normal;
  border: 1px solid var(--line);
  color: var(--text-faint);
  background: var(--bg-elev-2);
}

.role.admin {
  color: var(--accent);
  border-color: rgba(45, 212, 191, 0.28);
  background: rgba(45, 212, 191, 0.08);
}

.status.on {
  color: var(--ok);
  border-color: rgba(34, 197, 94, 0.24);
  background: rgba(34, 197, 94, 0.08);
}

.status.off {
  color: var(--err);
  border-color: rgba(239, 68, 68, 0.24);
  background: rgba(239, 68, 68, 0.08);
}

.time {
  font-size: 11.5px;
  color: var(--text-faint);
}

.ops {
  display: flex;
  flex-wrap: nowrap;
  justify-content: flex-start;
  gap: 5px;
}

.op {
  height: 26px;
  padding: 0 7px;
  font-size: 11px;
  white-space: nowrap;
}

.confirm {
  border-color: rgba(239, 68, 68, 0.35);
  color: var(--err);
  background: rgba(239, 68, 68, 0.08);
}

.state {
  padding: 38px 18px;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-faint);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--line);
}

.pager-info {
  font-size: 11.5px;
  color: var(--text-faint);
}

.right {
  text-align: left;
}

@media (max-width: 900px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-head,
  .table-row {
    grid-template-columns: 1.1fr 1fr 0.7fr 0.7fr 1.4fr;
  }

  .time {
    display: none;
  }
}
</style>