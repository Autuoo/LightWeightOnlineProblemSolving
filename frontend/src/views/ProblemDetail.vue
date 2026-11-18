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
          <span class="shortcut-hint mono">Ctrl/⌘ + Enter</span>
          <button class="btn btn-ghost reset" @click="resetCode">重置模板</button>
          <button class="btn btn-primary" :disabled="submitting || !problem" @click="submit">
            <span v-if="submitting" class="spinner" />
            {{ submitting ? '提交中' : '提交评测' }}
          </button>
        </div>
      </div>

      <div class="editor-wrap">
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

async function load() {
  loading.value = true
  submissionStore.stopRealtime()
  submissionStore.$patch({ detail: null, error: '' })
  reused.value = false
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
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  submissionStore.stopRealtime()
})
</script>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: minmax(380px, 0.92fr) minmax(460px, 1.08fr);
  gap: 18px;
  padding: 18px;
  height: 100vh;
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
  color: #c7d2dd;
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
  color: #b9c6d4;
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
  gap: 14px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

.lang-group {
  display: flex;
  gap: 2px;
  padding: 3px;
  border-radius: 8px;
  background: var(--bg-elev-2);
}

.lang {
  padding: 5px 12px;
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
}

.shortcut-hint {
  font-size: 10.5px;
  color: var(--text-faint);
  padding: 2px 6px;
  border-radius: 5px;
  border: 1px solid var(--line);
  background: var(--bg-elev);
  white-space: nowrap;
}

.reset {
  height: 34px;
  font-size: 12.5px;
}

.toolbar-right .btn-primary {
  height: 34px;
  font-size: 12.5px;
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
  min-height: 220px;
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
  max-height: 46%;
  display: flex;
  flex-direction: column;
  background: var(--bg-sunken);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.result-left,
.result-right {
  display: flex;
  align-items: center;
  gap: 12px;
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
</style>