<template>
  <div class="page">
    <header class="head rise">
      <div>
        <h1>个人设置</h1>
        <p class="sub">
          配置你自己的 AI 模型凭据。未配置时 AI 助教不可用，配置只对当前账号生效。
        </p>
      </div>
    </header>

    <section class="panel rise">
      <div class="card-head">
        <h2>AI 助教</h2>
        <span class="pill" :class="{ on: view.enabled && view.hasApiKey }">{{ statusText }}</span>
      </div>

      <form class="form" @submit.prevent="save">
        <label class="check">
          <input v-model="form.enabled" type="checkbox" />
          <span>启用 AI 助教</span>
        </label>

        <label class="field">
          <span class="label">模型端点（OpenAI 兼容）</span>
          <input
            v-model.trim="form.baseUrl"
            class="input"
            placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1"
          />
          <span class="tip">写到 <code>/v1</code> 为止，程序会自动拼接 <code>/chat/completions</code></span>
        </label>

        <label class="field">
          <span class="label">模型名称</span>
          <input v-model.trim="form.model" class="input" placeholder="qwen-plus" />
        </label>

        <label class="field">
          <span class="label">API Key</span>
          <input
            v-model="form.apiKey"
            class="input"
            type="password"
            autocomplete="new-password"
            :placeholder="keyPlaceholder"
          />
          <span class="tip">
            当前：{{ view.hasApiKey ? view.apiKeyMasked : '未配置' }}。
            Key 加密存储，页面不会显示明文；留空表示不修改。
          </span>
        </label>

        <p v-if="error" class="err">{{ error }}</p>
        <p v-if="notice" class="ok">{{ notice }}</p>
        <p v-if="testResult" class="test" :class="testResult.ok ? 'ok' : 'err'">
          {{
            testResult.ok
              ? `连接成功：${testResult.model}，耗时 ${testResult.elapsedMs}ms`
              : `连接失败：${testResult.message}`
          }}
        </p>

        <div class="actions">
          <button class="btn btn-primary" type="submit" :disabled="saving">
            {{ saving ? '保存中' : '保存配置' }}
          </button>
          <button class="btn btn-ghost" type="button" :disabled="testing" @click="testConnection">
            {{ testing ? '测试中' : '测试连接' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { aiConfigApi } from '../api'

const view = reactive({
  enabled: false,
  baseUrl: '',
  model: '',
  hasApiKey: false,
  apiKeyMasked: '',
  updatedAt: null
})

const form = reactive({
  enabled: true,
  baseUrl: '',
  model: '',
  apiKey: ''
})

const saving = ref(false)
const testing = ref(false)
const error = ref('')
const notice = ref('')
const testResult = ref(null)

const statusText = computed(() => {
  if (!view.hasApiKey || !view.baseUrl) return '未配置'
  return view.enabled ? '已启用' : '已停用'
})

const keyPlaceholder = computed(() => (view.hasApiKey ? '留空则保持不变' : 'sk-...'))

async function load() {
  try {
    const data = await aiConfigApi.get()
    Object.assign(view, data)
    form.enabled = Boolean(data.enabled)
    form.baseUrl = data.baseUrl || ''
    form.model = data.model || ''
  } catch (err) {
    error.value = err.message || '加载配置失败'
  }
}

async function save() {
  saving.value = true
  error.value = ''
  notice.value = ''
  testResult.value = null
  try {
    const data = await aiConfigApi.save({
      enabled: form.enabled,
      baseUrl: form.baseUrl,
      model: form.model,
      apiKey: form.apiKey ? form.apiKey : null
    })
    Object.assign(view, data)
    form.apiKey = ''
    notice.value = data.hasApiKey && data.baseUrl ? '配置已保存' : '已保存，但端点或 Key 不完整，AI 助教仍不可用'
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function testConnection() {
  testing.value = true
  error.value = ''
  notice.value = ''
  testResult.value = null
  try {
    testResult.value = await aiConfigApi.test()
  } catch (err) {
    testResult.value = { ok: false, message: err.message || '测试失败' }
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 34px 40px 48px;
  max-width: 860px;
}

.head {
  margin-bottom: 20px;
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
  line-height: 1.7;
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

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  margin-top: 2px;
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
</style>