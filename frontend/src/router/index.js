import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '智慧总览' }
      },
      {
        path: 'device',
        name: 'DeviceList',
        component: () => import('@/views/device/DeviceList.vue'),
        meta: { title: '设备台账' }
      },
      {
        path: 'device/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/device/DeviceDetail.vue'),
        meta: { title: '设备详情' }
      },
      {
        path: 'task/inspect',
        name: 'InspectList',
        component: () => import('@/views/task/TaskList.vue'),
        meta: { title: '巡检管理' }
      },
      {
        path: 'task/maintain',
        name: 'MaintainList',
        component: () => import('@/views/task/MaintainList.vue'),
        meta: { title: '保养管理' }
      },
      {
        path: 'repair',
        name: 'RepairList',
        component: () => import('@/views/repair/RepairList.vue'),
        meta: { title: '报修工单' }
      },
      {
        path: 'contract',
        name: 'ContractList',
        component: () => import('@/views/contract/ContractList.vue'),
        meta: { title: '维保合同' }
      },
      {
        path: 'monitor',
        name: 'MonitorBoard',
        component: () => import('@/views/monitor/MonitorBoard.vue'),
        meta: { title: '运行监测' }
      },
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/analysis/Analysis.vue'),
        meta: { title: '年限分析' }
      },
      {
        path: 'system/user',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理' }
      }
    ]
  },
  {
    path: '/mobile/login',
    name: 'MobileLogin',
    component: () => import('@/views/mobile/MobileLogin.vue'),
    meta: { title: '移动端登录' }
  },
  {
    path: '/mobile/scan',
    name: 'MobileScan',
    component: () => import('@/views/mobile/MobileScan.vue'),
    meta: { title: '扫码巡检' }
  },
  {
    path: '/mobile/tasks',
    name: 'MobileTasks',
    component: () => import('@/views/mobile/MobileTasks.vue'),
    meta: { title: '我的任务' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} · 智慧社区设备设施管理系统`
  const store = useUserStore()
  // 手机访问主系统 → 自动进入移动端（避免桌面版在小屏上错乱）
  const isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)
  if (isMobile && !to.path.startsWith('/mobile/') && !to.path.startsWith('/login')) {
    next({ path: '/mobile/login', query: { redirect: to.fullPath } })
    return
  }
  // 移动端扫码页、登录页允许未登录访问
  const publicPages = ['Login', 'MobileLogin', 'MobileScan']
  if (!publicPages.includes(to.name) && !store.isLogin) {
    // 移动端页面未登录 → 跳移动端登录页，保持移动端体验
    if (to.path.startsWith('/mobile/')) {
      next({ path: '/mobile/login', query: { redirect: to.fullPath } })
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  } else {
    next()
  }
})

export default router
