<template>
  <div class="page monitor">
    <div class="page-head" style="display: flex; justify-content: space-between; align-items: flex-end">
      <div>
        <div class="page-eyebrow">REAL-TIME MONITOR</div>
        <div class="page-title">运行监测</div>
        <div class="page-desc">设备实时状态 · 温度/振动/健康度 · 每 5 秒自动刷新</div>
      </div>
      <div class="m-head-clock">
        <div class="m-time stat-num">{{ now }}</div>
        <div class="live-badge"><span class="live-dot"></span>5S 轮询</div>
      </div>
    </div>

    <!-- 顶部指标 -->
    <div class="m-head">
      <div class="panel m-stat">
        <span class="m-label">监测设备</span>
        <CountUp class="m-num" :value="devices.length" :size="30" />
        <span class="m-unit">台</span>
      </div>
      <div class="panel m-stat">
        <span class="m-label">运行中</span>
        <CountUp class="m-num" :value="runningCount" :size="30" />
        <span class="m-unit">台</span>
      </div>
      <div class="panel m-stat">
        <span class="m-label">故障/维修</span>
        <CountUp class="m-num" :value="faultCount" :size="30" />
        <span class="m-unit">台</span>
      </div>
      <div class="panel m-stat">
        <span class="m-label">平均健康度</span>
        <CountUp class="m-num" :value="avgHealth" :size="30" :decimals="1" />
        <span class="m-unit">分</span>
      </div>
      <div class="panel m-stat">
        <span class="m-label">实时告警</span>
        <CountUp class="m-num" :value="alerts.length" :size="30" />
        <span class="m-unit">条</span>
      </div>
    </div>

    <!-- 告警横幅 -->
    <div v-if="alerts.length" class="panel alarm-banner">
      <el-icon :size="18" color="var(--danger)"><BellFilled /></el-icon>
      <div class="alarm-text">
        <span v-for="(a, i) in alerts.slice(0, 3)" :key="i" class="alarm-chip">
          <b>{{ a.name }}</b> {{ a.message }}
        </span>
        <span v-if="alerts.length > 3" class="alarm-more">等 {{ alerts.length }} 条告警</span>
      </div>
    </div>

    <!-- 设备矩阵 -->
    <div class="device-grid">
      <div
        v-for="d in devices"
        :key="d.deviceId"
        class="panel device-cell"
        :class="{ alerting: d.health < 60 }"
        @click="openRealtime(d)"
      >
        <div class="cell-head">
          <div class="cell-status" :class="(d.status || '').toLowerCase()">
            <span class="cell-dot"></span>{{ statusMap[d.status] }}
          </div>
          <span class="cell-type">{{ typeMap[d.type] }}</span>
        </div>
        <div class="cell-name" :title="d.name">{{ d.name }}</div>
        <div class="cell-code">{{ d.deviceCode }} · {{ d.time }}</div>
        <div class="cell-metrics">
          <div class="cell-metric">
            <span class="cm-label">温度</span>
            <span class="cm-value" style="color: var(--warn)">{{ d.temperature }}°C</span>
          </div>
          <div class="cell-metric">
            <span class="cm-label">振动</span>
            <span class="cm-value" style="color: var(--text-2)">{{ d.vibration }}mm/s</span>
          </div>
          <div class="cell-metric">
            <span class="cm-label">电压</span>
            <span class="cm-value" style="color: var(--accent)">{{ d.voltage }}V</span>
          </div>
        </div>
        <div class="health-bar">
          <div class="health-track">
            <div class="health-fill" :style="{ width: d.health + '%', background: healthColor(d.health) }"></div>
          </div>
          <span class="health-num" :style="{ color: healthColor(d.health) }">{{ d.health }}</span>
        </div>
      </div>
    </div>

    <!-- 实时曲线抽屉 -->
    <el-drawer v-model="drawerVisible" :title="current ? `${current.name} 实时监测` : ''" size="420px">
      <template v-if="current">
        <div class="drawer-head">
          <div class="drawer-status" :class="(current.status || '').toLowerCase()">
            <span class="cell-dot"></span>{{ statusMap[current.status] }}
          </div>
          <span class="drawer-code">{{ current.deviceCode }} · {{ current.location }}</span>
        </div>
        <div ref="drawerChart" style="height: 240px"></div>
        <div class="drawer-metrics">
          <div class="dm-item">
            <span class="dm-label">当前温度</span>
            <span class="dm-value" style="color: var(--warn)">{{ current.temperature }}°C</span>
          </div>
          <div class="dm-item">
            <span class="dm-label">当前振动</span>
            <span class="dm-value" style="color: var(--text-2)">{{ current.vibration }}mm/s</span>
          </div>
          <div class="dm-item">
            <span class="dm-label">健康度</span>
            <span class="dm-value" :style="{ color: healthColor(current.health) }">{{ current.health }}</span>
          </div>
        </div>
        <div class="drawer-tip">点击设备卡片可切换查看</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import CountUp from '@/components/CountUp.vue'
import { monitorStatus, monitorAlerts, monitorRealtime } from '@/api'
import { axisStyle, tooltipStyle, areaGradient } from '@/utils/echarts'

// echarts 动态加载（避免顶层静态 import 在部分环境下 evaluate 失败导致白屏）
let echartsMod = null
const loadEcharts = async () => {
  if (!echartsMod) echartsMod = await import('echarts')
  return echartsMod
}

const statusMap = { RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停用', SCRAPPED: '报废' }
const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }

const devices = ref([])
const alerts = ref([])
const now = ref('')
const runningCount = computed(() => devices.value.filter(d => d.status === 'RUNNING').length)
const faultCount = computed(() => devices.value.filter(d => ['FAULT', 'REPAIRING'].includes(d.status)).length)
const avgHealth = computed(() => {
  if (!devices.value.length) return 0
  return Math.round(devices.value.reduce((s, d) => s + d.health, 0) / devices.value.length * 10) / 10
})

const healthColor = (h) => h >= 80 ? '#16a34a' : h >= 60 ? '#d97706' : '#dc2626'

const load = async () => {
  try {
    const [list, alertList] = await Promise.all([monitorStatus(), monitorAlerts()])
    devices.value = list
    alerts.value = alertList
  } catch (e) { /* ignore */ }
}

const tick = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  now.value = `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// 抽屉曲线
const drawerVisible = ref(false)
const current = ref(null)
const drawerChart = ref()
let chart = null
let rtTimer = null

const openRealtime = async (d) => {
  current.value = d
  drawerVisible.value = true
  await nextTickRender()
}

const nextTickRender = async () => {
  try {
    const echarts = await loadEcharts()
    await new Promise(r => setTimeout(r, 50))
    if (!drawerChart.value) return
    if (!chart) {
      chart = echarts.init(drawerChart.value)
    }
    const data = await monitorRealtime(current.value.deviceId, 24)
    chart.setOption({
      tooltip: { ...tooltipStyle, trigger: 'axis' },
      legend: { top: 0, right: 0, textStyle: { color: '#94a3b8', fontSize: 11 } },
      grid: { left: 36, right: 10, top: 30, bottom: 24 },
      xAxis: { type: 'category', data: data.times, ...axisStyle },
      yAxis: { type: 'value', ...axisStyle },
      series: [
        { name: '温度°C', type: 'line', smooth: true, symbol: 'none', data: data.temperature, lineStyle: { width: 2, color: '#d97706' }, itemStyle: { color: '#d97706' }, areaStyle: { color: areaGradient('rgba(217,119,6,.1)') } },
        { name: '振动mm/s', type: 'line', smooth: true, symbol: 'none', data: data.vibration, lineStyle: { width: 2, color: '#96a2b4' }, itemStyle: { color: '#96a2b4' } }
      ]
    })
  } catch (e) { /* 轮询失败静默 */ }
}

let timer = null
onMounted(() => {
  tick()
  load()
  timer = setInterval(() => {
    tick()
    load()
    if (drawerVisible.value && current.value) nextTickRender()
  }, 5000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(rtTimer)
  chart && chart.dispose()
})
</script>

<style scoped>
.monitor { padding-top: 10px; }

.m-head {
  display: grid;
  grid-template-columns: repeat(5, 1fr) 1.3fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1200px) { .m-head { grid-template-columns: repeat(3, 1fr); } }

.m-stat { padding: 16px 20px; }
.m-head-clock { text-align: right; }
.m-time { font-size: 24px; font-weight: 500; letter-spacing: 2px; color: var(--text-1); }
.m-label { font-size: 12px; color: var(--text-3); display: block; margin-bottom: 6px; letter-spacing: 1px; }
.m-num {
  font-size: 30px;
  font-weight: 500;
  color: var(--text-1);
  letter-spacing: .5px;
}
.m-unit { font-size: 12px; color: var(--text-3); margin-left: 4px; }
.clock-time { font-size: 26px; letter-spacing: 2px; color: var(--text-1); }
.live-badge {
  margin-top: 8px;
  font-size: 10px;
  color: var(--ok);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  letter-spacing: 1.5px;
  font-family: var(--font-num);
}
.live-dot { width: 4px; height: 4px; border-radius: 50%; background: var(--ok); }

.alarm-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  margin-bottom: 14px;
  border-left: 2px solid var(--danger);
}
.alarm-text { display: flex; gap: 10px; flex-wrap: wrap; font-size: 12.5px; color: var(--text-2); }
.alarm-chip b { color: var(--text-1); }
.alarm-more { color: var(--danger); }

.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}
.device-cell { padding: 14px 16px; cursor: pointer; }
.device-cell.alerting { border-color: rgba(212, 93, 93, .45); }
.device-cell.alerting::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: var(--danger);
}
.cell-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.cell-status { display: flex; align-items: center; gap: 6px; font-size: 11.5px; }
.cell-dot { width: 4px; height: 4px; border-radius: 50%; }
.cell-status.running { color: var(--ok); } .cell-status.running .cell-dot { background: var(--ok); }
.cell-status.fault { color: var(--danger); } .cell-status.fault .cell-dot { background: var(--danger); }
.cell-status.repairing { color: var(--warn); } .cell-status.repairing .cell-dot { background: var(--warn); }
.cell-status.stopped { color: var(--text-3); } .cell-status.stopped .cell-dot { background: var(--text-3); }
.cell-type { font-size: 11px; color: var(--text-3); border: 1px solid var(--line); padding: 1px 7px; border-radius: 4px; }
.cell-name { font-size: 14.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cell-code { font-family: var(--font-num); font-size: 11.5px; color: var(--text-3); margin-top: 3px; }
.cell-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; margin: 12px 0 10px; }
.cell-metric {
  text-align: center;
  padding: 7px 2px;
  border-radius: 6px;
  background: var(--bg-deep);
  border: 1px solid var(--line);
}
.cm-label { display: block; font-size: 10.5px; color: var(--text-3); margin-bottom: 3px; }
.cm-value { font-family: var(--font-num); font-size: 12.5px; font-weight: 600; }
.health-bar { display: flex; align-items: center; gap: 8px; }
.health-track { flex: 1; height: 3px; border-radius: 2px; background: var(--line); overflow: hidden; }
.health-fill { height: 100%; border-radius: 2px; transition: width .6s ease; }
.health-num { font-family: var(--font-num); font-size: 12px; font-weight: 700; }

.drawer-head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.drawer-status { display: flex; align-items: center; gap: 6px; font-size: 12.5px; }
.drawer-status.running { color: var(--ok); }
.drawer-status .cell-dot { width: 4px; height: 4px; }
.drawer-code { font-family: var(--font-num); color: var(--text-3); font-size: 12px; margin-left: auto; }
.drawer-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 14px; }
.dm-item { text-align: center; padding: 12px 4px; border-radius: 6px; background: var(--bg-deep); border: 1px solid var(--line); }
.dm-label { display: block; font-size: 11px; color: var(--text-3); margin-bottom: 4px; }
.dm-value { font-family: var(--font-num); font-size: 16px; font-weight: 700; }
.drawer-tip { margin-top: 14px; text-align: center; font-size: 11.5px; color: var(--text-3); }
</style>
