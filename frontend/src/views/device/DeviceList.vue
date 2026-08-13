<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-head d-head">
      <div>
        <div class="page-eyebrow">DEVICE LEDGER</div>
        <div class="page-title">设备台账</div>
        <div class="page-desc">社区核心设备资产档案 · 二维码标签 · 全生命周期追踪</div>
      </div>
      <div class="d-actions">
        <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增设备</el-button>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="panel stat-bar">
      <div class="stat-cell" v-for="s in miniStats" :key="s.label" @click="quickFilter(s)">
        <span class="mini-dot" :class="s.dot"></span>
        <span class="mini-label">{{ s.label }}</span>
        <CountUp class="mini-num" :value="s.value" :size="22" />
      </div>
      <div class="stat-cell type-cell">
        <div class="type-bar">
          <div
            v-for="t in typeStats"
            :key="t.type"
            class="type-seg"
            :style="{ width: t.pct + '%', background: t.color }"
            :title="`${typeMap[t.type]} ${t.count} 台`"
          ></div>
        </div>
        <div class="type-legend">
          <span v-for="t in typeStats" :key="t.type" class="tl-item">
            <i :style="{ background: t.color }"></i>{{ typeMap[t.type] }} {{ t.count }}
          </span>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="panel table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索名称 / 编号 / 厂家 / 位置" clearable style="width: 250px" @keyup.enter="load" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.type" placeholder="设备类型" clearable style="width: 120px" @change="onFilter">
            <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="query.status" placeholder="设备状态" clearable style="width: 120px" @change="onFilter">
            <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-button type="primary" plain @click="load"><el-icon><Search /></el-icon>查询</el-button>
        </div>
        <div class="view-switch">
          <button class="vs-btn" :class="{ on: view === 'card' }" @click="view = 'card'">
            <el-icon :size="14"><Grid /></el-icon>卡片
          </button>
          <button class="vs-btn" :class="{ on: view === 'table' }" @click="view = 'table'">
            <el-icon :size="14"><List /></el-icon>列表
          </button>
        </div>
      </div>

      <!-- 卡片视图 -->
      <div v-if="view === 'card'" class="card-grid" v-loading="loading">
        <div v-for="row in rows" :key="row.id" class="panel dev-card" @click="$router.push(`/device/${row.id}`)">
          <div class="dev-head">
            <span class="dev-status">
              <span class="status-dot" :class="statusDot(row.status)"></span>{{ statusMap[row.status] }}
            </span>
            <span class="type-label">{{ typeMap[row.type] }}</span>
          </div>
          <div class="dev-name">{{ row.name }}</div>
          <div class="dev-code">{{ row.deviceCode }} · {{ row.model || '—' }}</div>
          <div class="dev-loc">
            <el-icon :size="12"><Location /></el-icon>{{ row.location }}
          </div>

          <div class="dev-meta">
            <div class="dm-row">
              <span class="dm-label">服役年限</span>
              <span class="dm-value" :class="{ overdue: lifeOf(row) > row.serviceLifeYears }">
                {{ lifeOf(row) }} / {{ row.serviceLifeYears }} 年
              </span>
            </div>
            <div class="life-bar">
              <div
                class="life-fill"
                :class="{ overdue: lifeOf(row) > row.serviceLifeYears }"
                :style="{ width: Math.min(100, lifeOf(row) / row.serviceLifeYears * 100) + '%' }"
              ></div>
            </div>
            <div class="dm-row">
              <span class="dm-label">巡检周期</span>
              <span class="dm-value">{{ row.inspectCycle }} 天</span>
              <span class="dm-label" style="margin-left: auto">建档 {{ (row.createTime || '').slice(0, 10) }}</span>
            </div>
          </div>

          <div class="dev-actions" @click.stop>
            <el-button link type="primary" size="small" @click="$router.push(`/device/${row.id}`)">详情</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="showQr(row)"><el-icon :size="13"><QrCode /></el-icon>二维码</el-button>
            <el-button v-if="store.isAdmin" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!rows.length && !loading" description="没有符合条件的设备" />
      </div>

      <!-- 表格视图 -->
      <el-table v-else :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="deviceCode" label="设备编号" width="110">
          <template #default="{ row }">
            <span class="code-chip">{{ row.deviceCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="设备名称" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="device-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <span class="type-label">{{ typeMap[row.type] || row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" min-width="130" show-overflow-tooltip />
        <el-table-column prop="installDate" label="安装日期" width="110" />
        <el-table-column label="服役年限" width="150">
          <template #default="{ row }">
            <div class="tbl-life">
              <div class="tbl-life-track">
                <div
                  class="tbl-life-fill"
                  :class="{ overdue: lifeOf(row) > row.serviceLifeYears }"
                  :style="{ width: Math.min(100, lifeOf(row) / row.serviceLifeYears * 100) + '%' }"
                ></div>
              </div>
              <span class="stat-num">{{ lifeOf(row) }}</span>
              <span class="tbl-life-total">/{{ row.serviceLifeYears }}年</span>
              <el-tooltip v-if="lifeOf(row) > row.serviceLifeYears" content="已超期服役" placement="top">
                <el-icon style="color: var(--danger); margin-left: 3px"><WarningFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-text">
              <span class="status-dot" :class="statusDot(row.status)"></span>{{ statusMap[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="225" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/device/${row.id}`)">详情</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="showQr(row)"><el-icon :size="13"><QrCode /></el-icon>二维码</el-button>
            <el-button v-if="store.isAdmin" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[12, 24, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="load"
        />
      </div>
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑设备' : '新增设备'" width="640px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备编号" prop="deviceCode">
              <el-input v-model="form.deviceCode" placeholder="如 DT-006" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="name">
              <el-input v-model="form.name" placeholder="如 6号楼客梯" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="form.model" placeholder="规格型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产厂家">
              <el-input v-model="form.manufacturer" placeholder="生产厂家" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装位置" prop="location">
              <el-input v-model="form.location" placeholder="如 3号楼1单元" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装日期">
              <el-date-picker v-model="form.installDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保修截止">
              <el-date-picker v-model="form.warrantyEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用年限(年)" prop="serviceLifeYears">
              <el-input-number v-model="form.serviceLifeYears" :min="1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检周期(天)">
              <el-input-number v-model="form.inspectCycle" :min="1" :max="180" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保养周期(天)">
              <el-input-number v-model="form.maintainCycle" :min="30" :max="365" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术参数">
              <el-input v-model="form.spec" type="textarea" :rows="2" placeholder='JSON 格式，如 {"额定载重":"1050kg","额定速度":"1.75m/s"}' />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 二维码 -->
    <el-dialog v-model="qrVisible" title="设备二维码标签" width="400px" align-center>
      <div class="qr-box">
        <div class="qr-img-wrap">
          <img v-if="qrUrl" :src="qrUrl" alt="二维码" />
          <div v-else class="qr-loading">生成中...</div>
        </div>
        <div class="qr-info">
          <div class="qr-name">{{ qrDevice?.name }}</div>
          <div class="qr-code">编号：{{ qrDevice?.deviceCode }}</div>
          <div class="qr-tip">手机扫码即可进入移动端巡检打卡</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="qrVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadQr">下载标签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { devicePage, deviceList, addDevice, updateDevice, deleteDevice, deviceQr } from '@/api'
import { useUserStore } from '@/stores/user'
import CountUp from '@/components/CountUp.vue'

const store = useUserStore()

const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }
const statusMap = { RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停用', SCRAPPED: '报废' }
const statusDot = (s) => ({
  RUNNING: 'ok',
  FAULT: 'danger',
  REPAIRING: 'warn',
  STOPPED: 'neutral',
  SCRAPPED: 'neutral'
}[s] || 'neutral')

const view = ref('card')
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 12, keyword: '', type: '', status: '' })

const lifeOf = (row) => {
  if (!row || !row.installDate) return 0
  const y = Number(row.installDate.slice(0, 4))
  if (!Number.isFinite(y)) return 0
  const age = new Date().getFullYear() - y
  return Math.max(0, age)
}

// 全部设备（用于统计与类型分布）
const allDevices = ref([])
const loadAll = async () => {
  try { allDevices.value = await deviceList() } catch (e) { /* ignore */ }
}

const miniStats = computed(() => {
  const all = allDevices.value
  return [
    { label: '设备总数', value: all.length, dot: 'accent', filter: null },
    { label: '运行中', value: all.filter(r => r.status === 'RUNNING').length, dot: 'ok', filter: { status: 'RUNNING' } },
    { label: '故障/维修', value: all.filter(r => ['FAULT', 'REPAIRING'].includes(r.status)).length, dot: 'danger', filter: { status: 'FAULT' } },
    { label: '停用/报废', value: all.filter(r => ['STOPPED', 'SCRAPPED'].includes(r.status)).length, dot: 'neutral', filter: { status: 'STOPPED' } },
    { label: '超期服役', value: all.filter(r => lifeOf(r) > r.serviceLifeYears).length, dot: 'warn', filter: null }
  ]
})

const typeStats = computed(() => {
  const all = allDevices.value
  if (!all.length) return []
  return Object.entries(typeMap).map(([k, v]) => {
    const count = all.filter(r => r.type === k).length
    return { type: k, count, pct: Math.round(count / all.length * 100), color: {
      ELEVATOR: '#4f46e5', FIRE: '#3730a3', PUMP: '#16a34a', ACCESS: '#818cf8', OTHER: '#a1a1aa'
    }[k] }
  }).filter(t => t.count > 0)
})

const quickFilter = (s) => {
  if (!s.filter) return
  query.status = s.filter.status
  query.type = ''
  query.page = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const data = await devicePage(query)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const onFilter = () => { query.page = 1; load() }

// 表单
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  location: [{ required: true, message: '请输入安装位置', trigger: 'blur' }],
  serviceLifeYears: [{ required: true, message: '请输入使用年限', trigger: 'blur' }]
}

const openForm = (row) => {
  form.value = row ? { ...row } : {
    deviceCode: '', name: '', type: 'ELEVATOR', model: '', manufacturer: '',
    location: '', installDate: '', warrantyEnd: '', serviceLifeYears: 10,
    inspectCycle: 15, maintainCycle: 90, status: 'RUNNING', remark: ''
  }
  formVisible.value = true
}

const save = async () => {
  try {
    await formRef.value.validate()
  } catch (e) { return }
  saving.value = true
  try {
    if (form.value.id) {
      await updateDevice(form.value.id, form.value)
      ElMessage.success('设备信息已更新')
    } else {
      await addDevice(form.value)
      ElMessage.success('设备新增成功，可打印二维码标签')
    }
    formVisible.value = false
    load()
    loadAll()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteDevice(row.id)
    ElMessage.success('已删除')
    load()
    loadAll()
  } catch (e) { /* 拦截器已提示 */ }
}

// 二维码
const qrVisible = ref(false)
const qrDevice = ref(null)
const qrUrl = ref('')

const showQr = async (row) => {
  qrDevice.value = row
  qrVisible.value = true
  qrUrl.value = ''
  try {
    const blob = await deviceQr(row.id, 280)
    qrUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    ElMessage.error('二维码生成失败，请稍后重试')
  }
}

const downloadQr = async () => {
  try {
    const blob = await deviceQr(qrDevice.value.id, 500)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${qrDevice.value.deviceCode}-二维码.png`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('二维码下载失败，请稍后重试')
  }
}

onMounted(() => {
  load()
  loadAll()
})
</script>

<style scoped>
.d-head { display: flex; align-items: flex-end; justify-content: space-between; }
.page-desc { font-size: 12.5px; color: var(--text-3); margin-top: 5px; }
.d-actions { display: flex; gap: 8px; }

/* 统计条 */
.stat-bar {
  display: flex;
  align-items: center;
  padding: 0 10px;
  margin-bottom: 16px;
  overflow: hidden;
  flex-wrap: wrap;
}
.stat-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 14px 18px;
  border-left: 1px solid var(--line);
  cursor: pointer;
  transition: background .15s ease;
}
.stat-cell:first-child { border-left: none; }
.stat-cell:hover { background: var(--panel-2); }
.mini-dot { width: 6px; height: 6px; border-radius: 50%; }
.mini-label { font-size: 12.5px; color: var(--text-2); }
.mini-num { font-weight: 600; }

.type-cell { flex: 1; min-width: 280px; padding: 10px 18px; display: block; }
.type-bar { display: flex; height: 6px; border-radius: 3px; overflow: hidden; background: var(--panel-3); }
.type-seg { height: 100%; transition: width .5s ease; }
.type-legend {
  display: flex;
  gap: 14px;
  margin-top: 7px;
  flex-wrap: wrap;
}
.tl-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-3);
  font-family: var(--font-num);
}
.tl-item i { width: 8px; height: 8px; border-radius: 2px; display: inline-block; }

/* 工具栏 */
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
.view-switch { display: flex; gap: 2px; }
.vs-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--line-2);
  background: #fff;
  color: var(--text-3);
  font-size: 12.5px;
  cursor: pointer;
  transition: all .15s ease;
}
.vs-btn:first-child { border-radius: 7px 0 0 7px; }
.vs-btn:last-child { border-radius: 0 7px 7px 0; }
.vs-btn.on {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

/* 卡片视图 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  min-height: 200px;
}
.dev-card { padding: 16px 18px 12px; cursor: pointer; }
.dev-card:hover { transform: translateY(-2px); }
.dev-head { display: flex; justify-content: space-between; align-items: center; }
.dev-status { display: inline-flex; align-items: center; gap: 7px; font-size: 12px; color: var(--text-2); }
.dev-name { font-size: 15px; font-weight: 600; margin-top: 10px; }
.dev-code { font-family: var(--font-num); font-size: 11.5px; color: var(--text-3); margin-top: 2px; }
.dev-loc {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-2);
  margin-top: 6px;
}
.dev-meta { margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--line); }
.dm-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 6px;
}
.dm-value { color: var(--text-2); font-family: var(--font-num); }
.dm-value.overdue { color: var(--danger); }
.life-bar { height: 4px; border-radius: 2px; background: var(--panel-3); overflow: hidden; margin-bottom: 8px; }
.life-fill { height: 100%; border-radius: 2px; background: var(--accent); transition: width .5s ease; }
.life-fill.overdue { background: var(--danger); }
.dev-actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  padding-top: 8px;
  margin-top: 8px;
  border-top: 1px solid var(--line);
}

/* 表格服役年限条 */
.tbl-life { display: flex; align-items: center; gap: 6px; }
.tbl-life-track { width: 56px; height: 4px; border-radius: 2px; background: var(--panel-3); overflow: hidden; }
.tbl-life-fill { height: 100%; background: var(--accent); border-radius: 2px; }
.tbl-life-fill.overdue { background: var(--danger); }
.tbl-life-total { font-size: 11px; color: var(--text-3); }

.device-name { font-weight: 500; }

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }

.qr-box { text-align: center; }
.qr-img-wrap {
  width: 200px;
  height: 200px;
  margin: 0 auto;
  border-radius: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: center;
}
.qr-img-wrap img { width: 100%; height: 100%; }
.qr-loading { color: var(--text-3); }
.qr-name { font-size: 15px; font-weight: 600; margin-top: 14px; }
.qr-code { font-family: var(--font-num); color: var(--text-2); margin-top: 4px; }
.qr-tip { font-size: 12px; color: var(--text-3); margin-top: 8px; }
</style>
