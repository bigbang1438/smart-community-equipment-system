<template>
  <div class="page dash">
    <!-- 页头 -->
    <div class="page-head d-head">
      <div>
        <div class="page-eyebrow">Overview</div>
        <div class="page-title">设备运营总览</div>
        <div class="page-desc">社区设备设施全生命周期管理 · 实时数据</div>
      </div>
      <div class="d-date">
        <div class="d-time stat-num">{{ timeText }}</div>
        <div class="d-day">{{ dateText }}</div>
      </div>
    </div>

    <!-- KPI 行（带迷你趋势） -->
    <div class="kpi-row">
      <div class="panel kpi" @click="$router.push('/device')">
        <div class="kpi-top">
          <span class="kpi-label">设备总数</span>
          <span class="kpi-icon"><el-icon :size="14"><Grid /></el-icon></span>
        </div>
        <div class="kpi-value stat-num">{{ overview.deviceTotal ?? 0 }}</div>
        <div class="kpi-bottom">
          <span class="kpi-foot">运行 {{ overview.deviceRunning ?? 0 }} 台</span>
          <div ref="sparkType" class="kpi-spark"></div>
        </div>
      </div>

      <div class="panel kpi" @click="$router.push('/monitor')">
        <div class="kpi-top">
          <span class="kpi-label">设备运行率</span>
          <span class="kpi-icon"><el-icon :size="14"><Odometer /></el-icon></span>
        </div>
        <div class="kpi-value stat-num">{{ runningRate }}<small>%</small></div>
        <div class="kpi-bottom">
          <span class="kpi-foot">异常 {{ overview.deviceFault ?? 0 }} 台</span>
          <div ref="sparkStatus" class="kpi-spark"></div>
        </div>
      </div>

      <div class="panel kpi" @click="$router.push('/repair')">
        <div class="kpi-top">
          <span class="kpi-label">待处理工单</span>
          <span class="kpi-icon"><el-icon :size="14"><Warning /></el-icon></span>
        </div>
        <div class="kpi-value stat-num" :class="{ danger: (overview.pendingOrders || 0) > 0 }">{{ overview.pendingOrders ?? 0 }}</div>
        <div class="kpi-bottom">
          <span class="kpi-foot">本月费用 ¥{{ overview.monthCost ?? 0 }}</span>
          <div ref="sparkOrder" class="kpi-spark"></div>
        </div>
      </div>

      <div class="panel kpi" @click="$router.push('/task/inspect')">
        <div class="kpi-top">
          <span class="kpi-label">逾期任务</span>
          <span class="kpi-icon"><el-icon :size="14"><AlarmClock /></el-icon></span>
        </div>
        <div class="kpi-value stat-num" :class="{ danger: (taskStats.overdueCount || 0) > 0 }">{{ taskStats.overdueCount ?? 0 }}</div>
        <div class="kpi-bottom">
          <span class="kpi-foot">待执行 {{ taskStats.pendingCount ?? 0 }} 项 · 点此查看</span>
          <div ref="sparkTask" class="kpi-spark"></div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div class="panel chart-box">
        <div class="cb-head">
          <div>
            <div class="cb-title">设备状态统计</div>
            <div class="cb-sub">运行 {{ overview.deviceRunning ?? 0 }} 台 · 占比 {{ runningRate }}%</div>
          </div>
        </div>
        <div ref="statusEl" class="cb-body"></div>
      </div>

      <div class="panel chart-box">
        <div class="cb-head">
          <div>
            <div class="cb-title">巡检完成率</div>
            <div class="cb-sub">近 14 天 · 总完成率 {{ completionRate }}%</div>
          </div>
        </div>
        <div ref="completionEl" class="cb-body"></div>
      </div>

      <div class="panel chart-box">
        <div class="cb-head">
          <div>
            <div class="cb-title">维修工单与费用趋势</div>
            <div class="cb-sub">近 6 个月 · 已验收 {{ verifiedCount }} 单</div>
          </div>
        </div>
        <div ref="trendEl" class="cb-body"></div>
      </div>

      <div class="panel chart-box">
        <div class="cb-head">
          <div>
            <div class="cb-title">设备使用年限分析</div>
            <div class="cb-sub" :class="{ warn: lifeInfo.overdueCount > 0 }">超龄 {{ lifeInfo.overdueCount }} 台 · 建议优先更新</div>
          </div>
        </div>
        <div ref="lifeEl" class="cb-body"></div>
      </div>
    </div>

    <!-- 今日巡检速览 -->
    <div class="panel quick-bar">
      <div class="qb-item">
        <span class="qb-label">今日计划</span>
        <b class="stat-num">{{ taskStats.todayTotal ?? 0 }}</b>
        <span class="qb-unit">项</span>
      </div>
      <div class="qb-item">
        <span class="qb-label">已完成</span>
        <b class="stat-num ok">{{ taskStats.todayDone ?? 0 }}</b>
        <span class="qb-unit">项</span>
      </div>
      <div class="qb-item">
        <span class="qb-label">待执行</span>
        <b class="stat-num warn">{{ taskStats.todayPending ?? 0 }}</b>
        <span class="qb-unit">项</span>
      </div>
      <div class="qb-item">
        <span class="qb-label">逾期任务</span>
        <b class="stat-num danger">{{ taskStats.overdueCount ?? 0 }}</b>
        <span class="qb-unit">项</span>
      </div>
      <div class="qb-progress">
        <span class="qb-progress-label">今日巡检完成率</span>
        <div class="qb-bar"><span :style="{ width: todayPct + '%' }"></span></div>
        <b class="stat-num">{{ todayPct }}%</b>
      </div>
    </div>

    <!-- 今日任务明细：待执行 + 已完成 -->
    <div class="panel today-tasks">
      <div class="tt-head">
        <span class="tt-title"><span class="tt-dot"></span>今日任务明细</span>
        <span class="tt-sub">共 {{ todayTasks.length }} 项 · 完成 {{ todayDoneCount }} 项</span>
        <router-link to="/task/inspect" class="tt-more">查看全部 →</router-link>
      </div>
      <div v-if="todayTasks.length" class="tt-list">
        <div v-for="t in todayTasks" :key="t.id" class="tt-item" @click="$router.push(`/device/${t.deviceId}`)">
          <span class="tt-code">{{ t.taskCode }}</span>
          <span class="tt-name">{{ t.deviceName }}</span>
          <span class="tt-loc"><el-icon :size="12"><Location /></el-icon>{{ t.deviceLocation }}</span>
          <span class="tt-state" :class="t.status === 'PENDING' ? 'pending' : 'done'">
            {{ t.status === 'PENDING' ? '待执行' : '已完成 ' + (t.checkTime ? t.checkTime.slice(5, 16) : '') }}
          </span>
        </div>
      </div>
      <div v-else class="tt-empty">
        <el-icon :size="16" color="var(--ok)"><CircleCheckFilled /></el-icon>
        今日暂无巡检任务
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {
  statsOverview, statsDeviceStatus, statsDeviceType, statsTaskCompletion,
  statsRepairTrend, statsLifecycle, taskToday, statsCostTrend, taskPage
} from '@/api'
import { axisStyle, tooltipStyle, areaGradient, accent } from '@/utils/echarts'

// echarts 动态加载
let echartsMod = null
const loadEcharts = async () => {
  if (!echartsMod) echartsMod = await import('echarts')
  return echartsMod
}

const overview = ref({})
const taskStats = ref({})
const lifeInfo = ref({ overdueCount: 0, buckets: [] })
const completionRate = ref(0)
const verifiedCount = ref(0)
const todayTasks = ref([])
const todayDoneCount = computed(() => todayTasks.value.filter(t => t.status === 'COMPLETED').length)

// 今天日期（与后端 LocalDate 对齐）
const todayStr = (() => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
})()

const runningRate = computed(() => overview.value.runningRate ?? 0)
const todayPct = computed(() => {
  const total = taskStats.value.todayTotal || 0
  return total ? Math.round((taskStats.value.todayDone || 0) / total * 100) : 0
})

/* 时钟 */
const timeText = ref('')
const dateText = ref('')
let clockTimer = null
const tick = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  timeText.value = `${p(d.getHours())}:${p(d.getMinutes())}`
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  dateText.value = `${d.getFullYear()} 年 ${p(d.getMonth() + 1)} 月 ${p(d.getDate())} 日 · 星期${week}`
}

/* 图表 refs */
const sparkType = ref()
const sparkStatus = ref()
const sparkOrder = ref()
const sparkTask = ref()
const statusEl = ref()
const completionEl = ref()
const trendEl = ref()
const lifeEl = ref()
const charts = []

const miniOpt = (echarts, data, color, bar = false) => ({
  grid: { left: 0, right: 0, top: 2, bottom: 0 },
  xAxis: { type: 'category', show: false, data: data.map((_, i) => i) },
  yAxis: { type: 'value', show: false, min: 0 },
  series: [{
    type: bar ? 'bar' : 'line',
    data,
    smooth: !bar,
    symbol: 'none',
    barWidth: '55%',
    lineStyle: { width: 1.5, color },
    itemStyle: { color },
    areaStyle: bar ? undefined : { color: areaGradient(color + '33') }
  }]
})

const renderAll = async () => {
  const echarts = await loadEcharts()
  if (!charts.length) {
    charts.push(
      echarts.init(sparkType.value),
      echarts.init(sparkStatus.value),
      echarts.init(sparkOrder.value),
      echarts.init(sparkTask.value),
      echarts.init(statusEl.value),
      echarts.init(completionEl.value),
      echarts.init(trendEl.value),
      echarts.init(lifeEl.value)
    )
  }

  // —— 数据 ——
  const [st, tp, comp, trend, cost, life] = await Promise.all([
    statsDeviceStatus(), statsDeviceType(), statsTaskCompletion(14, 'INSPECT'),
    statsRepairTrend(6), statsCostTrend(6), statsLifecycle()
  ])
  lifeInfo.value = life
  const doneTotal = comp.dones.reduce((a, b) => a + Number(b), 0)
  const planTotal = comp.totals.reduce((a, b) => a + Number(b), 0)
  completionRate.value = planTotal ? Math.round(doneTotal / planTotal * 100) : 0
  const vSum = Object.values(trend.verified || {}).reduce((a, b) => a + b, 0)
  verifiedCount.value = vSum

  // —— KPI 迷你图 ——
  const typeNames = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }
  const stNames = { RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停用', SCRAPPED: '报废' }
  charts[0].setOption(miniOpt(echarts, tp.map(t => t.value), accent, true), true)
  charts[1].setOption(miniOpt(echarts, st.map(s => s.value), '#16a34a', true), true)
  const months = [...new Set([...Object.keys(trend.pending), ...Object.keys(trend.processing), ...Object.keys(trend.completed), ...Object.keys(trend.verified)])].sort()
  const monthTotal = months.map(m =>
    (trend.pending[m] || 0) + (trend.processing[m] || 0) + (trend.completed[m] || 0) + (trend.verified[m] || 0))
  charts[2].setOption(miniOpt(echarts, monthTotal, accent), true)
  charts[3].setOption(miniOpt(echarts, comp.dones.map(Number), '#d97706'), true)

  // —— 1. 设备状态统计 ——
  charts[4].setOption({
    tooltip: { ...tooltipStyle, trigger: 'item', formatter: '{b}：{c} 台 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#9d9da8', fontSize: 11 }, itemWidth: 8, itemHeight: 8, icon: 'circle', itemGap: 14 },
    series: [{
      type: 'pie',
      radius: ['52%', '70%'],
      center: ['50%', '43%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 600, color: '#18181b', formatter: '{b}\n{c}台' } },
      data: st.map(s => ({ name: stNames[s.name] || s.name, value: s.value })),
      color: ['#4f46e5', '#dc2626', '#d97706', '#a1a1aa', '#6366f1']
    }]
  }, true)

  // —— 2. 巡检完成率 ——
  charts[5].setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    grid: { left: 36, right: 12, top: 20, bottom: 24 },
    xAxis: { type: 'category', data: comp.dates.map(d => d.slice(5)), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [
      { name: '应完成', type: 'bar', barWidth: 9, data: comp.totals.map(Number), itemStyle: { color: 'rgba(79,70,229,.1)', borderRadius: [4, 4, 0, 0] } },
      {
        name: '已完成', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: comp.dones.map(Number),
        lineStyle: { width: 2, color: accent },
        itemStyle: { color: accent, borderColor: '#fff', borderWidth: 2 },
        areaStyle: { color: areaGradient('rgba(79,70,229,.18)') }
      }
    ]
  }, true)

  // —— 3. 工单趋势（数量 + 费用双轴） ——
  const toArr = (m) => months.map(x => m[x] || 0)
  // 后端 cost-trend 返回数组 [{month, amount}]，转 map 后按月份取值
  const costMap = Object.fromEntries((cost || []).map(x => [x.month, Number(x.amount) || 0]))
  const costArr = months.map(m => costMap[m] || 0)
  charts[6].setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    legend: { top: 0, right: 0, textStyle: { color: '#9d9da8', fontSize: 11 }, icon: 'rect', itemWidth: 10, itemHeight: 3, itemGap: 12 },
    grid: { left: 36, right: 52, top: 28, bottom: 24 },
    xAxis: { type: 'category', data: months, ...axisStyle },
    yAxis: [
      { type: 'value', minInterval: 1, ...axisStyle },
      { type: 'value', name: '费用(元)', nameTextStyle: { color: '#9d9da8', fontSize: 10 }, splitLine: { show: false }, axisLabel: { color: '#9d9da8', fontSize: 10 } }
    ],
    series: [
      { name: '已验收', type: 'bar', stack: 't', barWidth: 16, data: toArr(trend.verified), itemStyle: { color: '#3730a3' } },
      { name: '已完成', type: 'bar', stack: 't', data: toArr(trend.completed), itemStyle: { color: '#4f46e5' } },
      { name: '维修中', type: 'bar', stack: 't', data: toArr(trend.processing), itemStyle: { color: '#818cf8' } },
      { name: '待派单', type: 'bar', stack: 't', data: toArr(trend.pending), itemStyle: { color: '#e0e7ff', borderRadius: [4, 4, 0, 0] } },
      { name: '维修费用', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5, data: costArr, lineStyle: { width: 2, color: '#d97706' }, itemStyle: { color: '#d97706' } }
    ]
  }, true)

  // —— 4. 年限分析 ——
  charts[7].setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps) => `${ps[0].name}<br/>设备数量：<b>${ps[0].value}</b> 台` },
    grid: { left: 40, right: 16, top: 20, bottom: 24 },
    xAxis: { type: 'category', data: life.buckets.map(b => b.name), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [{
      type: 'bar', barWidth: 26,
      data: life.buckets.map(b => b.value),
      itemStyle: {
        borderRadius: [5, 5, 0, 0],
        color: (p) => {
          const c = ['#c7d2fe', '#818cf8', '#4f46e5', '#3730a3', '#18181b']
          return c[p.dataIndex]
        }
      },
      label: { show: true, position: 'top', color: '#60606a', fontSize: 12, fontWeight: 600 }
    }]
  }, true)
}

const loadAll = async () => {
  try {
    const [ov, ts] = await Promise.all([
      statsOverview(), taskToday()
    ])
    overview.value = ov || {}
    taskStats.value = ts || {}
    await renderAll()
  } catch (e) {
    console.error('dashboard load error', e)
  }
}

// 今日任务明细（待执行在前、已完成按打卡时间，最多 8 条）
const loadTodayTasks = async () => {
  try {
    const data = await taskPage({ type: 'INSPECT', planDate: todayStr, page: 1, size: 8 })
    todayTasks.value = data.records || []
  } catch (e) { /* ignore */ }
}

const resizeAll = () => charts.forEach(c => c && c.resize())

let timer = null
onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  loadAll()
  loadTodayTasks()
  timer = setInterval(async () => {
    try {
      const [ov, ts] = await Promise.all([
        statsOverview(), taskToday()
      ])
      overview.value = ov || {}
      taskStats.value = ts || {}
      loadTodayTasks()
    } catch (e) { /* ignore */ }
  }, 30000)
  window.addEventListener('resize', resizeAll)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(timer)
  window.removeEventListener('resize', resizeAll)
  charts.forEach(c => c && c.dispose())
})
</script>

<style scoped>
.dash { padding-top: 22px; }

.d-head { display: flex; align-items: flex-end; justify-content: space-between; }
.d-date { text-align: right; }
.d-time { font-size: 26px; font-weight: 700; letter-spacing: 1px; color: var(--text-1); }
.d-day { font-size: 12px; color: var(--text-3); margin-top: 3px; }

/* KPI */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .kpi-row { grid-template-columns: 1fr; } }
.kpi {
  padding: 16px 18px 12px;
  cursor: pointer;
  transition: box-shadow .18s ease, transform .18s ease;
}
.kpi:hover { box-shadow: var(--shadow-hover); transform: translateY(-1px); }
.kpi-top { display: flex; align-items: center; justify-content: space-between; }
.kpi-label { font-size: 12.5px; color: var(--text-3); letter-spacing: .02em; }
.kpi-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--panel-2);
  color: var(--text-3);
  display: flex;
  align-items: center;
  justify-content: center;
}
.kpi-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 6px;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}
.kpi-value small { font-size: 14px; color: var(--text-3); font-weight: 400; margin-left: 1px; }
.kpi-value.danger { color: var(--danger); }
.kpi-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 8px;
  gap: 8px;
}
.kpi-foot { font-size: 12px; color: var(--text-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kpi-spark { width: 78px; height: 30px; flex-shrink: 0; }

/* 图表区 */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
.chart-box { padding: 16px 20px; }
.cb-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 4px; }
.cb-title { font-size: 14.5px; font-weight: 600; color: var(--text-1); }
.cb-sub { font-size: 11.5px; color: var(--text-3); margin-top: 2px; }
.cb-sub.warn { color: var(--warn); }
.cb-body { height: 300px; }

/* 今日巡检速览 */
.quick-bar {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  flex-wrap: wrap;
  gap: 8px 0;
}
.qb-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 0 24px;
  border-right: 1px solid var(--line);
}
.qb-item:first-child { padding-left: 0; }
.qb-label { font-size: 12px; color: var(--text-3); }
.qb-unit { font-size: 12px; color: var(--text-3); }
.qb-progress {
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 24px;
}
.qb-progress-label { font-size: 12px; color: var(--text-3); white-space: nowrap; }
.qb-bar {
  flex: 1;
  height: 8px;
  background: var(--panel-3);
  border-radius: 5px;
  overflow: hidden;
}
.qb-bar span {
  display: block;
  height: 100%;
  border-radius: 5px;
  background: linear-gradient(90deg, #4f46e5, #818cf8);
  transition: width .5s ease;
}

/* 今日待执行任务 */
.today-tasks { margin-top: 14px; padding: 14px 20px; }
.tt-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.tt-title { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 600; color: var(--text-1); }
.tt-dot { width: 8px; height: 8px; border-radius: 3px; background: var(--accent); }
.tt-more { font-size: 12px; color: var(--accent); text-decoration: none; font-weight: 600; }
.tt-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 18px; }
@media (max-width: 900px) { .tt-list { grid-template-columns: 1fr; } }
.tt-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--panel-2);
  cursor: pointer;
  transition: background .12s ease;
  min-width: 0;
}
.tt-item:hover { background: var(--accent-dim); }
.tt-code {
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  font-size: 11px;
  color: var(--accent);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 5px;
  padding: 1px 6px;
  flex-shrink: 0;
}
.tt-name { font-size: 13px; font-weight: 600; color: var(--text-1); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-loc { font-size: 11.5px; color: var(--text-3); display: flex; align-items: center; gap: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-plan { margin-left: auto; font-size: 11px; color: var(--text-3); flex-shrink: 0; }
.tt-sub { font-size: 11.5px; color: var(--text-3); margin-left: auto; }
.tt-more { font-size: 12px; color: var(--accent); text-decoration: none; font-weight: 600; margin-left: 14px; }
.tt-state {
  margin-left: auto;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 6px;
}
.tt-state.pending { background: rgba(217, 164, 65, .12); color: var(--warn); }
.tt-state.done { background: rgba(18, 165, 128, .1); color: var(--ok); }
.tt-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ok);
  background: rgba(18, 165, 128, .06);
  border-radius: 8px;
}
</style>
