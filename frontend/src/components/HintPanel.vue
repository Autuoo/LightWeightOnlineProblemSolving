<template>
  <div class="hint">
    <div class="hint-head">
      <div class="hint-title">
        <span class="spark" />
        <span>AI 助教</span>
      </div>
      <span class="hint-note">只给方向，不给答案</span>
    </div>

    <div class="hint-body">
      <template v-if="!answer && !loading && !error">
        <p class="placeholder">
          它会读取题面、你的代码和<strong>脱敏后的</strong>失败摘要，
          提示可能的出错方向，不会返回测试点数据。
        </p>
      </template>

      <template v-if="loading && !answer">
        <div class="thinking">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
          <span class="thinking-text">正在分析失败摘要…</span>
        </div>
      </template>

      <div v-if="answer" class="answer">{{ answer }}</div>

      <div v-if="traces.length" class="traces">
        <span
          v-for="(trace, index) in traces"
          :key="`${trace.tool}-${index}`"
          class="trace"
          :class="{ ok: trace.ok, fail: !trace.ok }"
        >
          {{ trace.tool }}
          <i class="mono">{{ trace.elapsedMs }}ms</i>
        </span>
      </div>

      <p v-if="error" class="err">{{ error }}</p>
    </div>

    <div class="hint-foot">
      <button class="btn btn-primary ask" :disabled="loading || !enabled" @click="ask">
        <span v-if="loading" class="mini-spinner" />
        {{ answer ? '再问一次' : '获取提示' }}
      </button>
      <span v-if="!enabled" class="foot-note">评测完成后可用</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { API_BASE } from '../api/config'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  submissionId: { type: [Number, String], required: true },
  enabled: { type: Boolean, default: true }
})

const auth = useAuthStore()

const answer = ref('')
const error = ref('')
const loading = ref(false)
const traces = ref([])

function handleEvent(chunk) {
  let event = 'message'
  let data = ''
  for (const line of chunk.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) data += line.slice(5).trim()
  }
  if (!data) return
  let payload
  try {
    payload = JSON.parse(data)
  } catch {
    return
  }
  if (event === 'token') answer.value += payload.text || ''
  else if (event === 'tool_call') traces.value.push(payload)
  else if (event === 'error') error.value = payload.message || '助教处理失败'
}

async function ask() {
  loading.value = true
  error.value = ''
  answer.value = ''
  traces.value = []

  try {
    const response = await fetch(`${API_BASE}/submissions/${props.submissionId}/hint`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${auth.token}`
      },
      body: JSON.stringify({ question: '请分析这次提交为什么没有通过' })
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
      for (const chunk of chunks) handleEvent(chunk)
    }
    if (buffer.trim()) handleEvent(buffer)
  } catch (err) {
    error.value = err.message || '请求失败'
  } finally {
    loading.value = false
  }
}
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

.answer {
  font-size: 13px;
  line-height: 1.9;
  color: #cdd8e3;
  white-space: pre-wrap;
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
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.ask {
  height: 32px;
  font-size: 12.5px;
}

.foot-note {
  font-size: 11px;
  color: var(--text-faint);
}

.mini-spinner {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid rgba(4, 18, 15, 0.28);
  border-top-color: #04120f;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>