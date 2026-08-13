<template>
  <div class="m-page">
    <div class="bg-aurora"></div>
    <div class="m-body">
      <div class="m-logo">
        <el-icon :size="30" color="#fff"><Cpu /></el-icon>
      </div>
      <h1 class="m-title">智慧社区巡检</h1>
      <p class="m-sub">移动端巡检打卡平台</p>

      <div class="panel m-form">
        <el-input v-model="form.username" placeholder="用户名" size="large">
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
        <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password style="margin-top: 14px">
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
        <el-button class="m-btn" type="primary" size="large" :loading="loading" @click="onLogin">登 录</el-button>
      </div>

      <div class="m-tip">
        <span>演示账号：admin / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

// 已登录用户访问登录页 → 直接进入移动端首页
onMounted(() => {
  if (store.isLogin) {
    router.replace('/mobile/scan')
  }
})

const form = ref({ username: 'inspector', password: '123456' })
const loading = ref(false)

const onLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value)
    store.setLogin(data)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(redirect && redirect.startsWith('/mobile/') ? redirect : '/mobile/scan')
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
}
.m-body { position: relative; z-index: 1; width: min(88vw, 380px); text-align: center; }
.m-logo {
  width: 62px;
  height: 62px;
  margin: 0 auto 16px;
  border-radius: 14px;
  background: var(--bg-deep);
  border: 1px solid var(--line-2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.m-title { font-size: 22px; font-weight: 700; margin: 0 0 6px; letter-spacing: 1px; }
.m-sub { font-size: 13px; color: var(--text-3); margin: 0 0 26px; }
.m-form { padding: 24px 20px; border-radius: var(--radius); }
.m-btn {
  width: 100%;
  margin-top: 20px;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 8px;
}
.m-tip { margin-top: 18px; font-size: 12px; color: var(--text-3); }
</style>
