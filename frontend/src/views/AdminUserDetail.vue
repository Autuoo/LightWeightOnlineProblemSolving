<template>
  <div class="page">
    <RouterLink to="/admin/users" class="back">← 返回用户管理</RouterLink>

    <div v-if="loading" class="loading">
      <div class="skeleton" style="height: 130px" />
      <div class="skeleton" style="height: 300px; margin-top: 16px" />
    </div>

    <div v-else-if="!user" class="empty-state">
      <p>{{ error || '用户不存在' }}</p>
      <button class="btn btn-ghost" @click="load">重试</button>
    </div>

    <template v-else>
      <header class="hero rise">
        <div class="hero-avatar">
          <label class="avatar-box" :class="{ uploading }">
            <img v-if="user.avatarUrl" :src="avatarSrc" alt="头像" />
            <span v-else class="avatar-fallback">{{ initial }}</span>
            <span class="avatar-mask">{{ uploading ? '上传中' : '更换头像' }}</span>
            <input type="file" accept="image/png,image/jpeg,image/webp" hidden @change="onAvatarChange" />
          </label>
          <button v-if="user.avatarUrl" type="button" class="avatar-remove" :disabled="uploading" @click="removeAvatar">
            移除头像
          </button>
          <span class="avatar-tip">点击头像更换</span>
        </div>
        <div class="hero-main">
          <div class="badge-row">
            <span class="role" :class="{ admin: user.role === 'ADMIN' }">{{ user.role }}</span>
            <span class="status" :class="user.status === 1 ? 'on' : 'off'">
              {{ user.status === 1 ? '启用' : '禁用' }}
            </span>
          </div>
          <h1>{{ user.nickname || user.username }}</h1>
          <p class="sub mono">@{{ user.username }} · ID {{ user.id }} · 注册于 {{ formatTime(user.createdAt) }}</p>
        </div>
      </header>

      <div class="grid">
        <section class="panel rise">
          <div class="panel-head">
            <h2>基本信息</h2>
            <span class="panel-sub">修改后点击保存</span>
          </div>
          <div class="form-body">
            <label class="field">
              <span>用户名（不可修改）</span>
              <input class="input" :value="user.username" disabled />
            </label>
            <label class="field">
              <span>昵称</span>
              <input v-model.trim="form.nickname" class="input" maxlength="32" placeholder="留空则使用用户名" />
            </label>
            <label class="field">
              <span>角色</span>
              <select v-model="form.role" class="input" :disabled="isSelf">
                <option value="STUDENT">STUDENT · 学生</option>
                <option value="ADMIN">ADMIN · 管理员</option>
              </select>
            </label>
            <p v-if="isSelf" class="hint">不能修改当前登录账号的角色</p>
            <p v-if="error" class="error" role="alert">{{ error }}</p>
            <p v-if="notice" class="notice" role="status">{{ notice }}</p>

            <div class="actions">
              <button class="btn btn-primary" :disabled="saving" @click="save">
                <span v-if="saving" class="spinner" />
                {{ saving ? '保存中…' : '保存修改' }}
              </button>
              <RouterLink to="/admin/users" class="btn btn-ghost">返回列表</RouterLink>
            </div>
          </div>
        </section>

        <section class="panel rise" style="animation-delay: 40ms">
          <div class="panel-head">
            <h2>账号操作</h2>
            <span class="panel-sub">谨慎操作</span>
          </div>
          <div class="op-list">
            <div class="op-row">
              <div class="op-text">
                <b>{{ user.status === 1 ? '禁用账号' : '启用账号' }}</b>
                <p>禁用后该账号立即无法登录，已登录的会话也会失效</p>
              </div>
              <button
                class="btn btn-ghost"
                :disabled="busy || user.id === auth.user?.id"
                :title="user.id === auth.user?.id ? '不能禁用当前登录账号' : ''"
                @click="toggleStatus"
              >
                {{ user.status === 1 ? '禁用' : '启用' }}
              </button>
            </div>

            <div class="op-row">
              <div class="op-text">
                <b>重置密码</b>
                <p>重置为系统默认密码，并在此处显示新密码</p>
              </div>
              <button class="btn btn-ghost" :disabled="busy" @click="resetPassword">重置密码</button>
            </div>

            <div class="op-row danger-row">
              <div class="op-text">
                <b>删除账号</b>
                <p>提交记录、比赛报名与 AI 配置会一并删除，且不可恢复</p>
              </div>
              <button
                class="btn btn-ghost danger"
                :disabled="busy || user.id === auth.user?.id"
                :title="user.id === auth.user?.id ? '不能删除当前登录账号' : ''"
                @click="removeUser"
              >
                删除
              </button>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { adminUserApi } from '../api'
import { API_BASE } from '../api/config'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const user = ref(null)
const loading = ref(true)
const saving = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({ nickname: '', role: 'STUDENT' })

const isSelf = computed(() => Boolean(user.value) && user.value.id === auth.user?.id)
const API_ORIGIN = API_BASE.replace(/\/api$/, '')
const avatarSrc = computed(() => (user.value?.avatarUrl ? API_ORIGIN + user.value.avatarUrl : ''))
const initial = computed(() => (user.value?.nickname || user.value?.username || '?').slice(0, 1))
const uploading = ref(false)

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminUserApi.detail(route.params.id)
    user.value = data
    form.nickname = data.nickname || ''
    form.role = data.role
  } catch (err) {
    user.value = null
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!user.value || saving.value) return
  if (form.nickname.length > 32) {
    error.value = '昵称不能超过 32 个字符'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await adminUserApi.update(user.value.id, {
      nickname: form.nickname.trim(),
      role: form.role
    })
    notice.value = '资料已保存'
    await load()
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function toggleStatus() {
  if (!user.value || busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const next = user.value.status === 1 ? 0 : 1
    await adminUserApi.updateStatus(user.value.id, next)
    notice.value = next === 0 ? '账号已禁用' : '账号已启用'
    await load()
  } catch (err) {
    error.value = err.message || '操作失败'
  } finally {
    busy.value = false
  }
}

async function resetPassword() {
  if (!user.value || busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = await adminUserApi.resetPassword(user.value.id)
    notice.value = `密码已重置为 ${data.password}`
  } catch (err) {
    error.value = err.message || '重置失败'
  } finally {
    busy.value = false
  }
}

async function removeUser() {
  if (!user.value || busy.value) return
  const confirmed = window.confirm(
    `确定删除用户「${user.value.username}」吗？\n该用户的提交记录、比赛报名与 AI 配置会一并删除，且不可恢复。`
  )
  if (!confirmed) return
  busy.value = true
  error.value = ''
  try {
    await adminUserApi.remove(user.value.id)
    router.push('/admin/users')
  } catch (err) {
    error.value = err.message || '删除失败'
  } finally {
    busy.value = false
  }
}

async function onAvatarChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file || !user.value) return
  if (file.size > 2 * 1024 * 1024) {
    error.value = '头像不能超过 2MB'
    return
  }
  uploading.value = true
  error.value = ''
  notice.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await adminUserApi.uploadAvatar(user.value.id, formData)
    user.value.avatarUrl = data.avatarUrl
    notice.value = '头像已更新'
  } catch (err) {
    error.value = err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function removeAvatar() {
  if (!user.value || uploading.value) return
  uploading.value = true
  error.value = ''
  notice.value = ''
  try {
    await adminUserApi.removeAvatar(user.value.id)
    user.value.avatarUrl = ''
    notice.value = '头像已移除'
  } catch (err) {
    error.value = err.message || '移除失败'
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 26px 36px 48px;
  max-width: 1120px;
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
  align-items: center;
  gap: 20px;
  padding: 24px 26px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--bg-elev);
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.hero-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.avatar-box {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid var(--line-strong);
  background: linear-gradient(160deg, var(--bg-elev-3), var(--bg-elev));
  display: grid;
  place-items: center;
  cursor: pointer;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 30px;
  font-weight: 650;
  color: var(--text-dim);
}

.avatar-mask {
  position: absolute;
  inset: auto 0 0 0;
  padding: 4px 0;
  text-align: center;
  font-size: 10.5px;
  color: #e8f1f8;
  background: rgba(6, 10, 14, 0.62);
  opacity: 0;
  transition: opacity 0.18s ease;
}

.avatar-box:hover .avatar-mask,
.avatar-box.uploading .avatar-mask {
  opacity: 1;
}

.avatar-remove {
  padding: 0;
  font-size: 11px;
  color: var(--err);
  border: 0;
  background: none;
  cursor: pointer;
}

.avatar-remove:disabled {
  opacity: 0.6;
  cursor: default;
}

.avatar-tip {
  font-size: 11px;
  color: var(--text-faint);
}

.badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.role,
.status {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 10.5px;
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

.hero h1 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 650;
}

.sub {
  margin: 0;
  font-size: 12px;
  color: var(--text-faint);
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 16px;
  align-items: start;
  margin-top: 16px;
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
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
}

.panel-head h2 {
  margin: 0;
  font-size: 14px;
}

.panel-sub {
  font-size: 11px;
  color: var(--text-faint);
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.field .input {
  width: 100%;
  min-width: 0;
  font-size: 12.5px;
}

.input:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}

.hint {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-faint);
}

.error {
  margin: 0;
  font-size: 12px;
  color: var(--err);
}

.notice {
  margin: 0;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--ok);
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.op-list {
  display: flex;
  flex-direction: column;
}

.op-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
}

.op-row:last-child {
  border-bottom: none;
}

.op-text b {
  display: block;
  margin-bottom: 3px;
  font-size: 12.5px;
}

.op-text p {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-faint);
}

.danger-row .op-text b {
  color: var(--err);
}

.danger {
  color: var(--err);
  border-color: rgba(239, 68, 68, 0.35);
}

.danger:hover {
  background: rgba(239, 68, 68, 0.12);
}

.loading {
  padding: 8px 0;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-dim);
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #ffffff66;
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .page {
    padding: 20px 18px 40px;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>