<template>
  <span class="stat-num" :class="{ 'num-grad': grad }" :style="{ fontSize: size + 'px' }">{{ display }}</span>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  size: { type: Number, default: 26 },
  decimals: { type: Number, default: 0 },
  duration: { type: Number, default: 900 },
  grad: { type: Boolean, default: false }
})

const display = ref('0')

const animate = (from, to) => {
  const start = performance.now()
  const step = (now) => {
    const p = Math.min(1, (now - start) / props.duration)
    const eased = 1 - Math.pow(1 - p, 3)
    display.value = (from + (to - from) * eased).toFixed(props.decimals)
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(() => props.value, (v) => {
  animate(Number(display.value) || 0, v || 0)
})

onMounted(() => {
  display.value = (props.value || 0).toFixed(props.decimals)
})
</script>
