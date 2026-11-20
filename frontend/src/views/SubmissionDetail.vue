<template>
  <div class="page">
    <div v-if="submissionStore.loading" class="loading">
      <div class="skeleton" style="height: 120px" />
      <div class="skeleton" style="height: 220px" />
    </div>

    <template v-else-if="detail">
      <header class="summary rise">
        <div class="score-ring">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="52" class="ring-bg" />
            <circle
              cx="60"
              cy="60"
              r="52"
              class="ring-fg"
              :class="ringTone"
              :style="{ strokeDasharray: `${dash} 326.7` }"
            />
          </svg>
          <div class="ring-center">
            <span class="score mono">{{ detail.score ?? 0 }}</span>
            <span class="score-label">分</span>
          </div>
        </div>

        <div class="summary-main">
          <div class="summary-top">
            <StatusBadge :status="detail.status" />
            <span class="sid mono">提交 #{{ detail.id }}</span>
            <span v-if="live" class="live-tag">
              <i class="live-dot" /> 实时更新中
            </span>
          </div>
          <h1>{{ detail.problemTitle || `题目 ${detail.problemId}` }}</h1>
          <div v-if="auth.isAdmin && detail.suspicious" class="suspicious">
            <span class="suspicious-tag">可疑提交</span>
            <span class="suspicious-reason">
              {{ detail.suspiciousReason || '命中打表特征，仅标记不影响得分' }}
            </span>
          </div>
          <div class="facts">
            <span class="fact"><i>语言</i><b class="mono">{{ detail.language }}</b></span>
            <span class="fact"><i>耗时</i><b class="mono">{{ detail.execTimeMs ?? '—' }} ms</b></span>
            <span class="fact"><i>内存</i><b class="mono">{{ memoryText }}</b></span>
            <span class="fact"><i>提交</i><b class="mono">{{ formatTime(detail.createdAt) }}</b></span>
            <span v-if="detail.finishedAt" class="fact">
              <i>完成</i><b class="mono">{{ formatTime(detail.finishedAt) }}</b>
            </span>
          </div>
        </div>
      </header>

      <div class="grid">
        <section class="left rise" style="animation-delay: 40ms">
          <div class="section-head">
            <h2>逐点结果</h2>
            <span class="section-sub mono">
              {{ passedCount }} / {{ (detail.cases || []).length }} 通过
            </span>
          </div>
          <CaseResultTable :cases="detail.cases || []" />

          <div v-if="detail.compileOutput" class="compile-wrap">
            <div class="section-head"><h2>编译输出</h2></div>
            <pre class="compile mono">{{ detail.compileOutput }}</pre>
          </div>

          <div v-if="detail.errorMessage" class="error-wrap">
            <div class="section-head"><h2>错误信息</h2></div>
            <pre class="compile mono">{{ detail.errorMessage }}</pre>
          </div>

          <HintPanel
            v-if="detail.owner && !detail.contestId"
            :submission-id="detail.id"
            :enabled="finished"
            :status="detail.status"
          />
          <div v-else-if="detail.contestId" class="hint-owner-note">
            比赛提交不提供 AI 助教；赛后可复盘代码与逐点结果。
          </div>
          <div v-else class="hint-owner-note">
            AI 助教仅对提交本人开放；管理员查看他人提交时不提供提示。
          </div>
        </section>

        <section class="right rise" style="animation-delay: 80ms">
          <div class="section-head">
            <h2>提交代码</h2>
            <button class="btn btn-ghost copy" @click="copyCode">复制</button>
          </div>
          <div class="code-box">
            <div class="code-meta mono">{{ detail.language }} · 只读</div>
            <pre class="code-view mono">{{ detail.code || '' }}</pre>
          </div>
        </section>
      </div>
    </template>

    <div v-else class="empty-state">
      <p>{{ submissionStore.error || '提交不存在或无权查看' }}</p>
      <RouterLink class="btn" to="/submissions">返回提交记录</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import StatusBadge from '../components/StatusBadge.vue'
import CaseResultTable from '../components/CaseResultTable.vue'
import HintPanel from '../components/HintPanel.vue'
import { useSubmissionStore } from '../stores/submission'
import { useAuthStore } from '../stores/auth'
import { isTerminal } from '../api'

const route = useRoute()
const submissionStore = useSubmissionStore()
const auth = useAuthStore()
const detail = computed(() => submissionStore.detail)
const connected = computed(() => submissionStore.connected)

const finished = ref(false)
const live = computed(() => !finished.value && connected.value)

const dash = computed(() => {
  const score = Math.max(0, Math.min(100, detail.value?.score ?? 0))
  return (score / 100) * 326.7
})

const ringTone = computed(() => {
  const status = detail.value?.status
  if (status === 'AC') return 'ring-ok'
  if (status === 'CE' || status === 'RE' || status === 'WA') return 'ring-err'
  if (status === 'TLE' || status === 'MLE') return 'ring-warn'
  return 'ring-mute'
})

const passedCount = computed(
  () => (detail.value?.cases || []).filter((item) => item.status === 'AC').length
)

const memoryText = computed(() => {
  const kb = detail.value?.memoryKb
  if (kb == null || kb <= 0) return '—'
  return kb < 1024 ? `${kb} KB` : `${(kb / 1024).toFixed(1)} MB`
})

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
    date.getSeconds()
  )}`
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(detail.value?.code || '')
  } catch {
    /* 忽略剪贴板权限问题 */
  }
}

async function refresh() {
  await submissionStore.load(route.params.id)
  finished.value = isTerminal(submissionStore.detail?.status)
}

onMounted(refresh)
onBeforeUnmount(() => submissionStore.stopRealtime())
</script>

<style scoped>
.page {
  padding: 28px 36px 48px;
  max-width: 1280px;
}

.loading {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 26px 28px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--bg-elev) 0%, var(--bg-sunken) 100%);
  margin-bottom: 20px;
}

.score-ring {
  position: relative;
  width: 118px;
  height: 118px;
  flex: 0 0 118px;
}

.score-ring svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #18222c;
  stroke-width: 7;
}

.ring-fg {
  fill: none;
  stroke-width: 7;
  stroke-linecap: round;
  transition: stroke-dasharray 0.55s cubic-bezier(0.22, 0.68, 0.35, 1), stroke 0.3s ease;
}

.ring-ok {
  stroke: var(--ok);
  filter: drop-shadow(0 0 6px rgba(34, 197, 94, 0.35));
}

.ring-err {
  stroke: var(--err);
}

.ring-warn {
  stroke: var(--warn);
}

.ring-mute {
  stroke: var(--text-faint);
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score {
  font-size: 30px;
  font-weight: 650;
  letter-spacing: -0.03em;
  line-height: 1;
}

.score-label {
  font-size: 10px;
  color: var(--text-faint);
  margin-top: 3px;
}

.summary-main {
  min-width: 0;
}

.summary-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.sid {
  font-size: 11.5px;
  color: var(--text-faint);
}

.live-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--accent);
}

.live-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse-dot 1.2s ease-in-out infinite;
}

h1 {
  margin: 0 0 12px;
  font-size: 21px;
  font-weight: 630;
  letter-spacing: -0.015em;
}

.facts {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
}

.fact {
  display: flex;
  align-items: baseline;
  gap: 7px;
}

.fact i {
  font-style: normal;
  font-size: 11px;
  color: var(--text-faint);
}

.fact b {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text-dim);
}

.grid {
  display: grid;
  grid-template-columns: minmax(420px, 1.1fr) minmax(380px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-dim);
}

.section-sub {
  font-size: 11.5px;
  color: var(--text-faint);
}

.copy {
  height: 26px;
  font-size: 11.5px;
  padding: 0 9px;
}

.code-box {
  height: 520px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg-editor);
}

.code-meta {
  padding: 8px 14px;
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-faint);
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.code-view {
  flex: 1;
  margin: 0;
  padding: 16px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text);
  white-space: pre;
  tab-size: 4;
}

.compile-wrap,
.error-wrap {
  margin-top: 18px;
}

.compile {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.16);
  color: #f0a5a5;
  font-size: 12px;
  white-space: pre-wrap;
  overflow-x: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 120px 0;
  color: var(--text-faint);
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .code-box {
    height: 380px;
  }

  .summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
.suspicious {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 2px;
  padding: 6px 10px;
  border-radius: 7px;
  border: 1px solid rgba(251, 191, 36, 0.3);
  background: rgba(251, 191, 36, 0.07);
}

.suspicious-tag {
  font-size: 11px;
  font-weight: 650;
  color: #fbbf24;
}

.suspicious-reason {
  font-size: 11.5px;
  color: var(--text-dim);
}
.hint-owner-note {
  margin-top: 20px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px dashed var(--line);
  background: var(--bg-sunken);
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-faint);
}
</style>