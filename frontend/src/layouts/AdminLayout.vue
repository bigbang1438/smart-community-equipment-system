<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-mark">智</div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-name">智慧社区</div>
          <div class="brand-sub">EQUIPMENT LIFECYCLE</div>
        </div>
      </div>

      <nav class="menu">
        <template v-for="group in menuGroups" :key="group.label">
          <div v-if="!collapsed" class="menu-group-label">{{ group.label }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="menu-item"
            :class="{ active: isActive(item.path) }"
          >
            <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
            <span v-if="!collapsed" class="menu-label">{{ item.label }}</span>
            <span v-if="!collapsed && item.badge && badgeCount(item) !== null" class="menu-badge">{{ badgeCount(item) }}</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-foot">
        <div class="clock stat-num">{{ timeText }}</div>
        <div class="clock-date">{{ dateText }}</div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn" @click="collapsed = !collapsed">
            <el-icon :size="15"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </button>

          <!-- 全局搜索 -->
          <div class="global-search" ref="searchWrap">
            <div class="gs-input" :class="{ open: searchOpen }">
              <el-icon :size="14" class="gs-search-icon"><Search /></el-icon>
              <input
                ref="searchInput"
                v-model="searchKw"
                type="text"
                placeholder="搜索设备、任务、工单、合同…"
                @focus="searchOpen = true"
                @input="onSearch"
                @keydown.esc="closeSearch"
                @keydown.enter="goFirst"
              />
              <kbd v-if="!searchKw">Ctrl K</kbd>
              <button v-else class="gs-clear" @click="searchKw = ''; onSearch()">
                <el-icon :size="12"><Close /></el-icon>
              </button>
            </div>

            <transition name="gs-pop">
              <div v-if="searchOpen" class="gs-panel">
                <div v-if="searching" class="gs-loading">搜索中…</div>
                <template v-else>
                  <div v-if="!searchKw.trim()" class="gs-hint">
                    输入关键词跨模块搜索<br />
                    <span class="gs-hint-k"><kbd>Enter</kbd> 打开第一条 · <kbd>Esc</kbd> 关闭</span>
                  </div>
                  <template v-else>
                    <div v-if="result.devices.length" class="gs-group">
                      <div class="gs-group-title">设备 · {{ result.devices.length }}</div>
                      <div v-for="d in result.devices" :key="'d' + d.id" class="gs-item" @click="goto(`/device/${d.id}`)">
                        <el-icon :size="13" class="gs-icon"><Grid /></el-icon>
                        <span class="gs-name">{{ d.name }}</span>
                        <span class="gs-code">{{ d.deviceCode }}</span>
                        <span class="gs-meta">{{ d.location }}</span>
                      </div>
                    </div>
                    <div v-if="result.tasks.length" class="gs-group">
                      <div class="gs-group-title">任务 · {{ result.tasks.length }}</div>
                      <div v-for="t in result.tasks" :key="'t' + t.id" class="gs-item" @click="goto(t.taskType === 'INSPECT' ? '/task/inspect' : '/task/maintain')">
                        <el-icon :size="13" class="gs-icon"><Checked /></el-icon>
                        <span class="gs-name">{{ t.deviceName || t.taskCode }}</span>
                        <span class="gs-code">{{ t.taskCode }}</span>
                        <span class="gs-meta">{{ t.taskType === 'INSPECT' ? '巡检' : '保养' }} · {{ t.planDate }}</span>
                      </div>
                    </div>
                    <div v-if="result.repairs.length" class="gs-group">
                      <div class="gs-group-title">工单 · {{ result.repairs.length }}</div>
                      <div v-for="r in result.repairs" :key="'r' + r.id" class="gs-item" @click="goto('/repair')">
                        <el-icon :size="13" class="gs-icon"><Warning /></el-icon>
                        <span class="gs-name">{{ r.faultDesc }}</span>
                        <span class="gs-code">{{ r.orderCode }}</span>
                        <span class="gs-meta">{{ r.reporter }} · {{ (r.createTime || '').slice(0, 10) }}</span>
                      </div>
                    </div>
                    <div v-if="result.contracts.length" class="gs-group">
                      <div class="gs-group-title">合同 · {{ result.contracts.length }}</div>
                      <div v-for="c in result.contracts" :key="'c' + c.id" class="gs-item" @click="goto('/contract')">
                        <el-icon :size="13" class="gs-icon"><Document /></el-icon>
                        <span class="gs-name">{{ c.contractName }}</span>
                        <span class="gs-code">{{ c.contractNo }}</span>
                        <span class="gs-meta">{{ c.vendor }}</span>
                      </div>
                    </div>
                    <div v-if="!result.devices.length && !result.tasks.length && !result.repairs.length && !result.contracts.length" class="gs-empty">
                      未找到与「{{ searchKw }}」相关的内容
                    </div>
                  </template>
                </template>
              </div>
            </transition>
          </div>
        </div>

        <div class="topbar-right">
          <el-popover placement="bottom-end" :width="330" trigger="click" popper-class="notify-pop">
            <template #reference>
              <button class="icon-btn" style="position: relative">
                <el-icon :size="15"><Bell /></el-icon>
                <span v-if="reminders.length" class="dot-alert"></span>
              </button>
            </template>
            <div class="notify-title">
              <span>合同到期提醒</span>
              <span class="notify-count">{{ reminders.length }}</span>
            </div>
            <div v-if="reminders.length" class="notify-list">
              <div v-for="r in reminders.slice(0, 6)" :key="r.id" class="notify-item">
                <span class="status-dot" :class="r.status === 'EXPIRED' ? 'danger' : 'warn'"></span>
                <div class="notify-body">
                  <div class="notify-name">{{ r.contractName }}</div>
                  <div class="notify-desc">{{ r.vendor }} · {{ r.endDate }}</div>
                </div>
                <span class="notify-state" :class="r.status === 'EXPIRED' ? 'expired' : 'expiring'">
                  {{ r.status === 'EXPIRED' ? '已过期' : '将到期' }}
                </span>
              </div>
            </div>
            <div v-else class="notify-empty">暂无到期合同</div>
            <router-link to="/contract" class="notify-more">查看全部合同 →</router-link>
          </el-popover>

          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="user-chip">
              <span class="user-avatar">{{ (user?.realName || 'U').slice(0, 1) }}</span>
              <span class="user-name">{{ user?.realName }}</span>
              <span class="user-role">{{ roleText }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="400px" align-center>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="72px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword, contractReminders, devicePage, taskPage, repairPage, contractPage } from '@/api'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const collapsed = ref(false)
const user = computed(() => store.user)
const roleText = computed(() =>
  ({ ADMIN: '管理员', MAINTAINER: '维保', INSPECTOR: '巡检' })[user.value?.role] || '用户')

const menuGroups = computed(() => {
  const all = [
    {
      label: '运行管理',
      items: [
        { path: '/dashboard', label: '智慧总览', icon: 'DataAnalysis', badge: 'overview', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] },
        { path: '/device', label: '设备台账', icon: 'Grid', badge: 'device', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] },
        { path: '/task/inspect', label: '巡检管理', icon: 'Checked', badge: 'inspect', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] },
        { path: '/task/maintain', label: '保养管理', icon: 'Tools', badge: 'maintain', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] }
      ]
    },
    {
      label: '服务保障',
      items: [
        { path: '/repair', label: '报修工单', icon: 'Warning', badge: 'repair', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] },
        { path: '/contract', label: '维保合同', icon: 'Document', badge: 'contract', roles: ['ADMIN'] }
      ]
    },
    {
      label: '洞察分析',
      items: [
        { path: '/monitor', label: '运行监测', icon: 'Odometer', badge: 'monitor', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] },
        { path: '/analysis', label: '年限分析', icon: 'TrendCharts', badge: 'analysis', roles: ['ADMIN', 'INSPECTOR', 'MAINTAINER'] }
      ]
    },
    {
      label: '系统设置',
      items: [
        { path: '/system/user', label: '用户管理', icon: 'User', badge: 'user', roles: ['ADMIN'] }
      ]
    }
  ]
  return all
    .map(g => ({ ...g, items: g.items.filter(m => m.roles.includes(store.role)) }))
    .filter(g => g.items.length)
})

const reminders = ref([])
const loadReminders = async () => {
  try { reminders.value = await contractReminders() } catch (e) { /* ignore */ }
}

const badges = ref({})
const badgeCount = (item) => badges.value[item.badge] ?? null
const loadBadges = async () => {
  try {
    const api = await import('@/api')
    const [ov, ts] = await Promise.all([api.statsOverview(), api.taskToday()])
    badges.value = {
      device: ov.deviceFault || 0,
      inspect: ts.todayPending || 0,
      maintain: ts.overdueCount || 0,
      repair: ov.pendingOrders || 0,
      contract: ov.expiringContracts || 0
    }
  } catch (e) { /* ignore */ }
}

const isActive = (path) => {
  if (path === '/device') return route.path.startsWith('/device')
  if (path === '/task/inspect') return route.path.startsWith('/task/inspect')
  if (path === '/task/maintain') return route.path.startsWith('/task/maintain')
  return route.path === path
}

// 时钟
const timeText = ref('')
const dateText = ref('')
let timer = null
const tick = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  timeText.value = `${p(d.getHours())}:${p(d.getMinutes())}`
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  dateText.value = `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} 周${week}`
}
onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
  loadReminders()
  loadBadges()
  const iv = setInterval(() => { loadReminders(); loadBadges() }, 120000)
  onUnmounted(() => { clearInterval(iv) })
  document.addEventListener('click', onDocClick)
  window.addEventListener('keydown', onHotkey)
})
onUnmounted(() => {
  clearInterval(timer)
  clearTimeout(searchTimer)
  document.removeEventListener('click', onDocClick)
  window.removeEventListener('keydown', onHotkey)
})

const onUserCommand = (cmd) => {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '', confirm: '' }
    pwdVisible.value = true
  }
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

// ===== 全局搜索 =====
const searchWrap = ref()
const searchInput = ref()
const searchKw = ref('')
const searchOpen = ref(false)
const searching = ref(false)
const result = reactive({ devices: [], tasks: [], repairs: [], contracts: [] })
let searchTimer = null

const onSearch = () => {
  clearTimeout(searchTimer)
  const kw = searchKw.value.trim()
  if (!kw) {
    result.devices = []
    result.tasks = []
    result.repairs = []
    result.contracts = []
    searching.value = false
    return
  }
  searching.value = true
  searchTimer = setTimeout(async () => {
    try {
      const [devices, tasks, repairs, contracts] = await Promise.all([
        devicePage({ page: 1, size: 5, keyword: kw }),
        taskPage({ page: 1, size: 5, keyword: kw }),
        repairPage({ page: 1, size: 5, keyword: kw }),
        contractPage({ page: 1, size: 5, keyword: kw })
      ])
      result.devices = devices.records || []
      result.tasks = tasks.records || []
      result.repairs = repairs.records || []
      result.contracts = contracts.records || []
    } catch (e) {
      // ignore
    } finally {
      searching.value = false
    }
  }, 280)
}

const totalResults = computed(() =>
  result.devices.length + result.tasks.length + result.repairs.length + result.contracts.length)

const goFirst = () => {
  if (result.devices.length) return goto(`/device/${result.devices[0].id}`)
  if (result.tasks.length) return goto(result.tasks[0].taskType === 'INSPECT' ? '/task/inspect' : '/task/maintain')
  if (result.repairs.length) return goto('/repair')
  if (result.contracts.length) return goto('/contract')
}

const goto = (path) => {
  closeSearch()
  router.push(path)
}

const closeSearch = () => {
  searchOpen.value = false
  searchInput.value && searchInput.value.blur()
}

const onDocClick = (e) => {
  if (searchWrap.value && !searchWrap.value.contains(e.target)) {
    searchOpen.value = false
  }
}
const onHotkey = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchOpen.value = true
    searchInput.value && searchInput.value.focus()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => v === pwdForm.value.newPassword ? cb() : cb(new Error('两次密码不一致')),
    trigger: 'blur'
  }]
}
const submitPwd = async () => {
  try {
    await pwdFormRef.value.validate()
  } catch (e) { return }
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    store.logout()
    router.push('/login')
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
}

/* ===== 侧边栏：近黑 ===== */
.sidebar {
  position: relative;
  z-index: 20;
  width: 228px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #101012;
  transition: width .22s cubic-bezier(.4, 0, .2, 1);
  overflow: hidden;
}
.sidebar.collapsed { width: 68px; }

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 18px 16px 14px;
  position: relative;
  z-index: 1;
}
.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: #fff;
  color: #101012;
  font-size: 16px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-name { font-size: 14.5px; font-weight: 700; color: #f4f4f5; letter-spacing: .01em; white-space: nowrap; }
.brand-sub { font-size: 9px; color: #71717a; letter-spacing: .14em; text-transform: uppercase; margin-top: 2px; white-space: nowrap; }

.menu {
  flex: 1;
  padding: 4px 10px 10px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
}
.menu::-webkit-scrollbar { width: 4px; }
.menu::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, .14); }
.menu-group-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .14em;
  text-transform: uppercase;
  color: #565660;
  padding: 14px 10px 5px;
  white-space: nowrap;
}
.menu-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  height: 40px;
  padding: 0 12px;
  margin: 2px 0;
  border-radius: 8px;
  color: #9c9ca6;
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  transition: all .14s ease;
  white-space: nowrap;
}
.menu-item:hover { background: rgba(255, 255, 255, .06); color: #f4f4f5; }
.menu-item.active {
  background: rgba(255, 255, 255, .1);
  color: #fff;
  font-weight: 600;
}
.menu-item.active::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 2.5px;
  height: 18px;
  border-radius: 0 3px 3px 0;
  background: #fff;
}
.menu-icon { font-size: 16px; flex-shrink: 0; color: #71717a; }
.menu-item:hover .menu-icon { color: #d4d4d8; }
.menu-item.active .menu-icon { color: #fff; }
.menu-label { flex: 1; overflow: hidden; text-overflow: ellipsis; }
.menu-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 5px;
  background: rgba(255, 255, 255, .12);
  color: #d4d4d8;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  font-size: 10.5px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.menu-badge.warn { background: rgba(217, 119, 6, .25); color: #fbbf24; }

.sidebar-foot {
  padding: 12px 14px 14px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  text-align: center;
  position: relative;
  z-index: 1;
}
.clock {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: #f4f4f5;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  font-variant-numeric: tabular-nums;
}
.clock-date { font-size: 10.5px; color: #71717a; margin-top: 3px; letter-spacing: .08em; }

/* ===== 主区域 ===== */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: rgba(255, 255, 255, .88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 15;
}
.topbar-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.topbar-right { display: flex; align-items: center; gap: 8px; }

/* ===== 全局搜索 ===== */
.global-search { position: relative; width: 300px; max-width: 36vw; }
.gs-input {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  transition: all .16s ease;
}
.gs-input.open { background: #fff; border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim-2); }
.gs-search-icon { color: var(--text-3); flex-shrink: 0; }
.gs-input input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: "HarmonyOS Sans SC", "Inter", sans-serif;
  font-size: 13px;
  color: var(--text-1);
}
.gs-input input::placeholder { color: var(--text-3); }
.gs-input kbd {
  font-family: "Inter", sans-serif;
  font-size: 10px;
  color: var(--text-3);
  border: 1px solid var(--line-2);
  border-radius: 4px;
  padding: 1px 5px;
  background: #fff;
  flex-shrink: 0;
}
.gs-clear {
  border: none;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  display: flex;
  padding: 2px;
  border-radius: 4px;
  flex-shrink: 0;
}
.gs-clear:hover { color: var(--text-1); background: var(--panel-3); }

.gs-panel {
  position: absolute;
  top: 42px;
  left: 0;
  right: 0;
  z-index: 100;
  max-height: 420px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: var(--shadow-pop);
  padding: 8px;
}
.gs-loading, .gs-hint, .gs-empty {
  padding: 26px 16px;
  text-align: center;
  color: var(--text-3);
  font-size: 12.5px;
  line-height: 1.9;
}
.gs-hint-k kbd {
  font-family: "Inter", sans-serif;
  font-size: 10px;
  border: 1px solid var(--line-2);
  border-radius: 4px;
  padding: 1px 5px;
  background: var(--panel-2);
  color: var(--text-2);
}
.gs-group { margin-bottom: 6px; }
.gs-group-title {
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: .1em;
  color: var(--text-3);
  padding: 8px 10px 5px;
  text-transform: uppercase;
}
.gs-group-title span { font-family: "Inter", sans-serif; }
.gs-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .12s ease;
}
.gs-item:hover { background: var(--panel-2); }
.gs-icon { color: var(--text-2); flex-shrink: 0; }
.gs-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 45%;
}
.gs-code {
  font-family: "Inter", sans-serif;
  font-size: 11px;
  color: var(--text-2);
  flex-shrink: 0;
}
.gs-meta {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 40%;
}
.gs-pop-enter-active, .gs-pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.gs-pop-enter-from, .gs-pop-leave-to { opacity: 0; transform: translateY(-4px); }

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s ease;
}
.icon-btn:hover {
  color: var(--text-1);
  border-color: var(--line);
  background: var(--panel-2);
}

.dot-alert {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--danger);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 10px 4px 5px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s ease;
}
.user-chip:hover { background: var(--panel-2); }
.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #18181b;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-name { font-size: 13px; font-weight: 600; }
.user-role {
  font-size: 10.5px;
  color: var(--text-3);
  border: 1px solid var(--line);
  padding: 1px 7px;
  border-radius: 5px;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  letter-spacing: .04em;
}

.content { flex: 1; overflow-y: auto; overflow-x: hidden; }

.page-fade-enter-active, .page-fade-leave-active { transition: opacity .16s ease, transform .16s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(4px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-2px); }

/* 通知 */
.notify-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 4px;
}
.notify-count { font-family: "Inter", sans-serif; font-size: 12px; color: var(--warn); }
.notify-list { max-height: 280px; overflow-y: auto; }
.notify-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 2px;
  border-bottom: 1px solid var(--line);
}
.notify-body { flex: 1; min-width: 0; }
.notify-name { font-size: 12.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.notify-desc { font-size: 11px; color: var(--text-3); margin-top: 2px; }
.notify-state { font-size: 11px; flex-shrink: 0; font-weight: 600; }
.notify-state.expired { color: var(--danger); }
.notify-state.expiring { color: var(--warn); }
.notify-empty { padding: 18px 0; text-align: center; color: var(--text-3); font-size: 12.5px; }
.notify-more {
  display: block;
  text-align: center;
  margin-top: 10px;
  font-size: 12px;
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
}
</style>


