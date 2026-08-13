<template>
  <div class="panel chart-card">
    <div class="chart-head">
      <div class="chart-title">
        <span class="chart-bar" :style="{ background: accent }"></span>{{ title }}
      </div>
      <slot name="extra"></slot>
    </div>
    <div ref="el" class="chart-body" :style="{ height: height + 'px' }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

// echarts 动态加载（避免顶层静态 import 在部分环境下 evaluate 失败导致白屏）
let echartsMod = null
const loadEcharts = async () => {
  if (!echartsMod) echartsMod = await import('echarts')
  return echartsMod
}

const props = defineProps({
  title: { type: String, default: '' },
  height: { type: Number, default: 300 },
  accent: { type: String, default: '#4f46e5' },
  option: { type: Object, default: null }
})

const el = ref()
let chart = null

const render = () => {
  if (!chart) return
  chart.setOption(props.option || {}, true)
}

onMounted(async () => {
  const echarts = await loadEcharts()
  chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart && chart.dispose()
})
const resize = () => chart && chart.resize()

watch(() => props.option, render, { deep: true })

defineExpose({ render })
</script>

<style scoped>
.chart-card { padding: 16px 18px 10px; }
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--line);
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: .5px;
}
.chart-bar {
  width: 14px;
  height: 2px;
  flex-shrink: 0;
}
</style>
