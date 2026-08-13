<template>
  <div class="page">
    <div class="page-head">
      <div class="page-eyebrow">LIFECYCLE ANALYSIS</div>
      <div class="page-title">使用年限分析</div>
      <div class="page-desc">设备服役年限分布 · 超期风险设备清单与更换建议</div>
    </div>

    <div class="panel mini-stats">
      <div class="mini-stat">
        <span class="mini-dot accent"></span>
        <span class="mini-label">设备总数</span>
        <CountUp class="mini-num" :value="allDevices.length" :size="20" />
      </div>
      <div class="mini-stat">
        <span class="mini-dot neutral"></span>
        <span class="mini-label">平均服役年限</span>
        <CountUp class="mini-num" :value="avgAge" :size="20" :decimals="1" />
        <span class="mini-unit">年</span>
      </div>
      <div class="mini-stat">
        <span class="mini-dot danger"></span>
        <span class="mini-label">超期服役</span>
        <CountUp class="mini-num" :value="lifecycle.overdueCount ?? 0" :size="20" />
        <span class="mini-unit">台</span>
      </div>
      <div class="mini-stat">
        <span class="mini-dot warn"></span>
        <span class="mini-label">最长服役</span>
        <CountUp class="mini-num" :value="maxAge" :size="20" />
        <span class="mini-unit">年</span>
      </div>
      <div class="mini-stat" style="margin-left: auto; border-left: none">
        <el-tag round effect="plain" type="warning">
          <el-icon style="margin-right: 4px"><Warning /></el-icon>建议对超期设备制定更换计划
        </el-tag>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-col" style="flex: 1.2">
        <ChartCard title="使用年限分布" :accent="'#4f46e5'" :option="bucketOption" :height="320" />
      </div>
      <div class="chart-col" style="flex: 1">
        <ChartCard title="安装年份统计" :accent="'#4f46e5'" :option="yearOption" :height="320" />
      </div>
    </div>

    <!-- 健康度排行：名次榜（参考 GoView 排名榜设计） -->
    <div class="panel rank-card" style="margin-top: 14px">
      <div class="card-title">
        <span class="card-dot"></span>
        设备健康度排行
        <span class="count-tag">综合评分 TOP {{ rankList.length }}</span>
      </div>
      <div class="rank-list">
        <div v-for="(r, i) in rankList" :key="r.deviceCode" class="rank-item">
          <span class="rank-no" :class="'rank-no-' + Math.min(i + 1, 4)">{{ i + 1 }}</span>
          <div class="rank-name">
            <span class="rank-code">{{ r.deviceCode }}</span>
            <span class="rank-dev">{{ r.name }}</span>
          </div>
          <div class="rank-bar">
            <div class="rank-fill" :class="rankTone(r.health)" :style="{ width: r.health + '%' }"></div>
          </div>
          <span class="rank-score">{{ r.health }}<small>分</small></span>
          <span class="rank-tag" :class="rankTone(r.health)">{{ rankText(r.health) }}</span>
        </div>
      </div>
    </div>

    <!-- 超期设备（全宽） -->
    <div class="panel table-card" style="margin-top: 14px">
      <div class="card-title">
        <span class="card-dot"></span>
        超期服役设备清单
        <span class="count-tag">{{ lifecycle.overdueDevices?.length || 0 }} 台</span>
      </div>
      <el-table :data="lifecycle.overdueDevices || []" style="width: 100%">
        <el-table-column prop="deviceCode" label="设备编号" width="110">
          <template #default="{ row }">
            <span class="code-chip">{{ row.deviceCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="设备名称" min-width="160" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" round effect="plain">{{ typeMap[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="120" />
        <el-table-column prop="installDate" label="安装日期" width="110" />
        <el-table-column label="已使用" width="90" align="center">
          <template #default="{ row }">
            <span class="stat-num" style="color: var(--warn)">{{ row.ageYears }}年</span>
          </template>
        </el-table-column>
        <el-table-column label="设计年限" width="90" align="center">
          <template #default="{ row }">
            <span class="stat-num">{{ row.lifeYears }}年</span>
          </template>
        </el-table-column>
        <el-table-column label="超期" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="danger" round>{{ row.overYears }} 年</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议" min-width="150">
          <template #default="{ row }">
            <span style="color: var(--text-2); font-size: 12.5px">
              {{ row.overYears >= 3 ? '立即更换' : '加强监测，限期更换' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="处置" width="110" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'SCRAPPED'" link type="danger" size="small" @click="scrapDevice(row)">标记报废</el-button>
            <el-tag v-else size="small" type="info" round>已报废</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/device/${row.id}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CountUp from '@/components/CountUp.vue'
import ChartCard from '@/components/ChartCard.vue'
import { statsLifecycle, deviceList, statsHealthRank, updateDevice } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ramp, axisStyle, tooltipStyle, areaGradient } from '@/utils/echarts'

const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }

const lifecycle = ref({ buckets: [], overdueDevices: [] })
const allDevices = ref([])

const avgAge = computed(() => {
  if (!allDevices.value.length) return 0
  const total = allDevices.value.reduce((s, d) => s + ageOf(d), 0)
  return Math.round(total / allDevices.value.length * 10) / 10
})
const maxAge = computed(() => allDevices.value.reduce((m, d) => Math.max(m, ageOf(d)), 0))

const ageOf = (d) => {
  if (!d.installDate) return 0
  return Math.max(0, new Date().getFullYear() - Number(d.installDate.slice(0, 4)))
}

const bucketOption = ref({})
const yearOption = ref({})
const rankList = ref([])

// 健康档位（参考 GoView 配色：翠绿/橘橙/红）
const rankTone = (h) => h >= 80 ? 'tone-good' : h >= 60 ? 'tone-warn' : 'tone-risk'
const rankText = (h) => h >= 80 ? '优秀' : h >= 60 ? '关注' : '风险'

// 超期设备处置：标记报废（闭环：年限分析 -> 处置决策）
const scrapDevice = async (row) => {
  try {
    await ElMessageBox.confirm(`确认将「${row.name}」标记为报废？报废后设备将不再参与巡检计划。`, '报废确认', {
      type: 'warning', confirmButtonText: '确认报废', cancelButtonText: '取消'
    })
  } catch (e) { return }
  try {
    await updateDevice(row.id, { ...row, status: 'SCRAPPED', remark: (row.remark ? row.remark + '；' : '') + '超期服役，已报废' })
    ElMessage.success('已标记报废')
    loadData()
  } catch (e) { /* 拦截器已提示 */ }
}

const loadData = async () => {
  let life, devices
  try {
    const res = await Promise.all([statsLifecycle(), deviceList()])
    life = res[0]
    devices = res[1]
  } catch (e) {
    ElMessage.error('分析数据加载失败，请刷新重试')
    return
  }
  lifecycle.value = life
  allDevices.value = devices

  // 健康度排行（TOP 10 名次榜）
  try {
    rankList.value = (await statsHealthRank(10)) || []
  } catch (e) { /* 健康度排行加载失败不阻塞页面 */ }

  bucketOption.value = {
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: life.buckets.map(b => b.name), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [{
      type: 'bar',
      barWidth: 40,
      data: life.buckets.map((b, i) => ({
        value: b.value,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: ramp[i % ramp.length]
        }
      })),
      label: { show: true, position: 'top', color: '#9aa4b2', fontSize: 12, fontFamily: 'Bahnschrift' }
    }]
  }

  // 按安装年份统计
  const yearMap = {}
  devices.forEach(d => {
    if (!d.installDate) return
    const y = d.installDate.slice(0, 4)
    yearMap[y] = (yearMap[y] || 0) + 1
  })
  const years = Object.keys(yearMap).sort()
  yearOption.value = {
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: years, ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: years.map(y => yearMap[y]),
      lineStyle: { width: 2, color: '#4f46e5' },
      itemStyle: { color: '#4f46e5' },
      areaStyle: { color: areaGradient('rgba(59,110,245,.12)') }
    }]
  }
}

onMounted(loadData)
</script>

<style scoped>
.mini-stats {
  display: flex;
  align-items: center;
  padding: 0 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.mini-stat {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 13px 18px;
  border-left: 1px solid var(--line);
}
.mini-stat:first-child { border-left: none; }
.mini-dot { width: 5px; height: 5px; border-radius: 50%; }
.mini-label { font-size: 12px; color: var(--text-3); letter-spacing: .5px; }
.mini-num { font-weight: 600; }
.mini-unit { font-size: 12px; color: var(--text-3); }

.chart-row { display: flex; gap: 14px; }
.chart-col { min-width: 0; }

.table-card { padding: 16px 20px; }
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 600;
  margin-bottom: 14px;
}
.card-dot {
  width: 14px;
  height: 2px;
  background: var(--accent);
}
.count-tag {
  margin-left: auto;
  font-size: 12px;
  color: var(--danger);
  border: 1px solid rgba(212, 93, 93, .35);
  padding: 1px 8px;
  border-radius: 4px;
  font-family: var(--font-num);
}
.code-chip {
  font-family: var(--font-num);
  font-size: 12px;
  color: var(--text-2);
  background: var(--bg-deep);
  border: 1px solid var(--line-2);
  padding: 2px 7px;
  border-radius: 4px;
}
.rank-card { padding: 16px 20px; }
.rank-list { margin-top: 6px; }
.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  border-radius: 8px;
  transition: background .12s ease;
}
.rank-item:hover { background: var(--panel-2); }
.rank-no {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  font-weight: 700;
  color: #8a8a94;
  flex-shrink: 0;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
}
.rank-no-1 { background: rgba(232, 163, 61, .16); color: #b97a17; }
.rank-no-2 { background: rgba(140, 144, 156, .16); color: #7a7e8a; }
.rank-no-3 { background: rgba(201, 138, 75, .16); color: #a06a2f; }
.rank-no-4 { background: var(--panel-3); color: #a0a0aa; }
.rank-name { display: flex; align-items: baseline; gap: 10px; min-width: 0; flex: 0 0 auto; }
.rank-code { font-family: "Inter", "HarmonyOS Sans SC", sans-serif; font-size: 12.5px; font-weight: 700; color: var(--text-1); }
.rank-dev { font-size: 12.5px; color: var(--text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; }
.rank-bar { flex: 1; height: 8px; background: #f0f0f2; border-radius: 5px; overflow: hidden; min-width: 60px; }
.rank-fill { height: 100%; border-radius: 5px; transition: width .5s ease; }
.rank-fill.tone-good { background: linear-gradient(90deg, #a5b4fc, #4f46e5); }
.rank-fill.tone-warn { background: linear-gradient(90deg, #e8c489, var(--warn)); }
.rank-fill.tone-risk { background: linear-gradient(90deg, #e8a2a2, var(--danger)); }
.rank-score {
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  width: 52px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.rank-score small { font-size: 10.5px; color: var(--text-3); font-weight: 400; }
.rank-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 6px;
  flex-shrink: 0;
}
.rank-tag.tone-good { background: rgba(79, 70, 229, .1); color: #4f46e5; }
.rank-tag.tone-warn { background: rgba(217, 164, 65, .12); color: var(--warn); }
.rank-tag.tone-risk { background: rgba(212, 93, 93, .12); color: var(--danger); }
</style>
