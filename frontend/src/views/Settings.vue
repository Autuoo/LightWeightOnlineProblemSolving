<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>个人中心</h1>
        <p class="sub">维护个人资料、查看刷题数据，并配置你自己的 AI 模型凭据。</p>
      </div>
    </header>

    <div class="top-grid">
      <section class="panel rise">
        <div class="card-head">
          <h2>个人信息</h2>
          <span v-if="profile.username" class="sub mono">@{{ profile.username }}</span>
        </div>

        <div class="profile-row">
          <label class="avatar-box" :class="{ uploading }">
            <img v-if="profile.avatarUrl" :src="avatarSrc" alt="头像" />
            <span v-else class="avatar-fallback">{{ initial }}</span>
            <span class="avatar-mask">{{ uploading ? '上传中' : '更换' }}</span>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              hidden
              @change="onAvatarChange"
            />
          </label>

          <div class="profile-main">
            <div class="name-row">
              <template v-if="editingNickname">
                <input
                  v-model.trim="nicknameDraft"
                  class="input nickname-input"
                  maxlength="32"
                  @keyup.enter="saveNickname"
                />
                <button class="btn btn-primary mini" :disabled="savingNickname" @click="saveNickname">
                  保存
                </button>
                <button class="btn btn-ghost mini" @click="cancelNickname">取消</button>
              </template>
              <template v-else>
                <span class="nickname">{{ profile.nickname || profile.username }}</span>
                <button class="link" @click="startNickname">修改昵称</button>
              </template>
            </div>

            <div class="meta-line">
              <span class="role-pill" :class="{ admin: profile.role === 'ADMIN' }">
                {{ profile.role === 'ADMIN' ? '管理员' : '学生' }}
              </span>
              <span class="joined">加入于 {{ formatDate(profile.createdAt) }}</span>
            </div>

            <p v-if="profileMessage" class="ok">{{ profileMessage }}</p>
            <p v-if="profileError" class="err">{{ profileError }}</p>
          </div>
        </div>

        <button v-if="profile.avatarUrl" class="link danger" @click="removeAvatar">移除头像</button>
      </section>

      <section class="panel rise">
        <div class="card-head">
          <h2>今日刷题</h2>
          <span class="sub mono">{{ todayLabel }}</span>
        </div>

        <div class="stat-grid">
          <div class="stat">
            <span class="stat-num">{{ stats.today.submissions }}</span>
            <span class="stat-label">提交</span>
          </div>
          <div class="stat">
            <span class="stat-num green">{{ stats.today.accepted }}</span>
            <span class="stat-label">通过</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ stats.today.acceptanceRate }}<i>%</i></span>
            <span class="stat-label">通过率</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ stats.today.solvedProblems }}</span>
            <span class="stat-label">通过题目</span>
          </div>
        </div>

        <div class="chart-head">
          <span>近 7 天提交</span>
          <span class="legend">
            <i class="dot total" />提交
            <i class="dot ac" />通过
          </span>
        </div>

        <div class="chart">
          <div v-for="(point, index) in stats.trend" :key="point.date" class="chart-col">
            <div class="bars">
              <div
                class="bar total"
                :style="{ height: barHeight(point.submissions), transitionDelay: `${index * 35}ms` }"
                :title="`${point.date} 提交 ${point.submissions}`"
              />
              <div
                class="bar ac"
                :style="{ height: barHeight(point.accepted), transitionDelay: `${index * 35 + 60}ms` }"
                :title="`${point.date} 通过 ${point.accepted}`"
              />
            </div>
            <span class="chart-label">{{ shortDate(point.date) }}</span>
          </div>
        </div>
      </section>
    </div>

    <section class="panel rise">
      <div class="card-head">
        <h2>刷题记录</h2>
        <span class="sub mono">共 {{ recordTotal }} 条 · 第 {{ recordPage }} 页</span>
      </div>

      <div class="record-table">
        <div class="record-head">
          <span>题目</span>
          <span>状态</span>
          <span>语言</span>
          <span>耗时</span>
          <span>提交时间</span>
        </div>

        <div v-if="recordLoading" class="record-empty">加载中…</div>
        <div v-else-if="!records.length" class="record-empty">还没有提交记录</div>
        <template v-else>
          <button
            v-for="item in records"
            :key="item.id"
            class="record-row"
            @click="openRecord(item.id)"
          >
            <span class="record-title">{{ item.problemTitle }}</span>
            <span>
              <i class="status" :class="statusClass(item.status)">{{ item.status }}</i>
            </span>
            <span class="mono">{{ item.language }}</span>
            <span class="mono">{{ item.execTimeMs == null ? '—' : item.execTimeMs + 'ms' }}</span>
            <span class="mono time">{{ formatDateTime(item.createdAt) }}</span>
          </button>
        </template>
      </div>

      <div class="pager">
        <button
          class="btn btn-ghost mini"
          :disabled="recordPage <= 1"
          @click="loadRecords(recordPage - 1)"
        >
          上一页
        </button>
        <span class="mono pager-text">{{ recordPage }} / {{ recordPages }}</span>
        <button
          class="btn btn-ghost mini"
          :disabled="recordPage >= recordPages"
          @click="loadRecords(recordPage + 1)"
        >
          下一页
        </button>
      </div>
    </section>

    <div class="bottom-grid">
      <section class="panel rise">
        <div class="card-head">
          <h2>修改密码</h2>
        </div>

        <form class="form" @submit.prevent="submitPassword">
          <label class="field">
            <span class="label">当前密码</span>
            <input
              v-model="passwordForm.oldPassword"
              class="input"
              type="password"
              autocomplete="current-password"
            />
          </label>
          <label class="field">
            <span class="label">新密码</span>
            <input
              v-model="passwordForm.newPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              placeholder="6-32 位"
            />
          </label>
          <label class="field">
            <span class="label">确认新密码</span>
            <input
              v-model="passwordForm.confirmPassword"
              class="input"
              type="password"
              autocomplete="new-password"
            />
          </label>

          <p v-if="passwordMessage" class="ok">{{ passwordMessage }}</p>
          <p v-if="passwordError" class="err">{{ passwordError }}</p>

          <div class="actions">
            <button class="btn btn-primary" type="submit" :disabled="savingPassword">
              {{ savingPassword ? '更新中' : '更新密码' }}
            </button>
          </div>
        </form>
      </section>

      <section class="panel rise">
        <div class="card-head">
          <h2>AI 助教</h2>
          <span class="pill" :class="{ on: aiView.enabled && aiView.hasApiKey }">
            {{ aiStatusText }}
          </span>
        </div>

        <form class="form" @submit.prevent="saveAi">
          <label class="check">
            <input v-model="aiForm.enabled" type="checkbox" />
            <span>启用 AI 助教</span>
          </label>

          <label class="field">
            <span class="label">模型端点（OpenAI 兼容）</span>
            <input
              v-model.trim="aiForm.baseUrl"
              class="input"
              placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1"
            />
            <span class="tip">
              写到 <code>/v1</code> 为止，程序会自动拼接 <code>/chat/completions</code>
            </span>
          </label>

          <label class="field">
            <span class="label">模型名称</span>
            <input v-model.trim="aiForm.model" class="input" placeholder="qwen-plus" />
          </label>

          <label class="field">
            <span class="label">API Key</span>
            <input
              v-model="aiForm.apiKey"
              class="input"
              type="password"
              autocomplete="new-password"
              :placeholder="aiKeyPlaceholder"
            />
            <span class="tip">
              当前：{{ aiView.hasApiKey ? aiView.apiKeyMasked : '未配置' }}。
              Key 加密存储，页面不会显示明文；留空表示不修改。
            </span>
          </label>

          <p v-if="aiError" class="err">{{ aiError }}</p>
          <p v-if="aiNotice" class="ok">{{ aiNotice }}</p>
          <p v-if="aiTest" class="test" :class="aiTest.ok ? 'ok' : 'err'">
            {{
              aiTest.ok
                ? `连接成功：${aiTest.model}，耗时 ${aiTest.elapsedMs}ms`
                : `连接失败：${aiTest.message}`
            }}
          </p>

          <div class="actions">
            <button class="btn btn-primary" type="submit" :disabled="savingAi">
              {{ savingAi ? '保存中' : '保存配置' }}
            </button>
            <button class="btn btn-ghost" type="button" :disabled="testingAi" @click="testAi">
              {{ testingAi ? '测试中' : '测试连接' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiConfigApi, profileApi, statsApi, submissionApi } from '../api'
import { API_BASE } from '../api/config'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const API_ORIGIN = API_BASE.replace(/\/api$/, '')

const profile = reactive({
  id: null,
  username: '',
  nickname: '',
  role: '',
  avatarUrl: '',
  createdAt: null
})

const editingNickname = ref(false)
const nicknameDraft = ref('')
const savingNickname = ref(false)
const uploading = ref(false)
const profileMessage = ref('')
const profileError = ref('')

const stats = reactive({
  today: { submissions: 0, accepted: 0, acceptanceRate: 0, solvedProblems: 0 },
  trend: []
})
const chartReady = ref(false)

const records = ref([])
const recordPage = ref(1)
const recordSize = 10
const recordTotal = ref(0)
const recordLoading = ref(false)

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPassword = ref(false)
const passwordMessage = ref('')
const passwordError = ref('')

const aiView = reactive({ enabled: false, baseUrl: '', model: '', hasApiKey: false, apiKeyMasked: '' })
const aiForm = reactive({ enabled: true, baseUrl: '', model: '', apiKey: '' })
const savingAi = ref(false)
const testingAi = ref(false)
const aiError = ref('')
const aiNotice = ref('')
const aiTest = ref(null)

const initial = computed(() => {
  const name = profile.nickname || profile.username || '?'
  return name.slice(0, 1).toUpperCase()
})

const avatarSrc = computed(() =>
  profile.avatarUrl ? `${API_ORIGIN}${profile.avatarUrl}` : ''
)

const todayLabel = computed(() => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
})

const recordPages = computed(() => Math.max(1, Math.ceil(recordTotal.value / recordSize)))

const chartMax = computed(() => {
  const values = stats.trend.map((point) => point.submissions)
  return Math.max(1, ...values)
})

const aiStatusText = computed(() => {
  if (!aiView.hasApiKey || !aiView.baseUrl) return '未配置'
  return aiView.enabled ? '已启用' : '已停用'
})

const aiKeyPlaceholder = computed(() =>
  aiView.hasApiKey ? '留空则保持不变' : 'sk-...'
)

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}`
}

function shortDate(value) {
  if (!value) return ''
  return value.slice(5)
}

function barHeight(value) {
  if (!chartReady.value || !value) return '0%'
  const percent = (value / chartMax.value) * 100
  return `${Math.max(percent, 8)}%`
}

function statusClass(status) {
  if (status === 'AC') return 'ac'
  if (status === 'PENDING' || status === 'COMPILING' || status === 'RUNNING') return 'pending'
  return 'fail'
}

async function loadProfile() {
  try {
    const data = await profileApi.get()
    Object.assign(profile, data)
  } catch (error) {
    profileError.value = error.message || '加载资料失败'
  }
}

function startNickname() {
  nicknameDraft.value = profile.nickname || ''
  editingNickname.value = true
  profileMessage.value = ''
  profileError.value = ''
}

function cancelNickname() {
  editingNickname.value = false
  nicknameDraft.value = ''
}

async function saveNickname() {
  const nickname = nicknameDraft.value.trim()
  if (!nickname) {
    profileError.value = '昵称不能为空'
    return
  }
  savingNickname.value = true
  profileError.value = ''
  profileMessage.value = ''
  try {
    const data = await profileApi.updateNickname(nickname)
    Object.assign(profile, data)
    await auth.refresh()
    editingNickname.value = false
    profileMessage.value = '昵称已更新'
  } catch (error) {
    profileError.value = error.message || '保存失败'
  } finally {
    savingNickname.value = false
  }
}

async function onAvatarChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    profileError.value = '头像不能超过 2MB'
    return
  }
  uploading.value = true
  profileError.value = ''
  profileMessage.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await profileApi.uploadAvatar(formData)
    profile.avatarUrl = data.avatarUrl
    await auth.refresh()
    profileMessage.value = '头像已更新'
  } catch (error) {
    profileError.value = error.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function removeAvatar() {
  profileError.value = ''
  profileMessage.value = ''
  try {
    await profileApi.removeAvatar()
    profile.avatarUrl = ''
    await auth.refresh()
    profileMessage.value = '头像已移除'
  } catch (error) {
    profileError.value = error.message || '移除失败'
  }
}

async function loadStats() {
  try {
    const data = await statsApi.me()
    stats.today = data.today
    stats.trend = data.trend || []
    requestAnimationFrame(() => {
      chartReady.value = true
    })
  } catch {
    stats.trend = []
  }
}

async function loadRecords(nextPage = recordPage.value) {
  recordLoading.value = true
  try {
    const data = await submissionApi.list({ page: nextPage, size: recordSize })
    records.value = data.records || []
    recordPage.value = Number(data.page || nextPage)
    recordTotal.value = Number(data.total || 0)
  } catch {
    records.value = []
  } finally {
    recordLoading.value = false
  }
}

function openRecord(id) {
  router.push(`/submissions/${id}`)
}

async function submitPassword() {
  passwordMessage.value = ''
  passwordError.value = ''
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordError.value = '请填写当前密码与新密码'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  if (passwordForm.newPassword.length < 6) {
    passwordError.value = '新密码至少 6 位'
    return
  }
  savingPassword.value = true
  try {
    await profileApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordMessage.value = '密码已更新，下次登录请使用新密码'
  } catch (error) {
    passwordError.value = error.message || '修改失败'
  } finally {
    savingPassword.value = false
  }
}

async function loadAi() {
  try {
    const data = await aiConfigApi.get()
    Object.assign(aiView, data)
    aiForm.enabled = Boolean(data.enabled)
    aiForm.baseUrl = data.baseUrl || ''
    aiForm.model = data.model || ''
  } catch (error) {
    aiError.value = error.message || '加载 AI 配置失败'
  }
}

async function saveAi() {
  savingAi.value = true
  aiError.value = ''
  aiNotice.value = ''
  aiTest.value = null
  try {
    const data = await aiConfigApi.save({
      enabled: aiForm.enabled,
      baseUrl: aiForm.baseUrl,
      model: aiForm.model,
      apiKey: aiForm.apiKey ? aiForm.apiKey : null
    })
    Object.assign(aiView, data)
    aiForm.apiKey = ''
    aiNotice.value =
      data.hasApiKey && data.baseUrl
        ? '配置已保存'
        : '已保存，但端点或 Key 不完整，AI 助教仍不可用'
  } catch (error) {
    aiError.value = error.message || '保存失败'
  } finally {
    savingAi.value = false
  }
}

async function testAi() {
  testingAi.value = true
  aiError.value = ''
  aiNotice.value = ''
  aiTest.value = null
  try {
    aiTest.value = await aiConfigApi.test()
  } catch (error) {
    aiTest.value = { ok: false, message: error.message || '测试失败' }
  } finally {
    testingAi.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadStats()
  loadRecords(1)
  loadAi()
})
</script>

<style scoped>
.page {
  padding: 30px 36px 52px;
  max-width: 1180px;
}

.head {
  margin-bottom: 20px;
}

h1 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 640;
  letter-spacing: -0.015em;
}

.sub {
  margin: 0;
  color: var(--text-faint);
  font-size: 12.5px;
}

.top-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 18px;
  margin-bottom: 18px;
  align-items: start;
}

.panel {
  padding: 18px 20px 20px;
}

.panel + .panel {
  margin-top: 0;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-head h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 620;
}

.profile-row {
  display: flex;
  gap: 18px;
  align-items: center;
}

.avatar-box {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid var(--line-strong);
  background: linear-gradient(160deg, var(--bg-elev-3), var(--bg-elev));
  display: grid;
  place-items: center;
  cursor: pointer;
  flex: 0 0 auto;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 26px;
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

.profile-main {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.nickname {
  font-size: 18px;
  font-weight: 640;
  letter-spacing: -0.01em;
}

.nickname-input {
  height: 32px;
  width: 180px;
}

.meta-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--text-faint);
}

.role-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  border: 1px solid var(--line);
  background: var(--bg-elev-2);
}

.role-pill.admin {
  color: var(--accent);
  border-color: rgba(45, 212, 191, 0.3);
  background: rgba(45, 212, 191, 0.08);
}

.link {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 11.5px;
  color: var(--text-dim);
  cursor: pointer;
  transition: color 0.16s ease;
}

.link:hover {
  color: var(--accent);
}

.link.danger {
  margin-top: 14px;
  color: var(--text-faint);
}

.link.danger:hover {
  color: var(--err);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.stat {
  padding: 12px 12px 10px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--bg-sunken);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.stat-num {
  font-size: 22px;
  font-weight: 660;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.stat-num i {
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-faint);
  margin-left: 1px;
}

.stat-num.green {
  color: var(--ok);
}

.stat-label {
  font-size: 10.5px;
  color: var(--text-faint);
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 10px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10.5px;
  color: var(--text-faint);
}

.legend .dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  margin-right: 2px;
}

.legend .dot + .dot {
  margin-left: 8px;
}

.dot.total {
  background: rgba(45, 212, 191, 0.4);
}

.dot.ac {
  background: var(--ok);
}

.chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 122px;
  padding-top: 6px;
}

.chart-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
}

.bars {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
}

.bar {
  width: 9px;
  border-radius: 3px 3px 0 0;
  height: 0;
  transition: height 0.55s cubic-bezier(0.22, 0.68, 0.35, 1);
}

.bar.total {
  background: linear-gradient(180deg, rgba(45, 212, 191, 0.55), rgba(45, 212, 191, 0.18));
}

.bar.ac {
  background: linear-gradient(180deg, #34d399, #10b981);
  box-shadow: 0 0 10px -4px rgba(34, 197, 94, 0.8);
}

.chart-label {
  font-size: 10px;
  color: var(--text-faint);
  font-family: var(--mono);
}

.record-table {
  display: flex;
  flex-direction: column;
}

.record-head,
.record-row {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr 0.8fr 0.8fr 1fr;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  width: 100%;
  text-align: left;
}

.record-head {
  font-size: 10.5px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
}

.record-row {
  border: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: var(--text-dim);
  font-size: 12.5px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.record-row:hover {
  background: rgba(45, 212, 191, 0.04);
}

.record-row:last-child {
  border-bottom: 0;
}

.record-title {
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  font-style: normal;
  border: 1px solid var(--line);
  background: var(--bg-elev-2);
  color: var(--text-faint);
}

.status.ac {
  color: var(--ok);
  border-color: rgba(34, 197, 94, 0.25);
  background: rgba(34, 197, 94, 0.08);
}

.status.fail {
  color: var(--err);
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.08);
}

.status.pending {
  color: #fbbf24;
  border-color: rgba(251, 191, 36, 0.25);
  background: rgba(251, 191, 36, 0.08);
}

.record-empty {
  padding: 30px 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-faint);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
}

.pager-text {
  font-size: 11px;
  color: var(--text-faint);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: block;
}

.label {
  display: block;
  margin-bottom: 6px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.tip {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-faint);
}

.tip code {
  font-family: var(--mono);
  font-size: 10.5px;
  color: var(--text-dim);
}

.check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-dim);
}

.check input {
  accent-color: var(--accent);
}

.actions {
  display: flex;
  gap: 10px;
}

.mini {
  height: 26px;
  padding: 0 10px;
  font-size: 11.5px;
}

.pill {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 10.5px;
  color: var(--text-faint);
  border: 1px solid var(--line);
  background: var(--bg-elev-2);
}

.pill.on {
  color: var(--ok);
  border-color: rgba(34, 197, 94, 0.24);
  background: rgba(34, 197, 94, 0.08);
}

.err {
  margin: 0;
  font-size: 12px;
  color: var(--err);
}

.ok {
  margin: 0;
  font-size: 12px;
  color: var(--ok);
}

.test {
  margin: 0;
  font-size: 12px;
}

@media (max-width: 1000px) {
  .top-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .record-head,
  .record-row {
    grid-template-columns: 1.4fr 0.7fr 0.8fr 1fr;
  }

  .record-head span:nth-child(4),
  .record-row span:nth-child(4) {
    display: none;
  }
}
</style>