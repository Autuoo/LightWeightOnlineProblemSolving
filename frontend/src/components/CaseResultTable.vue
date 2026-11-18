<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th class="w-1">测试点</th>
          <th class="w-2">结果</th>
          <th class="w-3">耗时</th>
          <th class="w-4">内存</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in cases" :key="item.caseId" class="row">
          <td class="mono num">#{{ item.orderNo }}</td>
          <td><StatusBadge :status="item.status" /></td>
          <td class="mono num">{{ item.execTimeMs ?? '—' }}<span class="unit" v-if="item.execTimeMs != null"> ms</span></td>
          <td class="mono num">{{ formatMemory(item.memoryKb) }}</td>
        </tr>
        <tr v-if="!cases.length">
          <td colspan="4" class="empty">暂无评测数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import StatusBadge from './StatusBadge.vue'

defineProps({
  cases: { type: Array, default: () => [] }
})

function formatMemory(kb) {
  if (kb == null || kb <= 0) return '—'
  if (kb < 1024) return `${kb} KB`
  return `${(kb / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.table-wrap {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--bg-elev);
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead th {
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  padding: 11px 16px;
  border-bottom: 1px solid var(--line);
  background: var(--bg-sunken);
}

tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}

tbody tr:last-child td {
  border-bottom: none;
}

.row {
  transition: background 0.16s ease;
  animation: case-in 0.34s cubic-bezier(0.22, 0.68, 0.35, 1) both;
}

.row:nth-child(1) {
  animation-delay: 0ms;
}

.row:nth-child(2) {
  animation-delay: 55ms;
}

.row:nth-child(3) {
  animation-delay: 110ms;
}

.row:nth-child(4) {
  animation-delay: 165ms;
}

.row:nth-child(5) {
  animation-delay: 220ms;
}

.row:nth-child(6) {
  animation-delay: 275ms;
}

.row:nth-child(n + 7) {
  animation-delay: 330ms;
}

@keyframes case-in {
  from {
    opacity: 0;
    transform: translateX(-6px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.row:hover {
  background: var(--bg-elev-2);
}

.num {
  font-size: 12.5px;
  color: var(--text-dim);
}

.unit {
  color: var(--text-faint);
  font-size: 11px;
}

.w-1 {
  width: 96px;
}

.w-2 {
  width: 170px;
}

.w-3 {
  width: 120px;
}

.empty {
  text-align: center;
  color: var(--text-faint);
  padding: 28px 0;
  font-size: 13px;
}
</style>