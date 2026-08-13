<template>
  <div class="nf-page">
    <div class="nf-card">
      <div class="nf-code stat-num">404</div>
      <div class="nf-title">页面不存在</div>
      <div class="nf-desc">您访问的页面可能已被移除或地址有误</div>
      <div class="nf-actions">
        <el-button type="primary" @click="$router.push('/dashboard')">
          <el-icon><HomeFilled /></el-icon>返回首页
        </el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.nf-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
}
.nf-card {
  text-align: center;
  padding: 60px 70px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-pop);
  animation: fadeUp .4s ease both;
}
.nf-code {
  font-size: 88px;
  font-weight: 600;
  line-height: 1;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: 4px;
}
.nf-title { font-size: 20px; font-weight: 600; margin-top: 18px; }
.nf-desc { font-size: 13.5px; color: var(--text-3); margin-top: 8px; }
.nf-actions { margin-top: 28px; display: flex; gap: 10px; justify-content: center; }
</style>
