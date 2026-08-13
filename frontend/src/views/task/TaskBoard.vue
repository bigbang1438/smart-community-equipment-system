<template>
  <div class="page">
    <div class="page-head d-head">
      <div>
        <div class="page-eyebrow">{{ isInspect ? 'INSPECTION' : 'MAINTENANCE' }}</div>
        <div class="page-title">{{ typeText }}管理</div>
        <div class="page-desc">{{ isInspect ? '按设备周期自动排程 · 扫码打卡执行 · 异常可一键转报修' : '定期保养计划 · 维保记录留档' }}</div>
      </div>
      <div class="d-actions">
        <el-button @click="handleRefreshOverdue"><el-icon><Refresh /></el-icon>刷新逾期</el-button>
        <el-button type="primary" @click="openGenerate"><el-icon><MagicStick /></el-icon>自动生成计划</el-button>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="panel mini-stats">
      <div class="mini-stat progress-cell">
        <div class="mini-ring" :style="{ background: `conic-gradient(var(--accent) ${todayPct}%, #e9edf3 0)` }">
          <div class="mini-ring-inner">
            <span class="mini-ring-num stat-num">{{ todayPct }}<small>%</small></span>
          </div>
        </div>
        <div class="progress-text">
          <span class="mini-label">今日完成</span>
          <span class="mini-num" style="font-size: 13px">{{ stats.todayDone ?? 0 }} / {{ stats.todayTotal ?? 0 }} 项</span>
        </div>
      </div>
      <div class="mini-stat">
        <span class="mini-dot accent"></span>
        <span class="mini-label">待执行</span>
        <CountUp class="mini-num" :value="stats.pendingCount ?? 0" :size="20" />
      </div>
      <div class="mini-stat">
        <span class="mini-dot danger"></span>
        <span class="mini-label">逾期任务</span>
        <CountUp class="mini-num" :value="stats.overdueCount ?? 0" :size="20" />
      </div>
      <div class="mini-stat" style="margin-left: auto; border-left: none">
        <el-tag round effect="plain">{{ typeText }}周期由设备台账自动生成</el-tag>
      </div>
    </div>

    <div class="panel table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索任务编号 / 设备" clearable style="width: 240px" @keyup.enter="load" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="任务状态" clearable style="width: 130px" @change="onFilter">
            <el-option label="待执行" value="PENDING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="taskCode" label="任务编号" width="170">
          <template #default="{ row }">
            <span class="code-chip">{{ row.taskCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="设备编号" width="105" />
        <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceLocation" label="位置" min-width="110" show-overflow-tooltip />
        <el-table-column prop="planDate" label="计划日期" width="110" />
        <el-table-column prop="executor" label="执行人" width="90">
          <template #default="{ row }">{{ row.executor || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <span class="status-text">
              <span class="status-dot" :class="taskDot(row.status)"></span>{{ statusMap[row.status] }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.result" class="result-text" :class="row.result.toLowerCase()">
              {{ row.result === 'NORMAL' ? '正常' : '异常' }}
            </span>
            <span v-else style="color: var(--text-3)">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'OVERDUE'" link type="primary" size="small" @click="openCheck(row)">{{ row.status === 'OVERDUE' ? '补卡' : '打卡完成' }}</el-button>
            <el-button v-else link type="info" size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="load"
        />
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <el-dialog v-model="checkVisible" title="任务打卡" width="460px" align-center>
      <div class="check-device">
        <div class="check-icon">
          <el-icon :size="22"><Checked /></el-icon>
        </div>
        <div>
          <div class="check-name">{{ checkingTask?.deviceName }}</div>
          <div class="check-code">{{ checkingTask?.deviceCode }} · {{ checkingTask?.planDate }}</div>
        </div>
      </div>
      <el-form label-width="80px" style="margin-top: 14px">
        <el-form-item label="巡检结果" required>
          <el-radio-group v-model="checkForm.result">
            <el-radio-button value="NORMAL">正常</el-radio-button>
            <el-radio-button value="ABNORMAL">异常</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="检查项">
          <div class="check-items">
            <div
              v-for="it in checkForm.items"
              :key="it.name"
              class="ci-pill"
              :class="{ bad: it.result === '异常' }"
              @click="toggleItem(it)"
            >
              <span class="ci-dot" :class="{ bad: it.result === '异常' }"></span>{{ it.name }}
              <span class="ci-state">{{ it.result === '异常' ? '异常' : '正常' }}</span>
            </div>
          </div>
          <div class="ci-hint">点击切换正常/异常 · 任一异常将自动标记为异常结果</div>
        </el-form-item>
        <el-form-item label="打卡位置">
          <el-input v-model="checkForm.location" placeholder="如 3号楼1单元 电梯厅" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="checkForm.remark" type="textarea" :rows="2" placeholder="检查情况说明（异常时必填）" />
        </el-form-item>
      </el-form>
      <el-alert v-if="checkForm.result === 'ABNORMAL'" type="warning" :closable="false" show-icon
        title="检测到异常，建议同时发起报修工单" style="margin-top: 4px" />
      <template #footer>
        <el-button @click="checkVisible = false">取消</el-button>
        <el-button type="primary" :loading="checking" @click="submitCheck">确认打卡</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="440px" align-center>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="任务编号">{{ detail.taskCode }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ detail.taskType === 'INSPECT' ? '巡检' : '保养' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ detail.deviceName }} ({{ detail.deviceCode }})</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.deviceLocation }}</el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ detail.planDate }}</el-descriptions-item>
        <el-descriptions-item label="执行人">{{ detail.executor || '—' }}</el-descriptions-item>
        <el-descriptions-item label="打卡时间">{{ detail.checkTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ detail.result ? (detail.result === 'NORMAL' ? '正常' : '异常') : '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.checkItems" label="检查明细">
          <div class="dl-items">
            <span
              v-for="it in parseItems(detail.checkItems)"
              :key="it.name"
              class="dl-pill"
              :class="{ bad: it.result === '异常' }"
            >{{ it.name }} <em>{{ it.result === '异常' ? '异常' : '✓' }}</em></span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 生成计划 -->
    <el-dialog v-model="genVisible" title="自动生成计划" width="460px" align-center>
      <el-form label-width="100px">
        <el-form-item label="任务类型">
          <span class="type-label">{{ typeText }}</span>
        </el-form-item>
        <el-form-item label="生成范围">
          <el-input-number v-model="genForm.horizonDays" :min="7" :max="180" :step="7" />
          <span style="margin-left: 10px; color: var(--text-3); font-size: 12.5px">天内（按设备周期自动补齐）</span>
        </el-form-item>
        <el-form-item label="执行人">
          <el-input v-model="genForm.executor" placeholder="默认由扫码打卡时填写" />
        </el-form-item>
        <el-form-item label="设备范围">
          <el-select v-model="genForm.deviceIds" multiple collapse-tags collapse-tags-tooltip placeholder="不选则为全部在用设备" style="width: 100%">
            <el-option v-for="d in devices" :key="d.id" :label="`${d.deviceCode} ${d.name}`" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  taskPage, taskToday, generateTask, refreshOverdue, checkTask,
  deviceList
} from '@/api'
import { useUserStore } from '@/stores/user'
import CountUp from '@/components/CountUp.vue'

const router = useRouter()
const props = defineProps({ type: { type: String, default: 'INSPECT' } })
const isInspect = computed(() => props.type === 'INSPECT')
const typeText = computed(() => isInspect.value ? '巡检' : '保养')

const store = useUserStore()
const statusMap = { PENDING: '待执行', COMPLETED: '已完成', OVERDUE: '已逾期' }
const taskDot = (s) => ({ PENDING: 'accent', COMPLETED: 'ok', OVERDUE: 'danger' }[s] || 'neutral')

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const todayPct = computed(() => {
  const total = stats.value.todayTotal || 0
  if (!total) return 0
  return Math.round((stats.value.todayDone || 0) / total * 100)
})

const load = async () => {
  loading.value = true
  try {
    const data = await taskPage({ ...query, type: props.type })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const onFilter = () => { query.page = 1; load() }

const loadStats = async () => {
  try {
    stats.value = (await taskToday()) || {}
  } catch (e) { /* 统计加载失败不阻塞列表 */ }
}

// 打卡
const checkVisible = ref(false)
const checking = ref(false)
const checkingTask = ref(null)
const checkForm = ref({ result: 'NORMAL', location: '', remark: '', items: [] })

// 检查项模板（按设备类型）
const CHECK_TEMPLATES = {
  ELEVATOR: ['轿厢照明与通风', '轿厢按钮面板', '平层精度', '开关门运行', '五方对讲', '钢丝绳磨损'],
  FIRE: ['报警主机运行', '探测器巡检', '手报按钮', '消防泵压力', '消防广播'],
  PUMP: ['泵体运行声音', '密封渗漏', '压力表读数', '电机温升', '控制柜指示'],
  ACCESS: ['读卡识别', '道闸起落', '摄像头画面', '门体闭合'],
  OTHER: ['设备运行声音', '温度检查', '接线端子', '外壳清洁']
}

const toggleItem = (it) => {
  it.result = it.result === '正常' ? '异常' : '正常'
  const anyBad = checkForm.value.items.some(i => i.result === '异常')
  checkForm.value.result = anyBad ? 'ABNORMAL' : 'NORMAL'
}

const parseItems = (json) => {
  try { return JSON.parse(json) } catch (e) { return [] }
}

const openCheck = (row) => {
  checkingTask.value = row
  const tpl = CHECK_TEMPLATES[row.deviceType] || CHECK_TEMPLATES.OTHER
  checkForm.value = {
    result: 'NORMAL',
    location: row.deviceLocation || '',
    remark: '',
    items: tpl.map(n => ({ name: n, result: '正常', detail: '' }))
  }
  checkVisible.value = true
}

const submitCheck = async () => {
  if (checkForm.value.result === 'ABNORMAL' && !checkForm.value.remark.trim()) {
    ElMessage.warning('检测到异常，请填写备注说明')
    return
  }
  checking.value = true
  try {
    await checkTask({
      taskId: checkingTask.value.id,
      result: checkForm.value.result,
      remark: checkForm.value.remark,
      location: checkForm.value.location,
      checkItems: JSON.stringify(checkForm.value.items),
      executor: store.user?.realName || store.user?.username
    })
    ElMessage.success('打卡成功')
    checkVisible.value = false
    load()
    loadStats()
    if (checkForm.value.result === 'ABNORMAL') {
      ElMessageBox.confirm('本次巡检发现异常，是否立即为该设备发起报修？', '发现异常', {
        type: 'warning',
        confirmButtonText: '去报修',
        cancelButtonText: '暂不'
      }).then(() => {
        router.push({ path: '/repair', query: { deviceId: checkingTask.value.deviceId } })
      }).catch(() => {})
    }
  } finally {
    checking.value = false
  }
}

// 详情
const detailVisible = ref(false)
const detail = ref({})
const viewDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

// 生成计划
const genVisible = ref(false)
const generating = ref(false)
const devices = ref([])
const genForm = ref({ horizonDays: 30, executor: '', deviceIds: [] })

const openGenerate = async () => {
  try {
    devices.value = await deviceList()
    genVisible.value = true
  } catch (e) { /* 拦截器已提示 */ }
}

const submitGenerate = async () => {
  generating.value = true
  try {
    const data = await generateTask({
      type: props.type,
      horizonDays: genForm.value.horizonDays,
      deviceIds: genForm.value.deviceIds,
      executor: genForm.value.executor
    })
    ElMessage.success(`已自动生成 ${data.created} 条${typeText.value}任务`)
    genVisible.value = false
    load()
    loadStats()
  } catch (e) { /* 拦截器已提示 */ } finally {
    generating.value = false
  }
}

const handleRefreshOverdue = async () => {
  try {
    const data = await refreshOverdue()
    ElMessage.success(`已更新 ${data.updated} 条逾期状态`)
    load()
    loadStats()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.d-head { display: flex; align-items: flex-end; justify-content: space-between; }
.page-desc { font-size: 12.5px; color: var(--text-3); margin-top: 5px; }
.d-actions { display: flex; gap: 8px; }

.mini-stats {
  display: flex;
  align-items: center;
  padding: 0 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  overflow: hidden;
}
.mini-stat {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-left: 1px solid var(--line);
}
.mini-stat:first-child { border-left: none; }
.mini-dot { width: 6px; height: 6px; border-radius: 50%; }
.mini-label { font-size: 12.5px; color: var(--text-2); }
.mini-num { font-weight: 600; }

.progress-cell { gap: 12px; }
.mini-ring {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background .6s ease;
}
.mini-ring-inner {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mini-ring-num { font-size: 13px; font-weight: 600; color: var(--text-1); }
.mini-ring-num small { font-size: 9px; color: var(--text-3); }
.progress-text { display: flex; flex-direction: column; gap: 2px; }

.table-card { padding: 16px 20px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-right { display: flex; gap: 8px; }

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.result-text { font-size: 12.5px; }
.result-text.normal { color: var(--ok); }
.result-text.abnormal { color: var(--danger); }

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }

.check-device {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  background: var(--bg-deep);
  border: 1px solid var(--line);
}
.check-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  color: var(--accent);
  border: 1px solid var(--accent-line);
  background: var(--accent-dim);
  display: flex;
  align-items: center;
  justify-content: center;
}
.check-name { font-weight: 600; font-size: 15px; }
.check-code { font-family: var(--font-num); font-size: 12px; color: var(--text-2); margin-top: 3px; }
/* 检查项 */
.check-items { display: flex; flex-wrap: wrap; gap: 8px; }
.ci-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--line-2);
  background: var(--panel-2);
  font-size: 12.5px;
  color: var(--text-2);
  cursor: pointer;
  transition: all .15s ease;
  user-select: none;
}
.ci-pill:hover { border-color: var(--accent); }
.ci-pill.bad {
  background: var(--danger-dim);
  border-color: rgba(224, 82, 82, .35);
  color: var(--danger);
}
.ci-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--ok); }
.ci-dot.bad { background: var(--danger); }
.ci-state { font-size: 10.5px; opacity: .75; font-weight: 600; }
.ci-hint { font-size: 11px; color: var(--text-3); margin-top: 8px; }

.dl-items { display: flex; flex-wrap: wrap; gap: 6px; }
.dl-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--panel-2);
  color: var(--text-2);
}
.dl-pill em { font-style: normal; color: var(--ok); font-weight: 700; }
.dl-pill.bad { border-color: rgba(224, 82, 82, .35); background: var(--danger-dim); color: var(--danger); }
.dl-pill.bad em { color: var(--danger); }
</style>
