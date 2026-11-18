<template>
  <div v-if="visible" class="hint">
    <div class="hint-head">
      <div class="hint-title">
        <span class="spark" />
        <span>AI 助教</span>
        <span class="level-pill">{{ levelMeta[current.level]?.label || '思路' }}</span>
      </div>
      <span class="hint-note">{{ enabled ? '只给方向，不给答案' : '评测完成后可用' }}</span>
    </div>

    <div class="hint-body">
      <p v-if="!messages.length && !current.question" class="placeholder">
        它会读取题面、你的代码和<strong>脱敏后的</strong>失败摘要，提示可能的出错方向，不会返回测试点数据。
      </p>

      <article v-for="(item, index) in messages" :key="item.id || `history-${index}`" class="qa">
        <div class="q">
          <span class="mark">问</span>
          <span class="q-text">{{ item.question }}</span>
        </div>
        <div class="a markdown" v-html="render(item.answer)" />
        <div class="meta">
          <span class="meta-level">{{ levelMeta[item.level]?.label || '思路' }}</span>
          <span v-if="item.reused" class="meta-reused">复用已有回答</span>
          <span v-if="item.createdAt" class="meta-time mono">{{ formatTime(item.createdAt) }}</span>
        </div>
      </article>

      <article v-if="current.question" class="qa live">
        <div class="q">
          <span class="mark">问</span>
          <span class="q-text">{{ current.question }}</span>
        </div>
        <div v-if="current.answer" class="a markdown" v-html="render(current.answer)" />
        <div v-else-if="loading" class="thinking">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
          <span class="thinking-text">正在分析失败摘要…</span>
        </div>
        <div v-if="traces.length" class="traces">
          <span
            v-for="(trace, index) in traces"
            :key="`${trace.tool}-${index}`"
            class="trace"
            :class="trace.ok ? 'ok' : 'fail'"
          >
            {{ trace.tool }}
            <i>{{ trace.elapsedMs }}ms</i>
          </span>
        </div>
        <div class="meta">
          <span class="meta-level">{{ levelMeta[current.level]?.label }}</span>
          <span v-if="current.reused" class="meta-reused">复用已有回答</span>
        </div>
      </article>

      <p v-if="error" class="err">{{ error }}</p>
    </div>

    <div v-if="enabled" class="hint-foot">
      <div class="chips">
        <button
          v-for="item in quickQuestions"
          :key="item.label"
          class="chip"
          :disabled="loading"
          @click="ask(item.text, 1)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="composer">
        <input
          v-model="draft"
          class="ask-input"
          :disabled="loading"
          placeholder="继续追问，例如：第 2 个测试点为什么过不了？"
          @keyup.enter="askDraft"
        />
        <button class="ask" :disabled="loading || !draft.trim()" @click="askDraft">
          <span v-if="loading" class="mini-spinner" />
          <span>{{ loading ? '生成中' : '发送' }}</span>
        </button>
      </div>

      <div class="foot-row">
        <button v-if="canDeepen" class="deep" :disabled="loading" @click="deepen">
          再具体一点<span class="arrow">→</span>
        </button>
        <span class="foot-note">最多 3 层：思路 → 定位 → 检查点</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { hintApi } from '../api'
import { API_BASE } from '../api/config'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  submissionId: { type: [Number, String], required: true },
  enabled: { type: Boolean, default: true },
  status: { type: String, default: '' }
})

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const levelMeta = {
  1: { label: '思路' },
  2: { label: '定位' },
  3: { label: '检查点' }
}

const quickQuestions = [
  { label: '为什么 WA？', text: '为什么这次提交是答案错误？' },
  { label: '为什么超时？', text: '为什么这次提交超时了？' },
  { label: '复杂度怎么优化？', text: '我的解法复杂度是否过高，可以从哪里优化？' }
]

const auth = useAuthStore()

const ready = ref(false)
const serverEnabled = ref(true)
const messages = ref([])
const current = ref({ question: '', answer: '', level: 1, reused: false })
const traces = ref([])
const error = ref('')
const loading = ref(false)
const draft = ref('')

let historyLoaded = false

const visible = computed(
  () => ready.value && serverEnabled.value && props.status !== 'AC'
)

const canDeepen = computed(() => {
  if (loading.value || !messages.value.length) return false
  const last = messages.value[messages.value.length - 1]
  return (last.level || 1) < 3
})

function render(text) {
  return DOMPurify.sanitize(md.render(text || ''))
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadHistory() {
  if (historyLoaded) return
  historyLoaded = true
  try {
    const list = await hintApi.history(props.submissionId)
    messages.value = Array.isArray(list) ? list : []
  } catch {
    messages.value = []
  }
}

function handleEvent(chunk) {
  let event = 'message'
  let data = ''
  for (const line of chunk.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) data += line.slice(5).trim()
  }
  if (!data) return null
  let payload
  try {
    payload = JSON.parse(data)
  } catch {
    return null
  }
  if (event === 'token') current.value.answer += payload.text || ''
  else if (event === 'tool_call') traces.value.push(payload)
  else if (event === 'error') error.value = payload.message || '助教处理失败'
  else if (event === 'done') return payload
  return null
}

async function ask(question, level) {
  const text = (question || '').trim()
  if (!text || loading.value) return

  loading.value = true
  error.value = ''
  traces.value = []
  current.value = { question: text, answer: '', level, reused: false }
  let finish = null

  try {
    const response = await fetch(`${API_BASE}/submissions/${props.submissionId}/hint`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${auth.token}`
      },
      body: JSON.stringify({ question: text, level })
    })

    const contentType = response.headers.get('content-type') || ''
    if (!response.ok || !contentType.includes('text/event-stream')) {
      const body = await response.json().catch(() => ({}))
      throw new Error(body.message || 'AI 助教暂不可用')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() || ''
      for (const chunk of chunks) {
        const payload = handleEvent(chunk)
        if (payload) finish = payload
      }
    }
    if (buffer.trim()) {
      const payload = handleEvent(buffer)
      if (payload) finish = payload
    }

    if (error.value) return

    messages.value.push({
      id: finish?.hintId,
      question: text,
      answer: current.value.answer,
      level: finish?.level ?? level,
      status: finish?.status,
      reused: Boolean(finish?.reused),
      createdAt: new Date().toISOString()
    })
    current.value = { question: '', answer: '', level: 1, reused: false }
    draft.value = ''
  } catch (err) {
    error.value = err.message || '请求失败'
  } finally {
    loading.value = false
    traces.value = []
  }
}

function askDraft() {
  const text = draft.value
  if (!text.trim()) return
  draft.value = ''
  ask(text, 1)
}

function deepen() {
  if (!messages.value.length) return
  const last = messages.value[messages.value.length - 1]
  const level = Math.min(3, (last.level || 1) + 1)
  ask(last.question, level)
}

onMounted(async () => {
  if (!props.enabled) {
    ready.value = true
    return
  }
  try {
    const status = await hintApi.status()
    serverEnabled.value = Boolean(status.enabled)
  } catch {
    serverEnabled.value = false
  }
  ready.value = true
  if (visible.value) {
    await loadHistory()
  }
})

watch(
  () => props.enabled,
  (value) => {
    if (value && visible.value) loadHistory()
  }
)

watch(
  () => props.submissionId,
  () => {
    historyLoaded = false
    messages.value = []
    current.value = { question: '', answer: '', level: 1, reused: false }
    error.value = ''
    if (visible.value) loadHistory()
  }
)
</script>

<style scoped>
.hint {
  margin-top: 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--bg-elev) 0%, var(--bg-sunken) 100%);
  overflow: hidden;
}

.hint-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.hint-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.spark {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--accent);
  transform: rotate(45deg);
  box-shadow: 0 0 10px rgba(45, 212, 191, 0.55);
}

.level-pill {
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 500;
  color: var(--accent);
  border: 1px solid rgba(45, 212, 191, 0.28);
  background: rgba(45, 212, 191, 0.08);
}

.hint-note {
  font-size: 11px;
  color: var(--text-faint);
}

.hint-body {
  padding: 14px 16px;
  min-height: 70px;
}

.placeholder {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-faint);
}

.placeholder strong {
  color: var(--text-dim);
  font-weight: 550;
}

.qa {
  padding: 12px 13px;
  border-radius: 9px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.014);
}

.qa + .qa {
  margin-top: 12px;
}

.qa.live {
  border-color: rgba(45, 212, 191, 0.22);
  background: rgba(45, 212, 191, 0.035);
}

.q {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.mark {
  flex: 0 0 auto;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 10px;
  line-height: 1.5;
  color: var(--accent);
  background: rgba(45, 212, 191, 0.12);
}

.q-text {
  font-size: 12px;
  color: var(--text-dim);
  word-break: break-word;
}

.a {
  font-size: 13px;
  line-height: 1.85;
  color: #cdd8e3;
  word-break: break-word;
}

.a :deep(p) {
  margin: 0 0 8px;
}

.a :deep(p:last-child) {
  margin-bottom: 0;
}

.a :deep(ul),
.a :deep(ol) {
  margin: 6px 0;
  padding-left: 20px;
}

.a :deep(li) {
  margin: 3px 0;
}

.a :deep(strong) {
  color: var(--text);
  font-weight: 600;
}

.a :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  font-family: var(--mono);
  font-size: 11.5px;
  background: var(--bg-elev-2);
  border: 1px solid var(--line);
}

.a :deep(pre) {
  margin: 8px 0;
  padding: 10px 12px;
  border-radius: 7px;
  overflow-x: auto;
  background: var(--bg-editor, var(--bg-sunken));
  border: 1px solid var(--line);
}

.a :deep(pre code) {
  padding: 0;
  border: 0;
  background: transparent;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 9px;
  font-size: 10.5px;
  color: var(--text-faint);
}

.meta-level {
  color: var(--text-faint);
}

.meta-reused {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-elev-2);
  border: 1px solid var(--line);
}

.meta-time {
  margin-left: auto;
  opacity: 0.75;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse-dot 1.1s ease-in-out infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.15s;
}

.dot:nth-child(3) {
  animation-delay: 0.3s;
}

.thinking-text {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-faint);
}

.traces {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.trace {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 10.5px;
  font-family: var(--mono);
  background: var(--bg-elev-2);
  border: 1px solid var(--line);
  color: var(--text-faint);
}

.trace i {
  font-style: normal;
  opacity: 0.7;
}

.trace.ok {
  color: var(--ok);
  border-color: rgba(34, 197, 94, 0.22);
}

.trace.fail {
  color: var(--err);
  border-color: rgba(239, 68, 68, 0.22);
}

.err {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--err);
}

.hint-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--bg-elev-2);
  color: var(--text-dim);
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.chip:hover:not(:disabled) {
  border-color: rgba(45, 212, 191, 0.4);
  color: var(--accent);
  transform: translateY(-1px);
}

.chip:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.composer {
  display: flex;
  gap: 8px;
}

.ask-input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--bg-sunken);
  color: var(--text);
  font-size: 12.5px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.ask-input::placeholder {
  color: var(--text-faint);
}

.ask-input:focus {
  border-color: rgba(45, 212, 191, 0.45);
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.08);
}

.ask {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid rgba(45, 212, 191, 0.35);
  background: rgba(45, 212, 191, 0.12);
  color: var(--accent);
  font-size: 12px;
  font-weight: 550;
  cursor: pointer;
  transition: background 0.18s ease, opacity 0.18s ease;
}

.ask:hover:not(:disabled) {
  background: rgba(45, 212, 191, 0.2);
}

.ask:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.foot-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.deep {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-dim);
  font-size: 11.5px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.18s ease;
}

.deep:hover:not(:disabled) {
  color: var(--accent);
}

.deep:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.arrow {
  transition: transform 0.2s ease;
}

.deep:hover:not(:disabled) .arrow {
  transform: translateX(3px);
}

.foot-note {
  font-size: 11px;
  color: var(--text-faint);
}

.mini-spinner {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid rgba(45, 212, 191, 0.25);
  border-top-color: var(--accent);
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>