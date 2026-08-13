<template>
  <div class="m-page">
    <!-- 顶部 -->
    <header class="m-header">
      <div class="m-header-inner">
        <div class="m-logo-sm">
          <el-icon :size="18" color="#fff"><Cpu /></el-icon>
        </div>
        <div class="m-header-text">
          <div class="m-header-title">扫码巡检</div>
          <div class="m-header-sub">{{ store.user?.realName || '未登录' }}</div>
        </div>
        <router-link to="/mobile/tasks" class="m-link">
          <el-icon :size="20"><List /></el-icon>
        </router-link>
      </div>
    </header>

    <div class="m-content">
      <!-- 未登录 -->
      <div v-if="!store.isLogin" class="panel m-empty">
        <el-icon :size="40" color="var(--text-3)"><Lock /></el-icon>
        <p>请先登录后再进行巡检打卡</p>
        <el-button type="primary" round @click="$router.push('/mobile/login?redirect=/mobile/scan')">去登录</el-button>
      </div>

      <template v-else>
        <!-- 扫码区 -->
        <div class="panel scan-card">
          <div class="scan-tabs">
            <button class="scan-tab" :class="{ active: mode === 'camera' }" @click="switchMode('camera')">
              <el-icon :size="15"><Camera /></el-icon>扫码
            </button>
            <button class="scan-tab" :class="{ active: mode === 'input' }" @click="switchMode('input')">
              <el-icon :size="15"><EditPen /></el-icon>输入编号
            </button>
          </div>

          <div v-show="mode === 'camera'" class="scan-box">
            <!-- 摄像头可用：取景框 -->
            <template v-if="!cameraError">
              <div id="qr-reader" class="qr-reader"></div>
              <p class="scan-hint">将设备二维码对准取景框</p>
            </template>
            <!-- 摄像头不可用：引导卡片 -->
            <div v-else class="camera-fallback">
              <div class="cf-icon"><el-icon :size="26"><CameraFilled /></el-icon></div>
              <p class="cf-title">当前浏览器无法调用摄像头</p>
              <p class="cf-desc">网页扫码在 HTTP 局域网下被浏览器安全策略限制。请用手机相机或微信「扫一扫」直接扫设备上的二维码标签，即可自动打开本页并识别设备完成打卡。</p>
              <el-button type="primary" round class="cf-btn" @click="switchMode('input')">
                输入设备编号
              </el-button>
            </div>
          </div>

          <div v-show="mode === 'input'" class="scan-input">
            <el-input v-model="inputCode" placeholder="输入设备编号，如 DT-001" size="large" clearable @keyup.enter="queryDevice()">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" style="margin-top: 12px" round :loading="querying" @click="queryDevice">
              查询设备
            </el-button>
          </div>
        </div>

        <!-- 设备信息 -->
        <div v-if="device" class="panel device-card">
          <div class="dev-head">
            <div class="dev-icon">
              <el-icon :size="26" color="#fff"><Grid /></el-icon>
            </div>
            <div class="dev-info">
              <div class="dev-name">{{ device.name }}</div>
              <div class="dev-code">{{ device.deviceCode }} · {{ typeText }}</div>
            </div>
            <span class="dev-status" :class="device.status.toLowerCase()">
              <span class="status-dot" :class="devDot(device.status)"></span>{{ statusText }}
            </span>
          </div>
          <div class="dev-meta">
            <span><el-icon :size="12"><Location /></el-icon>{{ device.location }}</span>
            <span>安装于 {{ device.installDate || '—' }}</span>
          </div>
          <el-button class="repair-entry" plain type="danger" round size="small" @click="openRepair">
            <el-icon :size="13"><Warning /></el-icon>发现故障 · 发起报修
          </el-button>
        </div>

        <!-- 打卡表单 -->
        <div v-if="device" class="panel check-card">
          <div class="check-title">
            <span class="check-dot"></span>巡检打卡
          </div>
          <el-radio-group v-model="checkForm.result" class="result-group">
            <el-radio-button value="NORMAL">正常</el-radio-button>
            <el-radio-button value="ABNORMAL">异常</el-radio-button>
          </el-radio-group>
          <el-input v-model="checkForm.remark" type="textarea" :rows="2" placeholder="备注（异常时请填写故障现象）" style="margin-top: 12px" />
          <el-button
            type="primary"
            size="large"
            class="check-btn"
            :loading="checking"
            @click="submit"
          >
            <el-icon :size="16"><Checked /></el-icon>确认打卡
          </el-button>
        </div>

        <!-- 报修弹窗 -->
        <el-dialog v-model="repairVisible" title="发起报修" width="92%" append-to-body>
          <div class="repair-form">
            <el-select v-model="repairForm.level" placeholder="紧急程度" style="width: 100%">
              <el-option label="紧急（影响使用）" value="HIGH" />
              <el-option label="一般" value="MEDIUM" />
              <el-option label="轻微" value="LOW" />
            </el-select>
            <el-input v-model="repairForm.faultDesc" type="textarea" :rows="3" placeholder="请描述故障现象（如：电梯运行异响、按键失灵）" style="margin-top: 12px" />
            <el-input v-model="repairForm.phone" placeholder="联系电话" style="margin-top: 12px" maxlength="11" />
          </div>
          <template #footer>
            <el-button round @click="repairVisible = false">取消</el-button>
            <el-button round type="danger" :loading="repairing" @click="submitRepair">提交报修</el-button>
          </template>
        </el-dialog>

        <!-- 成功浮层 -->
        <transition name="pop">
          <div v-if="success" class="success-mask" @click="success = false">
            <div class="success-box">
              <div class="success-ring">
                <el-icon :size="44" color="var(--ok)"><CircleCheckFilled /></el-icon>
              </div>
              <div class="success-title">打卡成功</div>
              <div class="success-sub">{{ device?.name }} 巡检完成</div>
              <el-button round type="primary" plain @click="reset">继续巡检</el-button>
            </div>
          </div>
        </transition>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Html5Qrcode } from 'html5-qrcode'
import { useUserStore } from '@/stores/user'
import { deviceByCode, checkByDevice, createRepair } from '@/api'

const store = useUserStore()
const mode = ref('camera')
const inputCode = ref('')
const device = ref(null)
const querying = ref(false)
const checking = ref(false)
const success = ref(false)
const cameraError = ref(false)
const checkForm = ref({ result: 'NORMAL', remark: '' })
const repairVisible = ref(false)
const repairing = ref(false)
const repairForm = ref({ level: 'MEDIUM', faultDesc: '', phone: '' })

const openRepair = () => {
  repairForm.value = { level: 'MEDIUM', faultDesc: '', phone: store.user?.phone || '' }
  repairVisible.value = true
}

const submitRepair = async () => {
  if (!device.value) return
  if (!repairForm.value.faultDesc.trim()) {
    ElMessage.warning('请填写故障描述')
    return
  }
  repairing.value = true
  try {
    await createRepair({
      deviceId: device.value.id,
      faultDesc: repairForm.value.faultDesc.trim(),
      level: repairForm.value.level,
      reporter: store.user?.realName || '巡检人员',
      phone: repairForm.value.phone
    })
    ElMessage.success('报修已提交，工单已生成')
    repairVisible.value = false
  } catch (e) { /* 拦截器已提示 */ } finally {
    repairing.value = false
  }
}

const typeMap = { ELEVATOR: '电梯', FIRE: '消防', PUMP: '水泵', ACCESS: '门禁', OTHER: '其他' }
const statusMap = { RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停用', SCRAPPED: '报废' }
const devDot = (s) => ({ RUNNING: 'ok', FAULT: 'danger', REPAIRING: 'warn', STOPPED: 'neutral', SCRAPPED: 'neutral' }[s] || 'neutral')
const typeText = computed(() => device.value ? (typeMap[device.value.type] || device.value.type) : '')
const statusText = computed(() => device.value ? (statusMap[device.value.status] || device.value.status) : '')

let scanner = null
let stopFlag = false

const startCamera = async () => {
  if (scanner && scanner.isScanning) return
  stopFlag = false
  cameraError.value = false
  if (!scanner) {
    scanner = new Html5Qrcode('qr-reader')
  }
  try {
    await scanner.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 210, height: 210 } },
      (text) => {
        if (stopFlag) return
        const m = text.match(/code=([\w-]+)/)
        if (m) {
          handleCode(m[1])
        } else {
          ElMessage.warning('未识别到设备二维码')
        }
      },
      () => { /* 每帧回调忽略 */ }
    )
  } catch (e) {
    cameraError.value = true
  }
}

const stopCamera = async () => {
  stopFlag = true
  if (scanner && scanner.isScanning) {
    try { await scanner.stop() } catch (e) { /* ignore */ }
  }
}

const switchMode = async (m) => {
  mode.value = m
  if (m === 'camera') {
    await startCamera()
  } else {
    await stopCamera()
  }
}

const handleCode = async (code) => {
  await stopCamera()
  mode.value = 'input'
  inputCode.value = code
  await queryDevice(code)
}

const queryDevice = async (code) => {
  const c = typeof code === 'string' ? code.trim() : inputCode.value.trim()
  if (!c) {
    ElMessage.warning('请输入设备编号')
    return
  }
  querying.value = true
  try {
    device.value = await deviceByCode(c)
    if (!device.value) {
      ElMessage.warning('未找到该设备，请核对编号')
      return
    }
    ElMessage.success(`已识别设备：${device.value.name}`)
  } catch (e) {
    device.value = null
  } finally {
    querying.value = false
  }
}

const submit = async () => {
  if (!device.value) return
  if (checkForm.value.result === 'ABNORMAL' && !checkForm.value.remark.trim()) {
    ElMessage.warning('异常打卡请填写故障说明')
    return
  }
  checking.value = true
  try {
    await checkByDevice({
      deviceCode: device.value.deviceCode,
      result: checkForm.value.result,
      remark: checkForm.value.remark,
      location: device.value.location,
      executor: store.user?.realName || store.user?.username
    })
    success.value = true
  } catch (e) { /* 拦截器已提示 */ } finally {
    checking.value = false
  }
}

const reset = () => {
  success.value = false
  device.value = null
  inputCode.value = ''
  checkForm.value = { result: 'NORMAL', remark: '' }
  mode.value = 'camera'
  startCamera()
}

// 从二维码 URL 参数直接进入（PC 端生成二维码指向本页）
onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  if (code) {
    inputCode.value = code
    mode.value = 'input'
    if (store.isLogin) {
      await queryDevice(code)
    }
  } else if (store.isLogin) {
    await startCamera()
  }
})

// 未登录打开 ?code= 链接：登录成功后自动补查设备
watch(() => store.isLogin, (v) => {
  if (v && inputCode.value && !device.value) {
    queryDevice(inputCode.value)
  }
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.m-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}
.m-header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 14px 16px 10px;
  background: var(--bg-deep);
  border-bottom: 1px solid var(--line);
}
.m-header-inner { display: flex; align-items: center; gap: 12px; max-width: 480px; margin: 0 auto; }
.m-logo-sm {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: var(--bg-deep);
  border: 1px solid var(--line-2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.m-header-text { flex: 1; }
.m-header-title { font-size: 15.5px; font-weight: 700; }
.m-header-sub { font-size: 11.5px; color: var(--text-3); }
.m-link {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-2);
  text-decoration: none;
}

.m-content { max-width: 480px; margin: 0 auto; padding: 16px; }

.m-empty { padding: 40px 20px; text-align: center; border-radius: var(--radius); }
.m-empty p { color: var(--text-2); font-size: 14px; margin: 12px 0 18px; }

.scan-card { padding: 16px; border-radius: var(--radius); }
.scan-tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.scan-tab {
  flex: 1;
  height: 38px;
  border-radius: 6px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-2);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all .25s ease;
}
.scan-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.scan-box { text-align: center; }
.qr-reader {
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
  background: #000;
}
.qr-reader :deep(video) { border-radius: 14px; }
.camera-fallback {
  padding: 26px 18px 24px;
  background: var(--panel-2);
  border: 1px dashed var(--line-2);
  border-radius: 14px;
  text-align: center;
}
.cf-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--accent-dim);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cf-title { font-size: 14.5px; font-weight: 600; color: var(--text-1); }
.cf-desc { font-size: 12.5px; color: var(--text-2); line-height: 1.75; margin: 10px auto 16px; max-width: 300px; }
.cf-btn { width: 100%; max-width: 280px; }
.repair-entry { width: 100%; margin-top: 14px; }
.repair-form .el-input, .repair-form .el-select { width: 100%; }
.scan-hint { font-size: 12px; color: var(--text-3); margin-top: 10px; }
.scan-input { padding: 8px 0 2px; }

.device-card { margin-top: 14px; padding: 16px; border-radius: var(--radius); }
.dev-head { display: flex; align-items: center; gap: 12px; }
.dev-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  background: var(--bg-deep);
  border: 1px solid var(--line-2);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dev-info { flex: 1; min-width: 0; }
.dev-name { font-size: 15px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dev-code { font-family: var(--font-num); font-size: 11.5px; color: var(--text-3); margin-top: 2px; }
.dev-status { font-size: 11.5px; padding: 3px 10px; border-radius: 14px; flex-shrink: 0; display: inline-flex; align-items: center; gap: 6px; }
.dev-status.running { background: rgba(63, 182, 139, .1); color: var(--ok); }
.dev-status.fault { background: rgba(212, 93, 93, .1); color: var(--danger); }
.dev-status.repairing { background: rgba(217, 164, 65, .1); color: var(--warn); }
.dev-status.stopped { background: rgba(255, 255, 255, .04); color: var(--text-3); }
.dev-status.scrapped { background: rgba(255, 255, 255, .04); color: var(--text-3); }
.dev-meta {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-2);
}
.dev-meta span { display: flex; align-items: center; gap: 4px; }

.check-card { margin-top: 14px; padding: 16px; border-radius: var(--radius); }
.check-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14.5px;
  font-weight: 600;
  margin-bottom: 12px;
}
.check-dot {
  width: 14px;
  height: 2px;
  background: var(--ok);
}
.result-group { display: flex; width: 100%; }
.result-group :deep(.el-radio-button) { flex: 1; }
.result-group :deep(.el-radio-button__inner) { width: 100%; }
.check-btn {
  width: 100%;
  margin-top: 14px;
  height: 46px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.success-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(7, 13, 26, .8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.success-box { text-align: center; }
.success-ring {
  width: 84px;
  height: 84px;
  margin: 0 auto;
  border-radius: 50%;
  background: rgba(63, 182, 139, .08);
  border: 1px solid rgba(63, 182, 139, .4);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: popIn .4s cubic-bezier(.2, .8, .3, 1.2) both;
}
.success-title { font-size: 20px; font-weight: 700; margin-top: 16px; }
.success-sub { font-size: 13px; color: var(--text-2); margin: 6px 0 20px; }
@keyframes popIn {
  from { transform: scale(.5); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.pop-enter-active { transition: opacity .25s; }
.pop-enter-from { opacity: 0; }
</style>
