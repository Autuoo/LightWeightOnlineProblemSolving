<template>
  <div class="login">
    <div class="grid-glow" />

    <section class="pitch">
      <div class="logo">
        <span class="logo-mark">›_</span>
        <span class="logo-name">OJ Console</span>
      </div>
      <h1>提交代码，<br /><span class="accent">立刻知道对错。</span></h1>
      <p class="lede">
        轻量在线判题系统。异步评测、逐点判定、实时状态推送，
        并附带一个只看失败摘要、不泄露测试数据的 AI 助教。
      </p>

      <ul class="specs">
        <li>
          <span class="spec-k">评测链路</span>
          <span class="spec-v">提交 → 编译 → 逐点 → 终态</span>
        </li>
        <li>
          <span class="spec-k">支持语言</span>
          <span class="spec-v">Python 3 · C++17 · Java 17</span>
        </li>
        <li>
          <span class="spec-k">实时通道</span>
          <span class="spec-v">STOMP over WebSocket</span>
        </li>
      </ul>
    </section>

    <section class="card rise">
      <h2>登录</h2>
      <p class="hint">使用课程账号进入控制台</p>

      <form @submit.prevent="onSubmit">
        <label class="field">
          <span class="label">用户名</span>
          <input v-model.trim="username" class="input" autocomplete="username" placeholder="admin" />
        </label>

        <label class="field">
          <span class="label">密码</span>
          <input
            v-model="password"
            class="input"
            type="password"
            autocomplete="current-password"
            placeholder="••••••••"
          />
        </label>

        <p v-if="auth.error" class="error">{{ auth.error }}</p>

        <button class="btn btn-primary submit" type="submit" :disabled="auth.loading">
          <span v-if="auth.loading" class="spinner" />
          {{ auth.loading ? '验证中' : '进入控制台' }}
        </button>
      </form>

      <div class="demo">
        <span class="demo-label">演示账号</span>
        <button class="demo-fill" type="button" @click="fillDemo">admin / Admin@123</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('admin')
const password = ref('')

function fillDemo() {
  username.value = 'admin'
  password.value = 'Admin@123'
}

async function onSubmit() {
  if (!username.value || !password.value) {
    auth.error = '请输入用户名和密码'
    return
  }
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/problems'
    router.replace(redirect)
  } catch {
    /* 错误已写入 store */
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  align-items: center;
  gap: 40px;
  padding: 0 8vw;
  position: relative;
  overflow: hidden;
}

.grid-glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(600px 380px at 18% 30%, rgba(45, 212, 191, 0.09), transparent 70%),
    radial-gradient(500px 320px at 78% 72%, rgba(96, 165, 250, 0.07), transparent 70%);
  pointer-events: none;
}

.pitch {
  position: relative;
  max-width: 520px;
  animation: rise 0.5s cubic-bezier(0.22, 0.68, 0.35, 1) both;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 44px;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--accent-dim);
  border: 1px solid rgba(45, 212, 191, 0.32);
  color: var(--accent);
  font-family: var(--mono);
  font-weight: 700;
}

.logo-name {
  font-weight: 650;
  letter-spacing: 0.02em;
}

h1 {
  font-size: 40px;
  line-height: 1.24;
  letter-spacing: -0.02em;
  margin: 0 0 18px;
  font-weight: 650;
}

.accent {
  color: var(--accent);
}

.lede {
  color: var(--text-dim);
  font-size: 14.5px;
  line-height: 1.85;
  margin: 0 0 36px;
  max-width: 460px;
}

.specs {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid var(--line);
  padding-top: 22px;
  max-width: 460px;
}

.specs li {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 13px;
}

.spec-k {
  color: var(--text-faint);
}

.spec-v {
  color: var(--text-dim);
  font-family: var(--mono);
  font-size: 12px;
}

.card {
  position: relative;
  width: 100%;
  max-width: 400px;
  justify-self: end;
  padding: 34px 32px 30px;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--bg-elev) 0%, var(--bg-sunken) 100%);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.45);
}

h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 620;
  letter-spacing: -0.01em;
}

.hint {
  margin: 6px 0 26px;
  color: var(--text-faint);
  font-size: 12.5px;
}

.field {
  display: block;
  margin-bottom: 16px;
}

.label {
  display: block;
  margin-bottom: 7px;
  font-size: 12px;
  color: var(--text-dim);
  letter-spacing: 0.02em;
}

.error {
  margin: 4px 0 14px;
  font-size: 12.5px;
  color: var(--err);
}

.submit {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  justify-content: center;
}

.spinner {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid rgba(4, 18, 15, 0.3);
  border-top-color: #04120f;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.demo {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px dashed var(--line-strong);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.demo-label {
  font-size: 11.5px;
  color: var(--text-faint);
  letter-spacing: 0.04em;
}

.demo-fill {
  font-family: var(--mono);
  font-size: 11.5px;
  color: var(--accent);
  padding: 3px 8px;
  border-radius: 5px;
  background: var(--accent-dim);
  transition: background 0.18s ease;
}

.demo-fill:hover {
  background: rgba(45, 212, 191, 0.24);
}

@media (max-width: 980px) {
  .login {
    grid-template-columns: 1fr;
    padding: 6vh 6vw;
    gap: 32px;
    align-content: center;
  }

  .pitch {
    max-width: none;
  }

  h1 {
    font-size: 30px;
  }

  .card {
    justify-self: stretch;
    max-width: none;
  }
}
</style>