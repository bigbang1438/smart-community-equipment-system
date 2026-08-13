<template>
  <div class="page">
    <div class="page-head">
      <div class="page-eyebrow">USERS</div>
      <div class="page-title">用户管理</div>
      <div class="page-desc">系统用户 · 角色权限（管理员 / 维保 / 巡检）</div>
    </div>

    <div class="panel table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索用户名 / 姓名" clearable style="width: 240px" @keyup.enter="load" @clear="onFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增用户</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="avatar">{{ row.realName.slice(0, 1) }}</div>
              <div>
                <div class="user-name">{{ row.realName }}</div>
                <div class="user-login">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <span class="role-tag" :class="row.role.toLowerCase()">{{ roleMap[row.role] }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-text">
              <span class="status-dot" :class="row.status === 1 ? 'ok' : 'neutral'"></span>{{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.id !== 1" link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="row.id !== 1" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="460px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="85px">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" placeholder="登录名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'" :prop="form.id ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="系统管理员" value="ADMIN" />
            <el-option label="维保人员" value="MAINTAINER" />
            <el-option label="巡检人员" value="INSPECTOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userPage, addUser, updateUser, deleteUser } from '@/api'

const roleMap = { ADMIN: '系统管理员', MAINTAINER: '维保人员', INSPECTOR: '巡检人员' }

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '' })

const load = async () => {
  loading.value = true
  try {
    const data = await userPage(query)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

const onFilter = () => { query.page = 1; load() }

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const openForm = (row) => {
  form.value = row ? { ...row, password: '' } : {
    username: '', realName: '', password: '123456', role: 'INSPECTOR', phone: '', status: 1
  }
  formVisible.value = true
}

const save = async () => {
  try {
    await formRef.value.validate()
  } catch (e) { return }
  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.password) delete payload.password
    if (form.value.id) {
      await updateUser(form.value.id, payload)
      ElMessage.success('用户已更新')
    } else {
      await addUser(payload)
      ElMessage.success('用户新增成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await updateUser(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已禁用该账号' : '已启用该账号')
    load()
  } catch (e) { /* ignore */ }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.realName}」？`, '删除确认', { type: 'warning' })
  } catch (e) { return }
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.table-card { padding: 18px 20px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left { display: flex; gap: 10px; align-items: center; }

.user-cell { display: flex; align-items: center; gap: 12px; }
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--bg-deep);
  border: 1px solid var(--line-2);
  color: var(--accent);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-name { font-weight: 600; font-size: 13.5px; }
.user-login { font-size: 11.5px; color: var(--text-3); font-family: var(--font-num); }

.role-tag { padding: 2px 10px; border-radius: 4px; font-size: 12px; border: 1px solid var(--line-2); color: var(--text-2); }
.role-tag.admin { color: var(--danger); border-color: rgba(212, 93, 93, .4); }
.role-tag.maintainer { color: var(--accent); border-color: var(--accent-line); }
.role-tag.inspector { color: var(--ok); border-color: rgba(63, 182, 139, .4); }

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
