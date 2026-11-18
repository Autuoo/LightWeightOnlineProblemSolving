<template>
  <span class="badge" :class="[`tone-${tone}`, { pulsing: running }]">
    <span class="dot" />
    <span class="label">{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'PENDING' }
})

const MAP = {
  PENDING: { label: '排队中', tone: 'mute', running: true },
  COMPILING: { label: '编译中', tone: 'info', running: true },
  RUNNING: { label: '评测中', tone: 'info', running: true },
  AC: { label: '通过', tone: 'ok', running: false },
  WA: { label: '答案错误', tone: 'err', running: false },
  TLE: { label: '运行超时', tone: 'warn', running: false },
  MLE: { label: '内存超限', tone: 'warn', running: false },
  RE: { label: '运行错误', tone: 'err', running: false },
  CE: { label: '编译错误', tone: 'err', running: false },
  REJECTED: { label: '被拒绝', tone: 'mute', running: false }
}

const entry = computed(() => MAP[props.status] || { label: props.status, tone: 'mute', running: false })
const label = computed(() => entry.value.label)
const tone = computed(() => entry.value.tone)
const running = computed(() => entry.value.running)
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 24px;
  padding: 0 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  transition: background 0.25s ease, color 0.25s ease;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  position: relative;
}

.badge.pulsing .dot::after {
  content: "";
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse-ring 1.4s cubic-bezier(0.24, 0.6, 0.36, 1) infinite;
}

.badge.pulsing .dot {
  animation: pulse-dot 1.4s ease-in-out infinite;
}

.tone-mute {
  background: rgba(139, 152, 168, 0.1);
  color: var(--text-dim);
}

.tone-info {
  background: rgba(96, 165, 250, 0.12);
  color: var(--info);
}

.tone-ok {
  background: rgba(34, 197, 94, 0.14);
  color: var(--ok);
}

.tone-warn {
  background: rgba(245, 158, 11, 0.14);
  color: var(--warn);
}

.tone-err {
  background: rgba(239, 68, 68, 0.14);
  color: var(--err);
  animation: shake 0.32s ease;
}
</style>