<template>
  <div class="m-page">
    <header class="m-header">
      <div class="m-header-inner">
        <button class="m-back" @click="$router.push('/mobile/scan')">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </button>
        <div class="m-header-title">我的任务</div>
        <div class="m-header-sub">{{ store.user?.realName }}</div>
      </div>
    </header>

    <div class="m-content">
      <div class="filter-tabs">
        <button class="f-tab" :class="{ active: type === 'INSPECT' }" @click="type = 'INSPECT'; load()">巡检</button>
        <button class="f-tab" :class="{ active: type === 'MAINTAIN' }" @click="type = 'MAINTAIN'; load()">保养</button>
      </div>

      <!-- 今日统计：打卡后数字实时变化 -->
      <div class="panel today-strip">
        <div class="ts-item">
          <b class="ts-num ok">{{ today.todayDone ?? '-' }}</b>
          <span>今日完成</span>
        </div>
        <div class="ts-divider"></div>
        <div class="ts-item">
          <b class="ts-num">{{ today.todayPending ?? '-' }}</b>
          <span>今日待办</span>
        </div>
        <div class="ts-divider"></div>
        <div class="ts-item">
          <b class="ts-num warn">{{ today.overdueCount ?? '-' }}</b>
          <span>逾期未巡</span>
        </div>
      </div>

      <div v-if="tasks.length" class="task-list">
        <div v-for="t in tasks" :key="t.id" class="panel task-item">
          <div class="task-head">
            <span class="task-code">{{ t.taskCode }}</span>
            <span class="task-status" :class="t.status.toLowerCase()">
              <span class="status-dot" :class="t.status === 'PENDING' ? 'accent' : 'danger'"></span>{{ t.status === 'PENDING' ? '待执行' : '已逾期' }}
            </span>
          </div>
          <div class="task-name">{{ t.deviceName }} <span class="task-code-sm">{{ t.deviceCode }}</span></div>
          <div class="task-loc"><el-icon :size="12"><Location /></el-icon>{{ t.deviceLocation }}</div>
          <div class="task-foot">
            <span class="task-date">计划 {{ t.planDate }}</span>
            <el-button
              v-if="t.status === 'PENDING'"
              type="primary"
              size="small"
              round
              @click="doCheck(t)"
            >打卡</el-button>
            <el-button v-else size="small" round @click="doCheck(t)">补卡</el-button>
          </div>
        </div>
      </div>
      <div v-else class="panel m-empty">
        <el-icon :size="36" color="var(--text-3)"><CircleCheck /></el-icon>
        <p>暂无待办任务</p>
        <el-button size="small" round @click="$router.push('/mobile/scan')">去扫码巡检</el-button>
      </div>

      <!-- 最近完成：打卡后立刻能看到最新记录 -->
      <div v-if="recent.length" class="recent-block">
        <div class="recent-title">
          <span class="check-dot ok"></span>最近完成
          <span class="recent-sub">按打卡时间倒序</span>
        </div>
        <div v-for="r in recent" :key="r.id" class="panel task-item recent-item">
          <div class="task-head">
            <span class="task-code">{{ r.taskCode }}</span>
            <span class="task-status completed">
              <span class="status-dot ok"></span>{{ r.result === 'ABNORMAL' ? '异常' : '正常' }}
            </span>
          </div>
          <div class="task-name">{{ r.deviceName }} <span class="task-code-sm">{{ r.deviceCode }}</span></div>
          <div class="task-foot">
            <span class="task-date">打卡 {{ r.checkTime ? r.checkTime.slice(5, 16) : '—' }} · {{ r.executor || '—' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { myTasks, checkTask, taskToday, taskPage } from '@/api'

const store = useUserStore()
const type = ref('INSPECT')
const tasks = ref([])
const recent = ref([])

const load = async () => {
  try {
    tasks.value = await myTasks({ type: type.value, executor: store.user?.realName || store.user?.username })
  } catch (e) {
    ElMessage.error('加载任务失败，请稍后重试')
  }
}

// 最近完成（打卡后立刻可见）
const loadRecent = async () => {
  try {
    const data = await taskPage({ type: type.value, status: 'COMPLETED', page: 1, size: 5 })
    recent.value = data.records || []
  } catch (e) { /* 忽略 */ }
}

const doCheck = async (t) => {
  const { value } = await ElMessageBox.prompt(
    `${t.deviceName}（${t.deviceCode}）\n打卡位置：${t.deviceLocation || '请填写'}`,
    '巡检打卡',
    {
      confirmButtonText: '正常',
      cancelButtonText: '取消',
      inputPlaceholder: '备注（选填）',
      inputValue: '',
      inputType: 'textarea',
      distinguishCancelAndClose: true
    }
  ).catch(e => {
    if (e === 'cancel' || e === 'close') return null
    return { value: '' }
  })
  if (value === undefined || value === null) return
  // 结果选择：默认正常，长按输入异常说明
  const result = await ElMessageBox.confirm(
    `确认「${t.deviceName}」巡检结果？\n备注：${value || '无'}`,
    '确认结果',
    {
      confirmButtonText: '正常',
      cancelButtonText: '异常',
      type: 'info',
      distinguishCancelAndClose: true
    }
  ).then(() => 'NORMAL').catch(e => {
    if (e === 'cancel') return 'ABNORMAL'
    return null
  })
  if (!result) return
  if (result === 'ABNORMAL' && !value.trim()) {
    ElMessage.warning('异常打卡请填写备注说明')
    return
  }
  await checkTask({
    taskId: t.id,
    result,
    remark: value || '',
    location: t.deviceLocation || '',
    executor: store.user?.realName || store.user?.username
  })
  ElMessage.success(result === 'NORMAL' ? '打卡成功' : '已记录异常，请及时报修')
  load()
  loadRecent()
  loadToday()
}

const today = ref({})

const loadToday = async () => {
  try {
    today.value = (await taskToday()) || {}
  } catch (e) { /* 忽略 */ }
}

onMounted(() => {
  load()
  loadRecent()
  loadToday()
})
</script>

<style scoped>
.m-page { min-height: 100vh; background: var(--bg); padding-bottom: 40px; }
.m-header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 14px 16px 10px;
  background: var(--bg-deep);
  border-bottom: 1px solid var(--line);
}
.m-header-inner { display: flex; align-items: center; gap: 12px; max-width: 480px; margin: 0 auto; }
.m-back {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.m-header-title { font-size: 15.5px; font-weight: 700; }
.m-header-sub { font-size: 11.5px; color: var(--text-3); margin-left: auto; }

.m-content { max-width: 480px; margin: 0 auto; padding: 16px; }

.filter-tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.today-strip {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 14px 6px;
  margin-bottom: 14px;
  border-radius: 14px;
}
.ts-item { text-align: center; flex: 1; }
.ts-num {
  display: block;
  font-family: "Inter", "HarmonyOS Sans SC", sans-serif;
  font-variant-numeric: tabular-nums;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-1);
}
.ts-num.ok { color: var(--ok); }
.ts-num.warn { color: var(--warn); }
.ts-item span { font-size: 11.5px; color: var(--text-3); }
.ts-divider { width: 1px; height: 26px; background: var(--line); }
.recent-block { margin-top: 16px; }
.recent-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 10px;
}
.recent-sub { font-size: 11px; color: var(--text-3); font-weight: 400; }.recent-item { margin-bottom: 10px; }
.task-status.completed { color: var(--ok); }
.status-dot.ok { background: var(--ok); }
.f-tab {
  flex: 1;
  height: 38px;
  border-radius: 6px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-2);
  font-size: 13.5px;
  cursor: pointer;
  transition: all .15s ease;
}
.f-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.task-item { padding: 14px 16px; border-radius: 10px; margin-bottom: 12px; }
.task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.task-code { font-family: var(--font-num); font-size: 11.5px; color: var(--text-3); }
.task-status {
  font-size: 11.5px;
  padding: 2px 10px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
}
.task-status.pending { background: rgba(59, 110, 245, .08); }
.task-status.overdue { background: rgba(214, 69, 69, .08); }
.task-name { font-size: 15px; font-weight: 700; }
.task-code-sm { font-family: var(--font-num); font-size: 11px; color: var(--accent); margin-left: 6px; }
.task-loc { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-2); margin-top: 5px; }
.task-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}
.task-date { font-family: var(--font-num); font-size: 12px; color: var(--text-3); }

.m-empty { padding: 40px 20px; text-align: center; border-radius: 20px; }
.m-empty p { color: var(--text-2); font-size: 14px; margin: 12px 0 18px; }
</style>
