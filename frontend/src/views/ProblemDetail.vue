<template>
  <div class="detail">
    <section class="brief">
      <div v-if="loading" class="brief-loading">
        <div class="skeleton" style="height: 28px; width: 40%" />
        <div class="skeleton" style="height: 14px; width: 90%; margin-top: 18px" />
        <div class="skeleton" style="height: 14px; width: 80%; margin-top: 10px" />
      </div>

      <template v-else-if="problem">
        <header class="brief-head">
          <h1>{{ problem.title }}</h1>
          <div class="meta">
            <span class="diff" :class="`d-${problem.difficulty}`">
              <i class="dot" /> {{ diffLabel(problem.difficulty) }}
            </span>
            <span class="meta-item mono">时限 {{ problem.timeLimitMs }}ms</span>
            <span class="meta-item mono">内存 {{ problem.memoryLimitMb }}MB</span>
          </div>
        </header>

        <article class="markdown" v-html="rendered" />

        <div v-if="samples.length" class="samples">
          <div class="section-title">样例</div>
          <div v-for="(sample, index) in samples" :key="index" class="sample">
            <div class="sample-label mono">输入 #{{ index + 1 }}</div>
            <pre class="sample-box mono">{{ sample.input }}</pre>
            <div class="sample-label mono">输出 #{{ index + 1 }}</div>
            <pre class="sample-box mono">{{ sample.output }}</pre>
          </div>
        </div>
      </template>
    </section>

    <section class="work">
      <div class="toolbar">
        <div class="lang-group">
          <button
            v-for="lang in languages"
            :key="lang"
            class="lang"
            :class="{ active: language === lang }"
            @click="changeLanguage(lang)"
          >
            {{ langLabel(lang) }}
          </button>
        </div>
        <div class="toolbar-right">

          <div class="history-wrap">
            <button class="btn btn-ghost history-toggle" :class="{ open: historyOpen }" @click.stop="toggleHistory">
              历史代码
              <span v-if="historyTotal" class="history-count">{{ historyTotal }}</span>
            </button>
            <div v-if="historyOpen" class="history-panel" @click.stop>
              <div class="history-head">
                <span>本题历史提交</span>
                <button class="history-close" aria-label="关闭" @click="historyOpen = false">×</button>
              </div>
              <div class="history-list">
                <div v-if="historyLoading" class="history-tip">加载中…</div>
                <p v-else-if="historyError" class="history-error">{{ historyError }}</p>
                <p v-else-if="!historyItems.length" class="history-tip">本题还没有提交记录</p>
                <template v-else>
                  <button
                    v-for="item in historyItems"
                    :key="item.id"
                    class="history-item"
                    @click="loadHistoryCode(item)"
                  >
                    <StatusBadge :status="item.status" />
                    <span class="history-lang mono">{{ item.language }}</span>
                    <span class="history-score mono">{{ item.score ?? 0 }} 分</span>
                    <span class="history-time mono">{{ formatHistoryTime(item.createdAt) }}</span>
                  </button>
                </template>
              </div>
              <div class="history-foot">
                <span>点击记录即可载入代码</span>
                <RouterLink to="/submissions">全部记录 →</RouterLink>
              </div>
            </div>
          </div>
          <button class="btn btn-ghost run-toggle" :disabled="!problem" @click="openCustomRun">
            运行自测
          </button>
          <button class="btn btn-ghost reset" @click="resetCode">重置模板</button>
          <button class="btn btn-primary" :disabled="submitting || !problem" @click="submit">
            <span v-if="submitting" class="spinner" />
            {{ submitting ? '提交中' : '提交评测' }}
          </button>
        </div>
      </div>

      <div class="editor-wrap">
        <span class="shortcut-hint mono">Ctrl/⌘ + Enter 提交</span>
        <div v-if="historyNotice" class="load-toast">{{ historyNotice }}</div>
        <CodeEditor v-model="code" :language="language" height="100%" />
      </div>

      <div v-if="submitError" class="submit-error">{{ submitError }}</div>

      <Transition name="slide">
        <div v-if="submissionStore.detail" class="result">
          <div class="result-head">
            <div class="result-left">
              <StatusBadge :status="submissionStore.detail.status" />
              <span v-if="reused" class="tag tag-info">复用最近结果</span>
              <span class="result-id mono">#{{ submissionStore.detail.id }}</span>
            </div>
            <div class="result-right">
              <span class="metric">
                <b class="mono">{{ submissionStore.detail.score ?? 0 }}</b>
                <span class="metric-label">分</span>
              </span>
              <span class="metric">
                <b class="mono">{{ submissionStore.detail.execTimeMs ?? '—' }}</b>
                <span class="metric-label">ms</span>
              </span>
              <RouterLink
                class="btn btn-ghost view-more"
                :to="{ name: 'submission-detail', params: { id: submissionStore.detail.id } }"
              >
                详情
              </RouterLink>
            </div>
          </div>

          <div class="result-body">
            <div v-if="submissionStore.detail.compileOutput" class="compile-box">
              <div class="compile-label">编译输出</div>
              <pre class="compile mono">{{ submissionStore.detail.compileOutput }}</pre>
            </div>
            <CaseResultTable :cases="submissionStore.detail.cases || []" />
          </div>
        </div>
      </Transition>
    </section>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="runOpen" class="run-overlay" @click.self="closeCustomRun">
          <section class="run-modal" role="dialog" aria-modal="true" aria-labelledby="run-title">
            <header class="run-head">
              <div class="run-head-copy">
                <h3 id="run-title">自定义运行</h3>
                <p>使用你自己填写的输入运行当前代码，不会读取或展示隐藏测试点。</p>
              </div>
              <button class="run-close" aria-label="关闭" @click="closeCustomRun">×</button>
            </header>

            <div class="run-body">
              <div class="run-pane">
                <div class="run-label">标准输入</div>
                <textarea
                  v-model="runInput"
                  class="run-input mono"
                  spellcheck="false"
                  placeholder="在这里粘贴输入数据，例如：&#10;5&#10;1 2 3 4 5"
                />
              </div>
              <div class="run-pane">
                <div class="run-label">
                  <span>运行结果</span>
                  <span
                    v-if="runResult"
                    class="run-meta mono"
                    :class="`run-state-${runResult.status.toLowerCase()}`"
                  >
                    {{ runStatusText }} · {{ runResult.timeMs }} ms
                  </span>
                </div>
                <pre v-if="runResult" class="run-output mono">{{ runDisplayOutput }}</pre>
                <div v-else class="run-placeholder">
                  {{ runLoading ? '正在编译并运行…' : '输入数据后，点击右下角「运行」查看输出' }}
                </div>
              </div>
            </div>

            <footer class="run-foot">
              <span v-if="runError" class="run-error">{{ runError }}</span>
              <span v-else class="run-hint">最多 64 KB 输入 · 时间限制与题目一致 · 不产生提交记录</span>
              <button class="btn btn-primary run-submit" :disabled="runLoading" @click="runCustom">
                <span v-if="runLoading" class="spinner" />
                {{ runLoading ? '运行中…' : '运行' }}
              </button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import CodeEditor from '../components/CodeEditor.vue'
import StatusBadge from '../components/StatusBadge.vue'
import CaseResultTable from '../components/CaseResultTable.vue'
import { problemApi, submissionApi } from '../api'
import { useSubmissionStore } from '../stores/submission'

const route = useRoute()
const submissionStore = useSubmissionStore()

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const problem = ref(null)
const loading = ref(true)
const language = ref('PYTHON3')
const submitting = ref(false)
const submitError = ref('')
const reused = ref(false)
const historyOpen = ref(false)
const historyItems = ref([])
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyError = ref('')
const historyNotice = ref('')
const runOpen = ref(false)
const runInput = ref('')
const runLoading = ref(false)
const runError = ref('')
const runResult = ref(null)
let noticeTimer = null

const TEMPLATES = {
  PYTHON3: `import sys

def solve():
    data = sys.stdin.read().split()
    # 在这里写你的逻辑

if __name__ == "__main__":
    solve()
`,
  CPP17: `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);
    // 在这里写你的逻辑
    return 0;
}
`,
  JAVA17: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // 在这里写你的逻辑
    }
}
`
}

const code = ref(TEMPLATES.PYTHON3)

const languages = computed(() => problem.value?.languages || ['PYTHON3', 'CPP17', 'JAVA17'])
const samples = computed(() => {
  const raw = problem.value?.samples
  return Array.isArray(raw) ? raw : []
})
const rendered = computed(() => DOMPurify.sanitize(md.render(problem.value?.description || '')))

const runStatusText = computed(() => {
  const status = runResult.value?.status
  return { OK: '运行成功', CE: '编译错误', TLE: '运行超时', RE: '运行异常' }[status] || ''
})

const runDisplayOutput = computed(() => {
  const result = runResult.value
  if (!result) return ''
  if (result.status === 'CE') {
    return result.compileOutput || '编译失败'
  }
  if (result.status === 'TLE') {
    return `程序运行超过题目时间限制（${problem.value?.timeLimitMs ?? '—'} ms），已被终止。`
  }
  const parts = []
  if (result.stdout) parts.push(result.stdout)
  if (result.stderr) parts.push(`[stderr]\n${result.stderr}`)
  if (!parts.length) {
    parts.push(result.status === 'OK' ? '（程序没有输出）' : `程序异常退出（exitCode ${result.exitCode}）`)
  }
  return parts.join('\n')
})

function diffLabel(level) {
  return { 1: '简单', 2: '中等', 3: '困难' }[level] || '未知'
}

function langLabel(lang) {
  return { PYTHON3: 'Python 3', CPP17: 'C++17', JAVA17: 'Java 17' }[lang] || lang
}

function changeLanguage(lang) {
  language.value = lang
  if (!code.value.trim() || Object.values(TEMPLATES).includes(code.value)) {
    code.value = TEMPLATES[lang] || ''
  }
}

function resetCode() {
  code.value = TEMPLATES[language.value] || ''
}

function formatHistoryTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function fetchHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    const data = await submissionApi.list({
      problemId: problem.value?.id,
      page: 1,
      size: 20
    })
    historyItems.value = data.records || []
    historyTotal.value = data.total || 0
  } catch (error) {
    historyError.value = error.message
    historyItems.value = []
    historyTotal.value = 0
  } finally {
    historyLoading.value = false
  }
}

function toggleHistory() {
  historyOpen.value = !historyOpen.value
  if (historyOpen.value) fetchHistory()
}

function showHistoryNotice(text) {
  historyNotice.value = text
  if (noticeTimer) clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => {
    historyNotice.value = ''
    noticeTimer = null
  }, 2800)
}

async function loadHistoryCode(item) {
  try {
    const data = await submissionApi.detail(item.id)
    code.value = data.code || ''
    language.value = data.language || language.value
    historyOpen.value = false
    showHistoryNotice(`已载入 #${item.id} 的代码 · ${langLabel(language.value)}`)
  } catch (error) {
    historyError.value = error.message
  }
}

function closeHistoryOnOutside(event) {
  if (historyOpen.value && !event.target.closest('.history-wrap')) {
    historyOpen.value = false
  }
}

function openCustomRun() {
  if (!problem.value) return
  runOpen.value = true
  runError.value = ''
}

function closeCustomRun() {
  if (runLoading.value) return
  runOpen.value = false
}

async function runCustom() {
  if (!problem.value || runLoading.value) return
  runLoading.value = true
  runError.value = ''
  runResult.value = null
  try {
    runResult.value = await submissionApi.runCustom({
      problemId: problem.value.id,
      language: language.value,
      code: code.value,
      input: runInput.value
    })
  } catch (error) {
    runError.value = error.message
  } finally {
    runLoading.value = false
  }
}

async function load() {
  loading.value = true
  submissionStore.stopRealtime()
  submissionStore.$patch({ detail: null, error: '' })
  reused.value = false
  historyOpen.value = false
  historyItems.value = []
  historyTotal.value = 0
  historyError.value = ''
  runOpen.value = false
  runResult.value = null
  runError.value = ''
  runInput.value = ''
  try {
    problem.value = await problemApi.detail(route.params.id)
  } catch {
    problem.value = null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!problem.value) return
  submitting.value = true
  submitError.value = ''
  reused.value = false
  try {
    const result = await submissionApi.submit({
      problemId: problem.value.id,
      language: language.value,
      code: code.value
    })
    reused.value = result.reused
    await submissionStore.load(result.submissionId, { force: true })
  } catch (error) {
    submitError.value = error.message
  } finally {
    submitting.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape' && runOpen.value) {
    closeCustomRun()
    return
  }
  if (event.key === 'Escape' && historyOpen.value) {
    historyOpen.value = false
    return
  }
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    if (!submitting.value && problem.value) {
      submit()
    }
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('click', closeHistoryOnOutside)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', closeHistoryOnOutside)
  if (noticeTimer) clearTimeout(noticeTimer)
  submissionStore.stopRealtime()
})
</script>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: minmax(380px, 0.92fr) minmax(460px, 1.08fr);
  gap: 18px;
  padding: 18px;
  flex: 1;
  min-height: 0;
}

.brief {
  overflow-y: auto;
  padding: 26px 28px 40px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
}

.brief-loading {
  padding-top: 6px;
}

.brief-head h1 {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 640;
  letter-spacing: -0.015em;
}

.meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 20px;
}

.diff {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
}

.diff .dot {
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

.meta-item {
  font-size: 11.5px;
  color: var(--text-faint);
}

.markdown {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-dim);
}

.markdown :deep(h2) {
  font-size: 15px;
  font-weight: 620;
  margin: 26px 0 10px;
  color: var(--text);
}

.markdown :deep(h3) {
  font-size: 14px;
  margin: 20px 0 8px;
}

.markdown :deep(p) {
  margin: 0 0 12px;
}

.markdown :deep(code) {
  font-family: var(--mono);
  font-size: 12.5px;
  background: var(--bg-elev-3);
  padding: 2px 6px;
  border-radius: 5px;
  color: #a5d6ff;
}

[data-theme="light"] .markdown :deep(code) {
  color: #0b5f8a;
}

.markdown :deep(pre) {
  background: var(--bg-sunken);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 14px 16px;
  overflow-x: auto;
}

.markdown :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 22px;
  margin: 0 0 12px;
}

.samples {
  margin-top: 30px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-bottom: 12px;
}

.sample {
  margin-bottom: 18px;
}

.sample-label {
  font-size: 11px;
  color: var(--text-faint);
  margin-bottom: 5px;
}

.sample-box {
  margin: 0 0 10px;
  background: var(--bg-sunken);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 13px;
  font-size: 12.5px;
  color: var(--text-dim);
  overflow-x: auto;
}

.work {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-elev);
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 14px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.lang-group {
  display: flex;
  gap: 2px;
  padding: 3px;
  border-radius: 8px;
  background: var(--bg-elev-2);
  flex-shrink: 0;
}

.lang {
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-faint);
  transition: background 0.18s ease, color 0.18s ease;
}

.lang:hover {
  color: var(--text-dim);
}

.lang.active {
  background: var(--bg-elev-3);
  color: var(--text);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

.shortcut-hint {
  position: absolute;
  right: 22px;
  bottom: 10px;
  z-index: 4;
  padding: 3px 7px;
  border-radius: 5px;
  border: 1px solid var(--line);
  background: rgba(13, 18, 24, 0.72);
  color: var(--text-faint);
  font-size: 10.5px;
  white-space: nowrap;
  pointer-events: none;
  backdrop-filter: blur(3px);
}

[data-theme="light"] .shortcut-hint {
  background: rgba(255, 255, 255, 0.82);
}

.reset {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-right .btn-primary {
  height: 32px;
  padding: 0 14px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.spinner {
  width: 12px;
  height: 12px;
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

.editor-wrap {
  flex: 1;
  min-height: 240px;
  position: relative;
}

.submit-error {
  padding: 10px 16px;
  font-size: 12.5px;
  color: var(--err);
  background: rgba(239, 68, 68, 0.08);
  border-top: 1px solid rgba(239, 68, 68, 0.2);
}

.result {
  border-top: 1px solid var(--line);
  max-height: 42%;
  display: flex;
  flex-direction: column;
  background: var(--bg-sunken);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--line);
}

.result-left,
.result-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-right {
  white-space: nowrap;
}

.result-id {
  font-size: 11.5px;
  color: var(--text-faint);
}

.metric {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
}

.metric b {
  font-size: 15px;
  font-weight: 620;
}

.metric-label {
  font-size: 10.5px;
  color: var(--text-faint);
}

.view-more {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.result-body {
  overflow-y: auto;
  padding: 14px 16px 18px;
}

.compile-box {
  margin-bottom: 14px;
}

.compile-label {
  font-size: 11px;
  color: var(--text-faint);
  margin-bottom: 6px;
}

.compile {
  margin: 0;
  padding: 11px 13px;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.07);
  border: 1px solid rgba(239, 68, 68, 0.16);
  color: #f2a3a3;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
}

.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.history-wrap {
  position: relative;
}

.history-toggle {
  height: 34px;
  font-size: 12.5px;
  gap: 6px;
}

.history-toggle.open {
  background: var(--bg-elev-3);
  color: var(--text);
}

.history-count {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--accent);
  background: rgba(45, 212, 191, 0.16);
}

.history-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  width: 328px;
  padding: 8px;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  background: var(--bg-elev);
  box-shadow: 0 20px 44px -24px rgba(0, 0, 0, 0.75);
}

.history-list {
  max-height: 292px;
  overflow-y: auto;
  padding-right: 2px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 6px 8px;
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-faint);
}

.history-close {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  color: var(--text-faint);
  font-size: 15px;
  line-height: 1;
}

.history-close:hover {
  background: var(--bg-hover);
  color: var(--text);
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px;
  border-radius: 7px;
  text-align: left;
  transition: background 0.15s ease;
}

.history-item:hover {
  background: var(--bg-hover);
}

.history-lang {
  min-width: 58px;
  font-size: 11px;
  color: var(--text-dim);
}

.history-score {
  margin-left: auto;
  font-size: 11px;
  color: var(--text);
}

.history-time {
  min-width: 74px;
  font-size: 10.5px;
  color: var(--text-faint);
  text-align: right;
}

.history-tip,
.history-error {
  margin: 6px;
  font-size: 12px;
}

.history-tip {
  color: var(--text-dim);
}

.history-error {
  color: var(--err);
}

.history-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  padding: 8px 6px 2px;
  border-top: 1px solid var(--line);
  font-size: 10.5px;
  color: var(--text-faint);
}

.history-foot a {
  color: var(--accent);
  white-space: nowrap;
}

.history-foot a:hover {
  text-decoration: underline;
}

.load-toast {
  position: absolute;
  top: 12px;
  left: 50%;
  z-index: 5;
  transform: translateX(-50%);
  padding: 6px 12px;
  border: 1px solid rgba(45, 212, 191, 0.35);
  border-radius: 7px;
  font-size: 11.5px;
  color: #7ff0e0;
  background: rgba(13, 18, 24, 0.92);
  box-shadow: 0 8px 22px -12px rgba(0, 0, 0, 0.7);
  pointer-events: none;
}

[data-theme="light"] .load-toast {
  background: #ffffff;
  color: #0d6b60;
}

.run-toggle {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.run-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(4, 8, 12, 0.62);
  backdrop-filter: blur(4px);
}

.run-modal {
  display: flex;
  flex-direction: column;
  width: min(980px, 100%);
  max-height: 86vh;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: var(--bg-elev);
  box-shadow: 0 40px 90px -40px rgba(0, 0, 0, 0.9);
  overflow: hidden;
}

.run-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.run-head h3 {
  margin: 0 0 5px;
  font-size: 16px;
  font-weight: 640;
}

.run-head p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-dim);
}

.run-close {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  color: var(--text-faint);
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.run-close:hover {
  background: var(--bg-hover);
  color: var(--text);
}

.run-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  flex: 1;
  min-height: 0;
  padding: 16px 22px;
}

.run-pane {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.run-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-faint);
}

.run-meta {
  font-size: 10.5px;
}

.run-state-ok {
  color: var(--ok);
}

.run-state-ce,
.run-state-re {
  color: var(--err);
}

.run-state-tle {
  color: var(--warn);
}

.run-input,
.run-output,
.run-placeholder {
  flex: 1;
  min-height: 210px;
  max-height: 46vh;
  overflow: auto;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--bg-sunken);
  color: var(--text);
  font-size: 12.5px;
  line-height: 1.7;
}

.run-input {
  resize: none;
}

.run-input:focus {
  outline: none;
  border-color: var(--accent);
}

.run-output {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.run-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-faint);
  font-size: 12px;
}

.run-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 22px 16px;
  border-top: 1px solid var(--line);
}

.run-hint {
  font-size: 11px;
  color: var(--text-faint);
}

.run-error {
  font-size: 12px;
  color: var(--err);
}

.run-submit {
  height: 34px;
  padding: 0 20px;
  flex-shrink: 0;
}

@media (max-width: 760px) {
  .run-body {
    grid-template-columns: 1fr;
  }

  .run-input,
  .run-output,
  .run-placeholder {
    min-height: 150px;
    max-height: 30vh;
  }
}
</style>