<template>
  <div class="page" v-if="device">
    <el-page-header @back="$router.back()" class="page-header">
      <template #content>
        <span class="header-title">{{ device.name }}</span>
        <span class="code-chip">{{ device.deviceCode }}</span>
        <span class="status-text">
          <span class="status-dot" :class="devDot(device.status)"></span>{{ statusMap[device.status] }}
        </span>
      </template>
    </el-page-header>

    <div class="detail-grid">
      <!-- 左侧：设备信息 -->
      <div class="left-col">
        <div class="panel info-card">
          <div class="info-head">
            <div class="device-icon">
              <el-icon :size="34"><component :is="typeIcon(device.type)" /></el-icon>
            </div>
            <div>
              <div class="info-name">{{ device.name }}</div>
              <div class="info-loc"><el-icon :size="13"><Location /></el-icon>{{ device.location }}</div>
            </div>
          </div>

          <!-- 关键数据条 -->
          <div class="key-metrics">
            <div class="km-item">
              <span class="km-label">保修剩余</span>
              <span class="km-value stat-num" :style="{ color: warrantyDays == null ? 'var(--text-3)' : warrantyDays < 0 ? 'var(--danger)' : warrantyDays < 90 ? 'var(--warn)' : 'var(--ok)' }">
                {{ warrantyDays == null ? '—' : warrantyDays < 0 ? '已过保' : warrantyDays + ' 天' }}
              </span>
            </div>
            <div class="km-item">
              <span class="km-label">巡检周期</span>
              <span class="km-value stat-num">{{ device.inspectCycle }} 天</span>
            </div>
            <div class="km-item">
              <span class="km-label">关联工单</span>
              <span class="km-value stat-num">{{ orders.length }} 单</span>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">设备类型</span>
              <span class="info-value">{{ typeMap[device.type] }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">规格型号</span>
              <span class="info-value">{{ device.model || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">生产厂家</span>
              <span class="info-value">{{ device.manufacturer || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">安装日期</span>
              <span class="info-value">{{ device.installDate || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">保修截止</span>
              <span class="info-value">{{ device.warrantyEnd || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">巡检/保养周期</span>
              <span class="info-value">{{ device.inspectCycle }}天 / {{ device.maintainCycle }}天</span>
            </div>
            <div class="info-item">
              <span class="info-label">已使用年限</span>
              <span class="info-value" :style="{ color: lifeInfo.overdue ? 'var(--danger)' : 'inherit' }">
                {{ lifeInfo.ageYears }} 年 / {{ device.serviceLifeYears }} 年
                <el-tag v-if="lifeInfo.overdue" size="small" type="danger" round>超期 {{ lifeInfo.overYears }} 年</el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">建档时间</span>
              <span class="info-value">{{ (device.createTime || '').replace('T', ' ') }}</span>
            </div>
          </div>

          <!-- 技术参数 -->
          <div v-if="specKeys.length" class="spec-box">
            <div class="spec-title">技术参数</div>
            <div class="spec-grid">
              <div v-for="k in specKeys" :key="k" class="spec-item">
                <span class="spec-label">{{ k }}</span>
                <span class="spec-value">{{ specMap[k] }}</span>
              </div>
            </div>
          </div>

          <div v-if="device.remark" class="remark-box">
            <el-icon :size="13"><ChatLineSquare /></el-icon>{{ device.remark }}
          </div>
        </div>

        <!-- 实时监测 -->
        <div class="panel info-card" style="margin-top: 16px">
          <div class="card-title">
            <span class="card-dot"></span>实时运行监测
            <span class="live-badge"><span class="live-dot"></span>LIVE</span>
          </div>
          <div ref="realtimeEl" style="height: 220px"></div>
          <div class="metric-row">
            <div class="metric-item">
              <span class="metric-label">温度</span>
              <span class="metric-value" style="color: var(--warn)">{{ current.temperature ?? '-' }}°C</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">振动</span>
              <span class="metric-value" style="color: var(--text-2)">{{ current.vibration ?? '-' }}mm/s</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">健康度</span>
              <span class="metric-value" :style="{ color: healthColor(current.health) }">{{ current.health ?? '-' }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">运行天数</span>
              <span class="metric-value" style="color: var(--ok)">{{ current.runtimeDays ?? '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：二维码 + 任务 + 工单 -->
      <div class="right-col">
        <div class="panel qr-card">
          <div class="card-title">
            <span class="card-dot"></span>二维码标签
          </div>
          <div class="qr-body">
            <div class="qr-img-wrap">
              <img v-if="qrUrl" :src="qrUrl" alt="设备二维码" />
              <div v-else class="qr-loading">生成中...</div>
            </div>
            <el-button type="primary" plain style="margin-top: 12px" @click="downloadQr">
              <el-icon><Download /></el-icon>下载标签 PNG
            </el-button>
            <div class="qr-hint">手机扫码 → 移动端巡检打卡</div>
          </div>
        </div>

        <div class="panel info-card" style="margin-top: 16px">
          <div class="card-title">
            <span class="card-dot"></span>最近巡检/保养记录
            <el-button link type="primary" size="small" @click="$router.push('/task/inspect')">全部 →</el-button>
          </div>
          <div v-if="tasks.length" class="timeline">
            <div v-for="t in tasks" :key="t.id" class="tl-item">
              <span class="tl-dot" :class="(t.status || '').toLowerCase()"></span>
              <div class="tl-body">
                <div class="tl-head">
                  <span class="tl-type" :class="t.taskType.toLowerCase()">{{ t.taskType === 'INSPECT' ? '巡检' : '保养' }}</span>
                  <span class="tl-date">{{ t.planDate }}</span>
                  <span class="tl-status" :class="(t.status || '').toLowerCase()">{{ taskStatusMap[t.status] }}</span>
                </div>
                <div class="tl-desc">{{ t.remark || (t.executor ? `执行人：${t.executor}` : '暂无备注') }}</div>
                <div v-if="t.checkTime" class="tl-meta">
                  <el-icon :size="11"><Clock /></el-icon>{{ (t.checkTime || '').replace('T', ' ') }}
                  <span v-if="t.executor"> · {{ t.executor }}</span>
                  <span v-if="t.location"> · {{ t.location }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无任务记录" :image-size="60" />
        </div>

        <div class="panel info-card" style="margin-top: 16px">
          <div class="card-title">
            <span class="card-dot"></span>关联报修工单
            <el-button link type="primary" size="small" @click="$router.push('/repair')">全部 →</el-button>
          </div>
          <div v-if="orders.length" class="order-list">
            <div v-for="o in orders" :key="o.id" class="order-item">
              <span class="order-no">{{ o.orderCode }}</span>
              <span class="order-desc" :title="o.faultDesc">{{ o.faultDesc }}</span>
              <span class="order-status" :class="(o.status || '').toLowerCase()">{{ orderStatusMap[o.status] }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无关联工单" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deviceDetail, deviceQr, tasksByDevice, monitorRealtime } from '@/api'
import { axisStyle, tooltipStyle, areaGradient } from '@/utils/echarts'

// echarts 动态加载（避免顶层静态 import 在部分环境下 evaluate 失败导致白屏）
let echartsMod = null
const loadEcharts = async () => {
  if (!echartsMod) echartsMod = await import('echarts')
  return echartsMod
}

const route = useRoute()
const device = ref(null)
const tasks = ref([])
const orders = ref([])
const qrUrl = ref('')
const current = ref({})

const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }
const statusMap = { RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停用', SCRAPPED: '报废' }
const devDot = (s) => ({ RUNNING: 'ok', FAULT: 'danger', REPAIRING: 'warn', STOPPED: 'neutral', SCRAPPED: 'neutral' }[s] || 'neutral')
const taskStatusMap = { PENDING: '待执行', COMPLETED: '已完成', OVERDUE: '已逾期' }
const orderStatusMap = { PENDING: '待派单', PROCESSING: '维修中', COMPLETED: '待验收', VERIFIED: '已验收' }

const typeIcon = (t) => ({ ELEVATOR: 'TrendCharts', FIRE: 'Warning', PUMP: 'Odometer', ACCESS: 'Lock', OTHER: 'Setting' }[t] || 'Grid')

const lifeInfo = computed(() => {
  const install = device.value?.installDate
  const age = install ? new Date().getFullYear() - Number(install.slice(0, 4)) : 0
  const life = device.value?.serviceLifeYears || 0
  return { ageYears: Math.max(0, age), lifeYears: life, overdue: age > life, overYears: Math.max(0, age - life) }
})

const warrantyDays = computed(() => {
  if (!device.value?.warrantyEnd) return null
  const end = new Date(device.value.warrantyEnd)
  const now = new Date()
  return Math.floor((end - now) / 86400000)
})

const healthColor = (h) => h >= 80 ? 'var(--ok)' : h >= 60 ? 'var(--warn)' : 'var(--danger)'

// 实时图表
const realtimeEl = ref()
let chart = null
let timer = null

const loadRealtime = async () => {
  const data = await monitorRealtime(device.value.id, 30)
  current.value = data.current || {}
  if (!chart) return
  chart.setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    grid: { left: 34, right: 10, top: 18, bottom: 24 },
    xAxis: { type: 'category', data: data.times, ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: [{
      name: '温度°C',
      type: 'line',
      smooth: true,
      symbol: 'none',
      data: data.temperature,
      lineStyle: { width: 2.5, color: '#d97706' },
      itemStyle: { color: '#d97706' },
      areaStyle: { color: areaGradient('rgba(217,130,43,.12)') }
    }, {
      name: '振动mm/s',
      type: 'line',
      smooth: true,
      symbol: 'none',
      yAxisIndex: 0,
      data: data.vibration,
      lineStyle: { width: 2.5, color: '#96a2b4' },
      itemStyle: { color: '#96a2b4' }
    }]
  })
}

onMounted(async () => {
  const echarts = await loadEcharts()
  const id = route.params.id
  try {
    device.value = await deviceDetail(id)
  } catch (e) {
    ElMessage.error('设备信息加载失败')
    return
  }
  document.title = `${device.value.name} · 设备详情`
  try {
    const [blob, taskList] = await Promise.all([
      deviceQr(id, 240),
      tasksByDevice(id)
    ])
    qrUrl.value = URL.createObjectURL(blob)
    tasks.value = taskList.slice(0, 6)
  } catch (e) {
    ElMessage.warning('二维码/任务加载失败')
  }

  try {
    chart = echarts.init(realtimeEl.value)
    await loadRealtime()
    timer = setInterval(loadRealtime, 5000)
    window.addEventListener('resize', onResize)
  } catch (e) { /* 监测图失败不影响详情 */ }

  // 关联工单（取该设备最近3条）
  try {
    const { repairPage } = await import('@/api')
    const rp = await repairPage({ page: 1, size: 3, deviceId: device.value.id })
    orders.value = rp.records
  } catch (e) { /* ignore */ }
})

// 技术参数
const specMap = computed(() => {
  if (!device.value?.spec) return {}
  try { return JSON.parse(device.value.spec) } catch (e) { return {} }
})
const specKeys = computed(() => Object.keys(specMap.value))

const onResize = () => chart && chart.resize()
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  chart && chart.dispose()
})

const downloadQr = async () => {
  try {
    const blob = await deviceQr(device.value.id, 500)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${device.value.deviceCode}-二维码.png`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('二维码下载失败，请稍后重试')
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 18px; }
.header-title { font-size: 17px; font-weight: 600; margin-right: 10px; }

.detail-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 1100px) { .detail-grid { grid-template-columns: 1fr; } }

.info-card { padding: 20px; }
.info-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.device-icon {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  color: var(--accent);
  border: 1px solid var(--line-2);
  background: var(--bg-deep);
  display: flex;
  align-items: center;
  justify-content: center;
}
.info-name { font-size: 17px; font-weight: 700; }
.info-loc { display: flex; align-items: center; gap: 4px; color: var(--text-2); font-size: 12.5px; margin-top: 5px; }

/* 关键数据条 */
.key-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
.km-item {
  text-align: center;
  padding: 10px 8px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: 10px;
}
.km-label { display: block; font-size: 11px; color: var(--text-3); margin-bottom: 4px; }
.km-value { font-size: 16px; font-weight: 600; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
}
.info-item {
  padding: 10px 14px;
  background: var(--bg-deep);
  border-radius: 6px;
  border: 1px solid var(--line);
}
.info-label { display: block; font-size: 11.5px; color: var(--text-3); margin-bottom: 4px; }
.info-value { font-size: 13.5px; font-weight: 500; }

.remark-box {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: 6px;
  background: rgba(217, 164, 65, .06);
  border: 1px solid rgba(217, 164, 65, .25);
  color: var(--warn);
  font-size: 12.5px;
  display: flex;
  gap: 6px;
  align-items: flex-start;
}

/* 技术参数 */
.spec-box {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--line-2);
}
.spec-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .1em;
  color: var(--text-3);
  margin-bottom: 10px;
  text-transform: uppercase;
}
.spec-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
}
.spec-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: 8px;
}
.spec-label { font-size: 12px; color: var(--text-3); white-space: nowrap; }
.spec-value { font-size: 12.5px; font-weight: 600; text-align: right; }

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14.5px;
  font-weight: 600;
  margin-bottom: 14px;
}
.card-dot {
  width: 14px;
  height: 2px;
  background: var(--accent);
}
.live-badge {
  margin-left: auto;
  font-size: 10px;
  color: var(--ok);
  display: flex;
  align-items: center;
  gap: 5px;
  letter-spacing: 1.5px;
  font-family: var(--font-num);
}
.live-dot { width: 4px; height: 4px; border-radius: 50%; background: var(--ok); }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.metric-item { text-align: center; padding: 10px 4px; background: var(--bg-deep); border: 1px solid var(--line); border-radius: 6px; }
.metric-label { display: block; font-size: 11px; color: var(--text-3); margin-bottom: 4px; }
.metric-value { font-family: var(--font-num); font-size: 17px; font-weight: 700; }

.qr-card { padding: 20px; }
.qr-body { text-align: center; }
.qr-img-wrap {
  width: 200px;
  height: 200px;
  margin: 0 auto;
  padding: 8px;
  background: #fff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qr-img-wrap img { width: 100%; height: 100%; }
.qr-hint { font-size: 11.5px; color: var(--text-3); margin-top: 8px; }

.timeline { padding-left: 6px; }
.tl-item { display: flex; gap: 12px; padding-bottom: 14px; position: relative; }
.tl-item::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 16px;
  bottom: 0;
  width: 1px;
  background: var(--line);
}
.tl-item:last-child::before { display: none; }
.tl-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  z-index: 1;
}
.tl-dot.completed { background: var(--ok); }
.tl-dot.pending { background: var(--accent); }
.tl-dot.overdue { background: var(--danger); }
.tl-body { flex: 1; }
.tl-head { display: flex; align-items: center; gap: 8px; }
.tl-type { font-size: 10.5px; padding: 1px 7px; border-radius: 4px; border: 1px solid var(--line-2); color: var(--text-2); }
.tl-date { font-family: var(--font-num); font-size: 12px; color: var(--text-2); }
.tl-status { margin-left: auto; font-size: 12px; }
.tl-status.completed { color: var(--ok); }
.tl-status.pending { color: var(--accent); }
.tl-status.overdue { color: var(--danger); }
.tl-desc { font-size: 12px; color: var(--text-3); margin-top: 3px; }
.tl-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-3);
  margin-top: 4px;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid var(--line);
}
.order-no { font-family: var(--font-num); font-size: 12px; color: var(--text-2); flex-shrink: 0; }
.order-desc { flex: 1; font-size: 12.5px; color: var(--text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.order-status { font-size: 11.5px; flex-shrink: 0; }
.order-status.pending { color: var(--warn); }
.order-status.processing { color: var(--accent); }
.order-status.completed { color: var(--text-2); }
.order-status.verified { color: var(--ok); }

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}
</style>
