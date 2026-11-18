<template>
  <main class="login-page">
    <GrowthScene @stage="activeStage = $event" />
    <div class="cinema-shade" />

    <header class="topbar">
      <div class="brand"><span class="brand-mark">›_</span><span>OJ <b>Console</b></span></div>
      <span class="topbar-note"><i /> ONLINE JUDGE · YOUR NEXT LEVEL STARTS HERE</span>
    </header>

    <section class="story" aria-live="polite">
      <div class="eyebrow"><span class="eyebrow-line" /> THE JOURNEY OF A CODER <span class="chapter-count">0{{ activeStage + 1 }} / 04</span></div>
      <Transition mode="out-in" name="chapter">
        <div :key="activeStage" class="chapter">
          <p class="chapter-label">{{ stages[activeStage].label }}</p>
          <h1>{{ stages[activeStage].title }}<span>{{ stages[activeStage].accent }}</span></h1>
          <p class="chapter-description">{{ stages[activeStage].description }}</p>
          <div class="chapter-code"><span class="code-prompt">›</span> {{ stages[activeStage].code }}<span class="code-cursor" /></div>
        </div>
      </Transition>
      <div class="journey">
        <div v-for="(stage, index) in stages" :key="stage.label" class="journey-step" :class="{ current: index === activeStage, passed: index < activeStage }">
          <div class="journey-track"><span /></div>
          <span>0{{ index + 1 }}</span><strong>{{ stage.short }}</strong>
        </div>
      </div>
    </section>

    <section class="auth-panel" aria-labelledby="login-title">
      <h2 id="login-title">欢迎回来</h2>
      <p class="panel-hint">登录账号，继续刷题和查看成长记录</p>
      <form @submit.prevent="onSubmit">
        <label class="field"><span>用户名</span><input v-model.trim="username" class="input" autocomplete="username" placeholder="请输入用户名" /></label>
        <label class="field"><span>密码</span><input v-model="password" class="input" type="password" autocomplete="current-password" placeholder="请输入密码" /></label>
        <p v-if="auth.error" class="error" role="alert">{{ auth.error }}</p>
        <button class="submit" type="submit" :disabled="auth.loading"><span v-if="auth.loading" class="spinner" /><span class="submit-label">{{ auth.loading ? '验证中…' : '进入控制台' }}</span><span v-if="!auth.loading" class="arrow" aria-hidden="true">↗</span></button>
      </form>
      <div class="demo"><span>想先体验？</span><button type="button" @click="fillDemo">填入演示账号 <span>↗</span></button></div>
    </section>

    <footer class="page-foot"><span>BUILD · SOLVE · GROW</span><span>每一次 AC，都是新的起点。</span></footer>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import GrowthScene from '../components/GrowthScene.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const username = ref('admin')
const password = ref('')
const activeStage = ref(0)
const stages = [
  { label: 'LEVEL 01 · THE FIRST LINE', short: '起点', title: '从 Hello World，', accent: '开始。', description: '第一行代码，第一次运行成功。你的算法旅程由此展开。', code: 'print("Hello, World!")' },
  { label: 'LEVEL 02 · KEEP PRACTICING', short: '积累', title: '把每一道题，', accent: '刷成底气。', description: '循环、数组、排序。每一次提交，都让思路更加清晰。', code: 'for (let i = 0; i < n; i++)' },
  { label: 'LEVEL 03 · THINK DEEPER', short: '突破', title: '让复杂问题，', accent: '有迹可循。', description: '栈、队列、图与搜索。你开始看见问题背后的结构。', code: 'queue.push(nextNode)' },
  { label: 'LEVEL 04 · ALGORITHM MASTERY', short: '进阶', title: '向复杂算法，', accent: '继续进发。', description: '动态规划与最短路。曾经遥远的难题，正在被你逐个攻克。', code: 'dp[i] = min(dp[i], cost)' },
]

function fillDemo() { username.value = 'admin'; password.value = 'Admin@123' }
async function onSubmit() {
  if (!username.value || !password.value) { auth.error = '请输入用户名和密码'; return }
  try {
    await auth.login(username.value, password.value)
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/problems')
  } catch { /* 错误已写入 store */ }
}
</script>

<style scoped>
.login-page{min-height:100svh;position:relative;overflow:hidden;display:grid;grid-template-columns:minmax(0,1fr) minmax(360px,410px);align-items:center;gap:4vw;padding:108px 6vw 76px;background:#080d14;color:#e6edf3;isolation:isolate}
.cinema-shade{position:absolute;inset:0;z-index:1;pointer-events:none;background:linear-gradient(90deg,#080d14f2 0%,#080d14d8 36%,#080d1438 58%,#080d14a8 78%,#080d14ef 100%),linear-gradient(0deg,#080d14 0%,transparent 24% 76%,#080d14bd 100%)}
.topbar,.page-foot{position:absolute;z-index:3;left:6vw;right:6vw;display:flex;justify-content:space-between;align-items:center}.topbar{top:35px}.page-foot{bottom:24px;color:#647b86;font:10px var(--mono);letter-spacing:.16em}.page-foot span:last-child{letter-spacing:.04em}
.brand{display:flex;align-items:center;gap:12px;font-size:16px;letter-spacing:.03em}.brand b{font-weight:400;color:#98aaaF}.brand-mark{display:grid;place-items:center;width:37px;height:37px;background:#2dd4bf20;border:1px solid #2dd4bf77;color:#2dd4bf;border-radius:9px;font:700 19px var(--mono);box-shadow:0 0 25px #2dd4bf22}.topbar-note{font:10px var(--mono);letter-spacing:.1em;color:#8ca1a8}.topbar-note i{display:inline-block;width:6px;height:6px;background:#2dd4bf;border-radius:50%;margin-right:9px;box-shadow:0 0 12px #2dd4bf}
.story{position:relative;z-index:2;max-width:660px;align-self:center;padding-top:26px}.eyebrow{display:flex;align-items:center;gap:12px;color:#8ab4b7;font:11px var(--mono);letter-spacing:.12em}.eyebrow-line{width:30px;height:1px;background:#2dd4bf}.chapter-count{color:#2dd4bf;margin-left:8px}.chapter{min-height:340px}.chapter-label{color:#2dd4bf;font:12px var(--mono);letter-spacing:.14em;margin:42px 0 16px}.chapter h1{font-size:clamp(40px,4.5vw,70px);line-height:1.16;letter-spacing:-.055em;margin:0 0 24px;font-weight:700;text-shadow:0 4px 30px #0009}.chapter h1 span{display:block;color:#2dd4bf;text-shadow:0 0 35px #2dd4bf43}.chapter-description{color:#b6c5cc;font-size:15px;line-height:1.9;margin:0 0 22px;max-width:530px}.chapter-code{display:inline-flex;align-items:center;gap:9px;max-width:100%;padding:10px 15px;background:#0a1b22b8;border:1px solid #2d5961;border-radius:7px;color:#9aebdc;font:12px var(--mono);box-shadow:0 12px 30px #0005}.code-prompt{color:#2dd4bf;font-size:19px}.code-cursor{height:14px;width:6px;background:#2dd4bf;animation:blink 1s steps(2) infinite}
.journey{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;max-width:530px;margin-top:38px}.journey-step{color:#728790;font:10px var(--mono);display:grid;grid-template-columns:auto 1fr;gap:4px 8px}.journey-step strong{font-weight:400}.journey-track{grid-column:1/-1;height:2px;background:#31505a;overflow:hidden}.journey-track span{display:block;height:100%;width:0;background:#2dd4bf}.journey-step.passed,.journey-step.current{color:#b8e8df}.journey-step.passed .journey-track span{width:100%}.journey-step.current .journey-track span{animation:stage-progress 4.5s linear both}
.auth-panel{position:relative;z-index:3;width:100%;padding:29px 31px 20px;background:linear-gradient(145deg,#11202beF,#09141ef2);border:1px solid #38636b;border-radius:18px;box-shadow:0 35px 90px #000b,0 0 65px #2dd4bf16,inset 0 1px #c0fff01c;backdrop-filter:blur(22px)}.auth-panel:before{content:'';position:absolute;top:-1px;left:28px;width:100px;height:2px;background:#2dd4bf;box-shadow:0 0 18px #2dd4bf}.panel-top{display:flex;justify-content:space-between;color:#7aabb0;font:10px var(--mono);letter-spacing:.12em}.panel-status{color:#2dd4bf}.auth-panel h2{font-size:25px;line-height:1.35;letter-spacing:-.03em;margin:27px 0 6px}.auth-panel h2 span{color:#2dd4bf}.panel-hint{color:#8c9da9;font-size:13px;margin:0 0 27px}.field{display:block;margin-bottom:19px}.field>span{display:block;color:#b4c7cd;font-size:12px;margin-bottom:8px}.input{display:block;width:100%;height:47px;border:1px solid #35505b;border-radius:8px;background:#07141cbd;color:#e6edf3;padding:0 14px;outline:none;transition:border-color .2s,box-shadow .2s}.input::placeholder{color:#607985}.input:focus{border-color:#2dd4bf;box-shadow:0 0 0 3px #2dd4bf24}.error{color:#ff8a8a;font-size:12px;margin:0 0 12px}.submit{width:100%;height:48px;margin-top:7px;display:flex;align-items:center;justify-content:center;gap:8px;background:#2dd4bf;color:#05221e;border-radius:8px;font-weight:700;box-shadow:0 8px 28px #2dd4bf32;transition:transform .2s,box-shadow .2s}.submit:hover{transform:translateY(-2px);box-shadow:0 12px 36px #2dd4bf55}.submit:disabled{opacity:.7;cursor:wait}.arrow{margin-left:auto;margin-right:14px;font-size:18px}.submit:not(:has(.arrow)){padding:0 20px}.spinner{width:14px;height:14px;border:2px solid #06372d55;border-top-color:#06372d;border-radius:50%;animation:spin .7s linear infinite}.demo{display:flex;justify-content:space-between;align-items:center;gap:8px;border-top:1px solid #34505a;margin-top:24px;padding-top:18px;color:#7f939d;font-size:11px}.demo button{color:#8fe4d4;font:11px var(--mono)}.demo button:hover{text-decoration:underline}.demo button span{margin-left:4px}.panel-bottom{display:flex;justify-content:space-between;margin-top:21px;color:#496b73;font:9px var(--mono);letter-spacing:.16em}.panel-bottom span:not(:last-child):after{content:'→';margin-left:32px;color:#2c6970}
.chapter-enter-active,.chapter-leave-active{transition:opacity .38s,transform .38s}.chapter-enter-from{opacity:0;transform:translateY(28px)}.chapter-leave-to{opacity:0;transform:translateY(-20px)}@keyframes blink{50%{opacity:0}}@keyframes spin{to{transform:rotate(360deg)}}@keyframes stage-progress{from{width:0}to{width:100%}}
@media(max-width:960px){.login-page{grid-template-columns:1fr;gap:26px;align-content:start;padding:110px 6vw 75px}.story{max-width:none;padding:0}.chapter{min-height:260px}.chapter-label{margin:22px 0 12px}.chapter h1{font-size:clamp(35px,7vw,58px)}.journey{margin-top:15px}.auth-panel{max-width:520px}.cinema-shade{background:linear-gradient(90deg,#080d14dd,#080d1480),linear-gradient(0deg,#080d14 0%,transparent 38%,#080d14aa 100%)}}
@media(max-width:600px){.login-page{padding:83px 20px 68px}.topbar,.page-foot{left:20px;right:20px}.topbar{top:21px}.topbar-note{display:none}.page-foot span:last-child{display:none}.eyebrow{font-size:9px}.chapter{min-height:250px}.chapter-label{margin:20px 0 10px}.chapter h1{font-size:36px}.chapter-description{font-size:13px;margin-bottom:14px}.chapter-code{font-size:10px}.journey{margin-top:10px}.auth-panel{padding:24px 20px 18px}.auth-panel h2{margin-top:18px}.panel-bottom{display:none}}
@media(prefers-reduced-motion:reduce){.chapter-enter-active,.chapter-leave-active{transition:none}.journey-step.current .journey-track span,.code-cursor{animation:none}}
.submit{position:relative;font-size:15px;font-weight:700;letter-spacing:.08em;line-height:1}
.submit-label{display:block;text-align:center}
.submit .arrow{position:absolute;right:16px;margin:0;font-size:19px;font-weight:500;line-height:1;transition:transform .2s}
.submit:hover .arrow{transform:translate(2px,-2px)}
.journey-step:nth-child(1).current .journey-track span{animation-duration:5.32s}
.journey-step:nth-child(2).current .journey-track span,.journey-step:nth-child(3).current .journey-track span{animation-duration:3.42s}
.journey-step:nth-child(4).current .journey-track span{animation-duration:6.84s}

/* Match the application's light theme while keeping the camera scene readable. */
.login-page{background:#f4fbf8;color:#183c33}
.cinema-shade{background:linear-gradient(90deg,#f4fbf8f5 0%,#f4fbf8e0 35%,#f4fbf842 59%,#f4fbf8aa 79%,#f4fbf8ef 100%),linear-gradient(0deg,#f4fbf8 0%,transparent 25% 76%,#f4fbf8ba 100%)}
.brand b{color:#648579}.brand-mark{background:#e4f4ed;border-color:#9bcebc;color:#0d9488;box-shadow:0 8px 22px #0d948819}
.topbar-note,.page-foot{color:#698c80}.topbar-note i{background:#0d9488;box-shadow:0 0 10px #0d948855}
.eyebrow{color:#568d7e}.eyebrow-line{background:#0d9488}.chapter-count,.chapter-label{color:#0d9488}
.chapter h1{text-shadow:none}.chapter h1 span{color:#0d9488;text-shadow:none}.chapter-description{color:#58746b}
.chapter-code{padding:0;background:none;border:0;box-shadow:none;color:#177d6a;font-size:13px}.code-prompt,.code-cursor{color:#0d9488;background:#0d9488}.code-prompt{background:none}
.journey-step{color:#82a396}.journey-track{background:#cee4da}.journey-track span{background:#0d9488}.journey-step.passed,.journey-step.current{color:#1d7865}
.auth-panel{background:linear-gradient(145deg,#ffffffec,#f4fbf8ec);border-color:#c4dfd2;box-shadow:0 28px 70px #25745a20,0 2px 10px #25745a0c,inset 0 1px #fff;backdrop-filter:blur(20px)}
.auth-panel:before{background:#0d9488;box-shadow:0 0 16px #0d948840}
.panel-top{color:#668d7e}.panel-status,.auth-panel h2 span{color:#0d9488}.panel-hint{color:#718f84}
.field>span{color:#446d5f}.input{border-color:#c7dfd3;background:#fff;color:#183c33}.input::placeholder{color:#9db4a8}.input:focus{border-color:#0d9488;box-shadow:0 0 0 3px #0d948821}
.error{color:#c64747}.submit{background:#0d9488;color:#fff;box-shadow:0 8px 24px #0d94882d}.submit:hover{box-shadow:0 12px 30px #0d948844}.spinner{border-color:#ffffff66;border-top-color:#fff}
.demo{border-color:#d7e9df;color:#799588}.demo button{color:#0d9488}.panel-bottom{color:#9ab7a9}.panel-bottom span:not(:last-child):after{color:#8abca7}
.auth-panel{padding:36px 32px 28px;border-color:#d3e6dc;border-radius:16px;box-shadow:0 20px 54px #25745a17,0 2px 8px #25745a0a}
.auth-panel:before{display:none}
.auth-panel h2{margin:0 0 7px;font-size:27px;letter-spacing:-.035em}
.panel-hint{margin:0 0 29px;font-size:13px}
.demo{margin-top:24px;padding-top:18px;font-size:12px}
.demo button{font:600 12px var(--sans)}
@media(max-width:960px){.cinema-shade{background:linear-gradient(90deg,#f4fbf8e8,#f4fbf888),linear-gradient(0deg,#f4fbf8 0%,transparent 38%,#f4fbf8b8 100%)}}
@media(max-width:600px){.auth-panel{padding:28px 22px 24px}.auth-panel h2{font-size:24px}.panel-hint{margin-bottom:23px}}
</style>
