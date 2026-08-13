<template>
  <div class="page">
    <div class="page-head">
      <div class="page-eyebrow">CONTRACTS</div>
      <div class="page-title">维保合同</div>
      <div class="page-desc">维保合同台账 · 到期前 30 天自动提醒 · 逾期红色预警</div>
    </div>

    <!-- 到期提醒横幅 -->
    <div v-if="reminders.length" class="panel remind-banner">
      <div class="remind-icon">
        <el-icon :size="22"><BellFilled /></el-icon>
      </div>
      <div class="remind-body">
        <div class="remind-title">合同到期提醒：{{ reminders.length }} 份合同需要关注</div>
        <div class="remind-list">
          <el-tag
            v-for="r in reminders"
            :key="r.id"
            round
            :type="r.status === 'EXPIRED' ? 'danger' : 'warning'"
            effect="plain"
            style="margin: 0 6px 6px 0"
          >
            {{ r.contractName }} · {{ r.endDate }} {{ r.status === 'EXPIRED' ? '已过期' : '即将到期' }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="panel table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索合同编号 / 名称 / 维保单位" clearable style="width: 260px" @keyup.enter="load" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="合同状态" clearable style="width: 140px" @change="onFilter">
            <el-option label="有效" value="VALID" />
            <el-option label="即将到期" value="EXPIRING" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button v-if="store.isAdmin" type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon>新增合同
          </el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="contractNo" label="合同编号" width="125">
          <template #default="{ row }">
            <span class="code-chip">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="设备类型" width="95">
          <template #default="{ row }">
            <span class="type-label">{{ typeMap[row.deviceType] || row.deviceType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="vendor" label="维保单位" min-width="150" show-overflow-tooltip />
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <span class="stat-num" style="font-size: 12.5px">{{ row.startDate }}</span>
            <span style="color: var(--text-3)"> → </span>
            <span class="stat-num" :style="{ color: row.status === 'EXPIRED' ? 'var(--danger)' : row.status === 'EXPIRING' ? 'var(--warn)' : 'var(--ok)', fontSize: '12.5px' }">
              {{ row.endDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 'EXPIRED'" style="color: var(--danger); font-size: 12px">已过期</span>
            <span v-else class="stat-num" :style="{ color: row.status === 'EXPIRING' ? 'var(--warn)' : 'var(--text-2)' }">
              {{ remainDays(row.endDate) }} 天
            </span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="stat-num">{{ row.amount == null ? '—' : '¥' + Number(row.amount).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="付款方式" width="95" align="center">
          <template #default="{ row }">
            <span class="type-label">{{ payMap[row.payMethod] || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-text">
              <span class="status-dot" :class="contractDot(row.status)"></span>{{ statusMap[row.status] }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.isAdmin" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="info" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="store.isAdmin" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑合同' : '新增合同'" width="600px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="form.contractNo" placeholder="如 HT-2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="如 电梯维保服务合同" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="覆盖设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" style="width: 100%">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维保单位" prop="vendor">
              <el-input v-model="form.vendor" placeholder="维保单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同金额">
              <el-input-number v-model="form.amount" :min="0" :precision="2" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式">
              <el-select v-model="form.payMethod" style="width: 100%">
                <el-option label="年付" value="YEARLY" />
                <el-option label="半年付" value="HALF_YEAR" />
                <el-option label="季付" value="QUARTERLY" />
                <el-option label="月付" value="MONTHLY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contact" placeholder="联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="合同详情" width="480px" align-center>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="合同编号">{{ detail.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="合同名称">{{ detail.contractName }}</el-descriptions-item>
        <el-descriptions-item label="覆盖设备">{{ typeMap[detail.deviceType] }}</el-descriptions-item>
        <el-descriptions-item label="维保单位">{{ detail.vendor }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ detail.startDate }} ~ {{ detail.endDate }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ detail.amount == null ? '—' : '¥' + Number(detail.amount).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ payMap[detail.payMethod] || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contact || '—' }} {{ detail.contactPhone || '' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="status-text"><span class="status-dot" :class="contractDot(detail.status)"></span>{{ statusMap[detail.status] }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contractPage, contractReminders, addContract, updateContract, deleteContract } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }
const statusMap = { VALID: '有效', EXPIRING: '即将到期', EXPIRED: '已过期' }
const payMap = { YEARLY: '年付', HALF_YEAR: '半年付', QUARTERLY: '季付', MONTHLY: '月付' }
const contractDot = (s) => ({ VALID: 'ok', EXPIRING: 'warn', EXPIRED: 'danger' }[s] || 'neutral')
const remainDays = (endDate) => {
  const end = new Date(endDate)
  const now = new Date()
  return Math.max(0, Math.ceil((end - now) / 86400000))
}

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const reminders = ref([])
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const load = async () => {
  loading.value = true
  try {
    const data = await contractPage(query)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载合同失败')
  } finally {
    loading.value = false
  }
}

const onFilter = () => { query.page = 1; load() }

const loadReminders = async () => {
  try {
    reminders.value = await contractReminders()
  } catch (e) { /* ignore */ }
}

// 表单
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  vendor: [{ required: true, message: '请输入维保单位', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }]
}

const openForm = (row) => {
  form.value = row ? { ...row } : {
    contractNo: '', contractName: '', deviceType: 'ELEVATOR', vendor: '',
    startDate: '', endDate: '', amount: null, contact: '', contactPhone: '', remark: ''
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
      await updateContract(form.value.id, form.value)
      ElMessage.success('合同已更新')
    } else {
      await addContract(form.value)
      ElMessage.success('合同新增成功')
    }
    formVisible.value = false
    load()
    loadReminders()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除合同「${row.contractNo}」？`, '删除确认', { type: 'warning' })
  } catch (e) { return }
  await deleteContract(row.id)
  ElMessage.success('已删除')
  load()
  loadReminders()
}

const detailVisible = ref(false)
const detail = ref({})
const viewDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  load()
  loadReminders()
})
</script>

<style scoped>
.remind-banner {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 14px;
  border-left: 2px solid var(--warn);
}
.remind-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: rgba(217, 164, 65, .1);
  color: var(--warn);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.remind-title { font-size: 13.5px; font-weight: 600; margin-bottom: 8px; }
.remind-list { display: flex; flex-wrap: wrap; }
.remind-list :deep(.el-tag) { background: transparent; }

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
.toolbar-right { display: flex; gap: 8px; }

.type-label {
  font-size: 12px;
  color: var(--text-2);
  border: 1px solid var(--line-2);
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
