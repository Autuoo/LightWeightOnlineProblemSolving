<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>题目管理</h1>
        <p class="sub">创建题目、维护测试点。测试点数据不会出现在学生端。</p>
      </div>
    </header>

    <div class="grid">
      <section class="panel rise" style="animation-delay: 30ms">
        <div class="card-head">
          <h2>{{ editingId ? `编辑题目 #${editingId}` : '新建题目' }}</h2>
          <button v-if="editingId" class="btn btn-ghost" @click="resetForm">取消编辑</button>
        </div>

        <form class="form" @submit.prevent="saveProblem">
          <label class="field">
            <span class="label">标题</span>
            <input v-model.trim="form.title" class="input" placeholder="A+B Problem" />
          </label>

          <label class="field">
            <span class="label">题面（Markdown）</span>
            <textarea v-model="form.description" class="input textarea" rows="9" placeholder="## 题目描述..." />
          </label>

          <div class="row">
            <label class="field">
              <span class="label">难度</span>
              <select v-model.number="form.difficulty" class="input">
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
            </label>
            <label class="field">
              <span class="label">时间限制 (ms)</span>
              <input v-model.number="form.timeLimitMs" class="input" type="number" min="100" />
            </label>
            <label class="field">
              <span class="label">内存限制 (MB)</span>
              <input v-model.number="form.memoryLimitMb" class="input" type="number" min="16" />
            </label>
          </div>

          <label class="field">
            <span class="label">样例（每行一个：输入 ||| 输出）</span>
            <textarea v-model="samplesText" class="input textarea mono" rows="3" placeholder="1 2 ||| 3" />
          </label>

          <label class="check">
            <input v-model="form.visible" type="checkbox" />
            <span>对学生可见</span>
          </label>

          <p v-if="problemError" class="err">{{ problemError }}</p>
          <p v-if="problemOk" class="ok">{{ problemOk }}</p>

          <div class="actions">
            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中' : editingId ? '保存修改' : '创建题目' }}
            </button>
          </div>
        </form>
      </section>

      <section class="panel rise" style="animation-delay: 70ms">
        <div class="card-head">
          <h2>测试点导入</h2>
          <span class="mono card-sub">整体替换</span>
        </div>

        <form class="form" @submit.prevent="saveTestCases">
          <label class="field">
            <span class="label">题目 ID</span>
            <input v-model.number="testCaseProblemId" class="input" type="number" min="1" />
          </label>

          <label class="field">
            <span class="label">测试点（JSON 数组）</span>
            <textarea
              v-model="testCasesText"
              class="input textarea mono code-area"
              rows="12"
              spellcheck="false"
            />
          </label>

          <div class="hint-box">
            <div class="hint-line"><span class="mono">orderNo</span> 从 1 开始且不可重复</div>
            <div class="hint-line"><span class="mono">score</span> 建议合计 100</div>
            <div class="hint-line">输入与期望输出支持 <span class="mono">\n</span> 换行</div>
          </div>

          <p v-if="caseError" class="err">{{ caseError }}</p>
          <p v-if="caseOk" class="ok">{{ caseOk }}</p>

          <div class="actions">
            <button class="btn btn-primary" type="submit" :disabled="savingCases">
              {{ savingCases ? '导入中' : '导入测试点' }}
            </button>
            <button class="btn btn-ghost" type="button" @click="fillExample">填充示例</button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { adminApi } from '../api'

const editingId = ref(null)
const saving = ref(false)
const problemError = ref('')
const problemOk = ref('')

const form = reactive({
  title: '',
  description: '',
  difficulty: 1,
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  visible: true
})

const samplesText = ref('')

const testCaseProblemId = ref(1)
const testCasesText = ref('')
const savingCases = ref(false)
const caseError = ref('')
const caseOk = ref('')

function parseSamples() {
  return samplesText.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [input, output] = line.split('|||')
      return { input: (input || '').trim(), output: (output || '').trim() }
    })
}

function buildPayload() {
  return {
    title: form.title,
    description: form.description,
    samples: parseSamples(),
    difficulty: form.difficulty,
    timeLimitMs: form.timeLimitMs,
    memoryLimitMb: form.memoryLimitMb,
    visible: form.visible ? 1 : 0
  }
}

async function saveProblem() {
  saving.value = true
  problemError.value = ''
  problemOk.value = ''
  try {
    if (editingId.value) {
      await adminApi.updateProblem(editingId.value, buildPayload())
      problemOk.value = `题目 #${editingId.value} 已更新`
    } else {
      const data = await adminApi.createProblem(buildPayload())
      editingId.value = data.id
      testCaseProblemId.value = data.id
      problemOk.value = `题目已创建，ID = ${data.id}，可以接着导入测试点`
    }
  } catch (error) {
    problemError.value = error.message
  } finally {
    saving.value = false
  }
}

async function saveTestCases() {
  savingCases.value = true
  caseError.value = ''
  caseOk.value = ''
  try {
    const parsed = JSON.parse(testCasesText.value)
    if (!Array.isArray(parsed) || !parsed.length) {
      throw new Error('需要非空的 JSON 数组')
    }
    await adminApi.replaceTestCases(testCaseProblemId.value, { testCases: parsed })
    caseOk.value = `已导入 ${parsed.length} 个测试点`
  } catch (error) {
    caseError.value = error.message.includes('JSON') ? 'JSON 格式不正确' : error.message
  } finally {
    savingCases.value = false
  }
}

function fillExample() {
  testCasesText.value = JSON.stringify(
    [
      { orderNo: 1, input: '1 2\n', expectedOutput: '3\n', score: 50 },
      { orderNo: 2, input: '100 -37\n', expectedOutput: '63\n', score: 50 }
    ],
    null,
    2
  )
}

function resetForm() {
  editingId.value = null
  form.title = ''
  form.description = ''
  form.difficulty = 1
  form.timeLimitMs = 1000
  form.memoryLimitMb = 256
  form.visible = true
  samplesText.value = ''
  problemError.value = ''
  problemOk.value = ''
}
</script>

<style scoped>
.page {
  padding: 34px 40px 48px;
  max-width: 1180px;
}

.head {
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

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: start;
}

.panel {
  padding: 20px 22px 24px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-head h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 620;
}

.card-sub {
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
  min-width: 0;
}

.label {
  display: block;
  margin-bottom: 6px;
  font-size: 11.5px;
  color: var(--text-dim);
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.textarea {
  height: auto;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.7;
}

.code-area {
  font-size: 12px;
  background: var(--bg-sunken);
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

.hint-box {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px dashed var(--line-strong);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hint-line {
  font-size: 11.5px;
  color: var(--text-faint);
}

.hint-line .mono {
  color: var(--text-dim);
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
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

@media (max-width: 1000px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>