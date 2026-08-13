<template>
  <div class="login-page">
    <div class="login-card">
      <!-- ============ 左：品牌区（纯品牌，无内容堆砌） ============ -->
      <div class="brand-panel">
        <div class="bp-brand">
          <div class="bp-mark">智</div>
          <div class="bp-meta">
            <div class="bp-name">智慧社区设备设施管理系统</div>
            <div class="bp-en">Equipment Lifecycle Platform</div>
          </div>
        </div>

        <div class="bp-slogan">
          设备设施全生命周期<br />管理平台
        </div>
        <div class="bp-desc">面向社区物业的设备管理数字化解决方案</div>

        <div class="bp-foot">© {{ year }} 智慧社区物业服务中心</div>
      </div>

      <!-- ============ 右：表单区 ============ -->
      <div class="form-panel">
        <div class="fp-title">欢迎回来</div>
        <div class="fp-sub">请输入账号密码登录系统</div>

        <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="onLogin" class="login-form">
          <el-form-item prop="username">
            <template #label><span class="field-label">用户名</span></template>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <template #label><span class="field-label">密码</span></template>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="onLogin">
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form>

        <!-- 演示账号（紧凑一行） -->
        <div class="demo">
          <div class="demo-label">演示账号（密码 123456）· 点击填充</div>
          <div class="demo-row">
            <button class="pill" @click="fill('admin', '123456')">
              <span class="pill-role">管理员</span>
              <code>admin</code>
            </button>
            <button class="pill" @click="fill('inspector', '123456')">
              <span class="pill-role">巡检员</span>
              <code>inspector</code>
            </button>
            <button class="pill" @click="fill('maintainer', '123456')">
              <span class="pill-role">维保员</span>
              <code>maintainer</code>
            </button>
          </div>
        </div>

        <div class="fp-foot">© {{ year }} 智慧社区物业服务中心</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const year = new Date().getFullYear()

const onLogin = async () => {
  try {
    await formRef.value.validate()
  } catch (e) { return }
  loading.value = true
  try {
    const data = await login(form.value)
    store.setLogin(data)
    ElMessage.success(`欢迎回来，${data.user.realName}`)
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const fill = (username, password) => {
  form.value.username = username
  form.value.password = password
  ElMessage.info(`已填充 ${username}，点击登录即可`)
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 28px 20px;
}

/* 大卡片：960px 分屏 */
.login-card {
  display: flex;
  width: 960px;
  max-width: 98vw;
  min-height: 600px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
  overflow: hidden;
  animation: cardIn .35s ease both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 左：品牌区（近黑，与侧栏同源，无装饰） ===== */
.brand-panel {
  width: 46%;
  padding: 48px 46px 36px;
  display: flex;
  flex-direction: column;
  background: #101012;
}
.bp-brand { display: flex; align-items: center; gap: 13px; }
.bp-mark {
  width: 44px;
  height: 44px;
  border-radius: 11px;
  background: #fff;
  color: #101012;
  font-size: 20px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.bp-name { font-size: 15px; font-weight: 700; color: #f4f4f5; letter-spacing: .01em; }
.bp-en {
  font-family: "Inter", sans-serif;
  font-size: 9.5px;
  letter-spacing: .14em;
  color: #71717a;
  margin-top: 3px;
  text-transform: uppercase;
}

.bp-slogan {
  margin-top: 72px;
  font-size: 30px;
  line-height: 1.45;
  font-weight: 700;
  letter-spacing: -.01em;
  color: #f4f4f5;
}
.bp-desc {
  margin-top: 16px;
  font-size: 13px;
  line-height: 1.8;
  color: #8b8b94;
}
.bp-foot {
  margin-top: auto;
  font-size: 11.5px;
  color: #565660;
  letter-spacing: .03em;
}

/* ===== 右：表单区 ===== */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 56px 36px;
}
.fp-title { font-size: 26px; font-weight: 700; color: var(--text-1); letter-spacing: -.01em; }
.fp-sub { font-size: 14px; color: var(--text-3); margin-top: 8px; }

.login-form { margin-top: 30px; }
.login-form :deep(.el-form-item) { margin-bottom: 22px; }
.login-form :deep(.el-form-item__label) {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 8px;
  line-height: 1.3;
  height: auto;
}
.login-form :deep(.el-input__wrapper) {
  padding: 6px 14px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px var(--line-2) inset;
  transition: box-shadow .15s ease;
}
.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--text-3) inset; }
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-dim-2);
}
.login-form :deep(.el-input__prefix) { margin-right: 10px; color: var(--text-3); }

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border-radius: 9px;
  margin-top: 6px;
}

/* 演示账号（紧凑胶囊） */
.demo { margin-top: 24px; }
.demo-label { font-size: 11.5px; color: var(--text-3); margin-bottom: 10px; }
.demo-row { display: flex; gap: 10px; }
.pill {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  transition: all .15s ease;
}
.pill:hover {
  border-color: var(--accent);
  background: var(--accent-dim);
}
.pill-role { font-size: 12px; font-weight: 600; color: var(--text-1); }
.pill code {
  font-family: "Inter", "HarmonyOS Sans SC", monospace;
  font-size: 11.5px;
  color: var(--text-3);
}

.fp-foot {
  margin-top: 28px;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
}

@media (max-width: 820px) {
  .brand-panel { display: none; }
}
</style>
