<template>
  <div class="page">
    <div class="page-head">
      <div class="page-eyebrow">WORK ORDERS</div>
      <div class="page-title">报修工单</div>
      <div class="page-desc">报修登记 → 派单 → 维修 → 验收，全流程闭环管理</div>
    </div>

    <div class="panel mini-stats">
      <div class="mini-stat">
        <span class="mini-dot warn"></span>
        <span class="mini-label">待派单</span>
        <CountUp class="mini-num" :value="countBy('PENDING')" :size="20" />
      </div>
      <div class="mini-stat">
        <span class="mini-dot accent"></span>
        <span class="mini-label">维修中</span>
        <CountUp class="mini-num" :value="countBy('PROCESSING')" :size="20" />
      </div>
      <div class="mini-stat">
        <span class="mini-dot neutral"></span>
        <span class="mini-label">待验收</span>
        <CountUp class="mini-num" :value="countBy('COMPLETED')" :size="20" />
      </div>
      <div class="mini-stat">
        <span class="mini-dot ok"></span>
        <span class="mini-label">已验收</span>
        <CountUp class="mini-num" :value="countBy('VERIFIED')" :size="20" />
      </div>
      <div class="mini-stat" style="margin-left: auto; border-left: none">
        <el-button type="primary" size="small" @click="openCreate">
          <el-icon><Warning /></el-icon>发起报修
        </el-button>
      </div>
    </div>

    <div class="panel table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索工单号 / 报修人 / 设备" clearable style="width: 240px" @keyup.enter="load" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="工单状态" clearable style="width: 130px" @change="onFilter">
            <el-option label="待派单" value="PENDING" />
            <el-option label="维修中" value="PROCESSING" />
            <el-option label="待验收" value="COMPLETED" />
            <el-option label="已验收" value="VERIFIED" />
          </el-select>
          <el-select v-model="query.level" placeholder="紧急程度" clearable style="width: 130px" @change="onFilter">
            <el-option label="紧急" value="HIGH" />
            <el-option label="一般" value="MEDIUM" />
            <el-option label="轻微" value="LOW" />
          </el-select>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderCode" label="工单号" width="185">
          <template #default="{ row }">
            <span class="code-chip">{{ row.orderCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceName }} <span class="sub">{{ row.deviceCode }}</span></template>
        </el-table-column>
        <el-table-column prop="faultDesc" label="故障描述" min-width="170" show-overflow-tooltip />
        <el-table-column prop="reporter" label="报修人" width="90" />
        <el-table-column label="紧急程度" width="95" align="center">
          <template #default="{ row }">
            <span class="level-text" :class="(row.level || '').toLowerCase()">{{ levelMap[row.level] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <span class="status-text">
              <span class="status-dot" :class="orderDot(row.status)"></span>{{ statusMap[row.status] }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="维修人" width="80">
          <template #default="{ row }">{{ row.assignee || '—' }}</template>
        </el-table-column>
        <el-table-column label="费用" width="90" align="right">
          <template #default="{ row }">
            <span class="stat-num">{{ row.cost == null ? '—' : '¥' + row.cost }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报修时间" width="155">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' && canAssign" link type="primary" size="small" @click="openAssign(row)">派单</el-button>
            <el-button v-if="row.status === 'PROCESSING' && canAssign" link type="success" size="small" @click="openFinish(row)">完成</el-button>
            <el-button v-if="row.status === 'COMPLETED' && store.isAdmin" link type="warning" size="small" @click="doVerify(row)">验收</el-button>
            <el-button link type="info" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="store.isAdmin" link type="danger" size="small" @click="doDelete(row)">删除</el-button>
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

    <!-- 报修 -->
    <el-dialog v-model="createVisible" title="发起报修" width="540px" align-center>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="报修设备" prop="deviceId">
          <el-select v-model="createForm.deviceId" filterable placeholder="选择设备" style="width: 100%">
            <el-option v-for="d in devices" :key="d.id" :label="`${d.deviceCode} ${d.name}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修人" prop="reporter">
          <el-input v-model="createForm.reporter" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="createForm.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="紧急程度" prop="level">
          <el-radio-group v-model="createForm.level">
            <el-radio-button value="HIGH">紧急</el-radio-button>
            <el-radio-button value="MEDIUM">一般</el-radio-button>
            <el-radio-button value="LOW">轻微</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDesc">
          <el-input v-model="createForm.faultDesc" type="textarea" :rows="3" placeholder="请详细描述故障现象" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" title="维修派单" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="工单号">
          <span class="code-chip">{{ assignForm.orderCode }}</span>
        </el-form-item>
        <el-form-item label="维修人员" required>
          <el-select v-model="assignForm.assignee" filterable placeholder="选择维修人员" style="width: 100%">
            <el-option v-for="u in maintainers" :key="u.username" :label="u.realName" :value="u.realName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 完成 -->
    <el-dialog v-model="finishVisible" title="提交维修结果" width="480px" align-center>
      <el-form label-width="90px">
        <el-form-item label="维修结果" required>
          <el-input v-model="finishForm.fixResult" type="textarea" :rows="3" placeholder="维修内容与结果" />
        </el-form-item>
        <el-form-item label="维修费用">
          <el-input-number v-model="finishForm.cost" :min="0" :precision="2" :step="50" style="width: 180px" />
          <span style="margin-left: 10px; color: var(--text-3); font-size: 12.5px">元</span>
        </el-form-item>
        <el-form-item label="维修工时">
          <el-input-number v-model="finishForm.fixHours" :min="0" :precision="1" :step="0.5" style="width: 180px" />
          <span style="margin-left: 10px; color: var(--text-3); font-size: 12.5px">小时</span>
        </el-form-item>
      </el-form>
      <el-alert type="success" :closable="false" show-icon title="完成后设备将自动恢复为「运行中」状态" />
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" :loading="finishing" @click="submitFinish">提交完成</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="520px" align-center>
      <div class="od-head">
        <span class="code-chip">{{ detail.orderCode }}</span>
        <span class="status-text">
          <span class="status-dot" :class="orderDot(detail.status)"></span>{{ statusMap[detail.status] }}
        </span>
      </div>

      <!-- 状态时间轴 -->
      <div class="od-timeline">
        <div class="od-node" :class="{ done: detail.createTime, current: detail.status === 'PENDING' }">
          <span class="od-dot"></span>
          <div class="od-content">
            <div class="od-title">报修登记</div>
            <div class="od-sub">{{ detail.reporter }} {{ detail.phone || '' }} · {{ (detail.createTime || '').replace('T', ' ') }}</div>
          </div>
        </div>
        <div class="od-node" :class="{ done: detail.assignTime, current: detail.status === 'PROCESSING' }">
          <span class="od-dot"></span>
          <div class="od-content">
            <div class="od-title">维修派单</div>
            <div class="od-sub">{{ detail.assignee ? `维修人：${detail.assignee}` : '待派单' }} · {{ (detail.assignTime || '').replace('T', ' ') || '未派单' }}</div>
          </div>
        </div>
        <div class="od-node" :class="{ done: detail.finishTime, current: detail.status === 'COMPLETED' }">
          <span class="od-dot"></span>
          <div class="od-content">
            <div class="od-title">维修完成</div>
            <div class="od-sub">{{ detail.fixResult || '待提交维修结果' }} · {{ (detail.finishTime || '').replace('T', ' ') || '未完成' }}</div>
            <div v-if="detail.cost != null" class="od-cost">费用 ¥{{ detail.cost }}</div>
          </div>
        </div>
        <div class="od-node" :class="{ done: detail.verifyTime, current: detail.status === 'VERIFIED' }">
          <span class="od-dot"></span>
          <div class="od-content">
            <div class="od-title">验收归档</div>
            <div class="od-sub">{{ detail.verifyTime ? '验收通过，工单闭环' : '待验收' }} · {{ (detail.verifyTime || '').replace('T', ' ') || '未验收' }}</div>
          </div>
        </div>
      </div>

      <div class="od-info">
        <div class="od-info-row"><span class="od-label">报修设备</span><span>{{ detail.deviceName }} ({{ detail.deviceCode }}) · {{ detail.deviceLocation }}</span></div>
        <div class="od-info-row"><span class="od-label">故障描述</span><span>{{ detail.faultDesc }}</span></div>
        <div class="od-info-row"><span class="od-label">紧急程度</span><span class="level-text" :class="detail.level.toLowerCase()">{{ levelMap[detail.level] }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  repairPage, repairStats, createRepair, assignRepair, finishRepair, verifyRepair,
  deleteRepair, deviceList, maintainerList
} from '@/api'
import { useUserStore } from '@/stores/user'
import CountUp from '@/components/CountUp.vue'

const store = useUserStore()
const statusMap = { PENDING: '待派单', PROCESSING: '维修中', COMPLETED: '待验收', VERIFIED: '已验收' }
const levelMap = { HIGH: '紧急', MEDIUM: '一般', LOW: '轻微' }
const orderDot = (s) => ({ PENDING: 'warn', PROCESSING: 'accent', COMPLETED: 'neutral', VERIFIED: 'ok' }[s] || 'neutral')
const canAssign = computed(() => ['ADMIN', 'MAINTAINER'].includes(store.role))

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '', level: '' })

const countBy = (s) => stats.value[s] || 0
const stats = ref({})

const load = async () => {
  loading.value = true
  try {
    const [data, st] = await Promise.all([repairPage(query), repairStats()])
    rows.value = data.records
    total.value = data.total
    stats.value = st || {}
  } catch (e) {
    ElMessage.error('加载工单失败')
  } finally {
    loading.value = false
  }
}

const onFilter = () => { query.page = 1; load() }

// 报修
const createVisible = ref(false)
const creating = ref(false)
const createRef = ref()
const devices = ref([])
const createForm = ref({ deviceId: null, reporter: '', phone: '', level: 'MEDIUM', faultDesc: '' })
const createRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  reporter: [{ required: true, message: '请输入报修人', trigger: 'blur' }],
  faultDesc: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

const openCreate = async () => {
  try {
    devices.value = await deviceList()
    createForm.value.reporter = store.user?.realName || ''
    createVisible.value = true
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  }
}

const submitCreate = async () => {
  try {
    await createRef.value.validate()
  } catch (e) { return }
  creating.value = true
  try {
    await createRepair(createForm.value)
    ElMessage.success('报修提交成功，等待派单')
    createVisible.value = false
    load()
  } finally {
    creating.value = false
  }
}

// 派单
const assignVisible = ref(false)
const assigning = ref(false)
const maintainers = ref([])
const assignForm = ref({ id: null, orderCode: '', assignee: '' })

const openAssign = async (row) => {
  assignForm.value = { id: row.id, orderCode: row.orderCode, assignee: '' }
  try {
    maintainers.value = await maintainerList()
    assignVisible.value = true
  } catch (e) {
    ElMessage.error('加载维修人员失败')
  }
}

const submitAssign = async () => {
  if (!assignForm.value.assignee) {
    ElMessage.warning('请选择维修人员')
    return
  }
  assigning.value = true
  try {
    await assignRepair(assignForm.value.id, { assignee: assignForm.value.assignee })
    ElMessage.success('派单成功')
    assignVisible.value = false
    load()
  } finally {
    assigning.value = false
  }
}

// 完成
const finishVisible = ref(false)
const finishing = ref(false)
const finishForm = ref({ id: null, fixResult: '', cost: 0, fixHours: 0 })

const openFinish = (row) => {
  finishForm.value = { id: row.id, fixResult: '', cost: 0, fixHours: 0 }
  finishVisible.value = true
}

const submitFinish = async () => {
  if (!finishForm.value.fixResult.trim()) {
    ElMessage.warning('请填写维修结果')
    return
  }
  finishing.value = true
  try {
    await finishRepair(finishForm.value.id, {
      fixResult: finishForm.value.fixResult,
      cost: finishForm.value.cost,
      fixHours: finishForm.value.fixHours
    })
    ElMessage.success('维修完成，待管理员验收')
    finishVisible.value = false
    load()
  } finally {
    finishing.value = false
  }
}

// 验收 / 删除 / 详情
const doVerify = async (row) => {
  try {
    await ElMessageBox.confirm(`确认验收工单「${row.orderCode}」？`, '验收确认', { type: 'info' })
  } catch (e) { return }
  try {
    await verifyRepair(row.id)
    ElMessage.success('验收通过，工单已闭环')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

const doDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除工单「${row.orderCode}」？`, '删除确认', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteRepair(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

const detailVisible = ref(false)
const detail = ref({})
const viewDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  load()
  // 从巡检异常跳转：预选设备并打开报修弹窗
  const q = useRoute().query
  if (q.deviceId) {
    openCreate().then(() => {
      createForm.value.deviceId = Number(q.deviceId)
    })
  }
})
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

.table-card { padding: 16px 20px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.sub { font-size: 11px; color: var(--text-3); }

.level-text { font-size: 12px; }
.level-text.high { color: var(--danger); }
.level-text.medium { color: var(--warn); }
.level-text.low { color: var(--text-3); }

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }

/* 工单详情时间轴 */
.od-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.od-timeline { padding-left: 6px; margin-bottom: 16px; }
.od-node {
  position: relative;
  display: flex;
  gap: 14px;
  padding-bottom: 18px;
}
.od-node:last-child { padding-bottom: 0; }
.od-node::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: -2px;
  width: 1px;
  background: var(--line);
}
.od-node:last-child::before { display: none; }
.od-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--line-2);
  box-shadow: 0 0 0 1px var(--line-2);
  flex-shrink: 0;
  margin-top: 3px;
  z-index: 1;
}
.od-node.done .od-dot {
  background: var(--accent);
  box-shadow: 0 0 0 1px var(--accent), 0 0 0 4px var(--accent-dim);
}
.od-node.current .od-dot {
  background: var(--warn);
  box-shadow: 0 0 0 1px var(--warn), 0 0 0 4px rgba(217, 130, 43, .12);
  animation: pulse-dot 1.6s infinite;
}
.od-content { flex: 1; min-width: 0; }
.od-title { font-size: 13.5px; font-weight: 600; }
.od-sub { font-size: 12px; color: var(--text-3); margin-top: 3px; }
.od-cost { font-size: 12px; color: var(--accent); margin-top: 3px; font-family: var(--font-num); }

.od-info {
  padding: 12px 14px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: 10px;
}
.od-info-row {
  display: flex;
  gap: 14px;
  padding: 4px 0;
  font-size: 12.5px;
  color: var(--text-1);
}
.od-label {
  width: 70px;
  flex-shrink: 0;
  color: var(--text-3);
}
</style>
