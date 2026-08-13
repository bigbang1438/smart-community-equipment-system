import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const changePassword = (data) => request.put('/auth/password', data)

// 设备
export const devicePage = (params) => request.get('/device/page', { params })
export const deviceList = () => request.get('/device/list')
export const deviceDetail = (id) => request.get(`/device/${id}`)
export const deviceByCode = (code) => request.get('/device/byCode', { params: { code } })
export const addDevice = (data) => request.post('/device', data)
export const updateDevice = (id, data) => request.put(`/device/${id}`, data)
export const deleteDevice = (id) => request.delete(`/device/${id}`)
export const deviceQr = async (id, size = 300) => {
  const res = await request.get(`/device/qr/${id}`, { params: { size }, responseType: 'blob' })
  return res.data
}

// 任务
export const taskPage = (params) => request.get('/task/page', { params })
export const taskToday = () => request.get('/task/today')
export const generateTask = (data) => request.post('/task/generate', data)
export const refreshOverdue = () => request.post('/task/refreshOverdue')
export const checkTask = (data) => request.post('/task/check', data)
export const checkByDevice = (data) => request.post('/task/checkByDevice', data)
export const myTasks = (params) => request.get('/task/my', { params })
export const tasksByDevice = (deviceId) => request.get(`/task/device/${deviceId}`)

// 报修工单
export const repairPage = (params) => request.get('/repair/page', { params })
export const repairStats = () => request.get('/repair/stats')
export const repairDetail = (id) => request.get(`/repair/${id}`)
export const createRepair = (data) => request.post('/repair', data)
export const assignRepair = (id, data) => request.post(`/repair/${id}/assign`, data)
export const finishRepair = (id, data) => request.post(`/repair/${id}/finish`, data)
export const verifyRepair = (id) => request.post(`/repair/${id}/verify`)
export const deleteRepair = (id) => request.delete(`/repair/${id}`)

// 合同
export const contractPage = (params) => request.get('/contract/page', { params })
export const contractReminders = () => request.get('/contract/reminders')
export const addContract = (data) => request.post('/contract', data)
export const updateContract = (id, data) => request.put(`/contract/${id}`, data)
export const deleteContract = (id) => request.delete(`/contract/${id}`)

// 监测
export const monitorStatus = () => request.get('/monitor/device-status')
export const monitorRealtime = (deviceId, points = 30) =>
  request.get(`/monitor/realtime/${deviceId}`, { params: { points } })
export const monitorHistory = (deviceId, metric) =>
  request.get(`/monitor/history/${deviceId}`, { params: { metric } })
export const monitorAlerts = () => request.get('/monitor/alerts')

// 统计
export const statsOverview = () => request.get('/stats/overview')
export const statsDeviceType = () => request.get('/stats/device-type')
export const statsDeviceStatus = () => request.get('/stats/device-status')
export const statsHealthRank = (limit = 10) => request.get('/stats/health-rank', { params: { limit } })
export const statsCostTrend = (months = 6) => request.get('/stats/cost-trend', { params: { months } })
export const statsLifecycle = () => request.get('/stats/lifecycle')
export const statsRepairTrend = (months = 6) => request.get('/stats/repair-trend', { params: { months } })
export const statsTaskCompletion = (days = 14, type = 'INSPECT') =>
  request.get('/stats/task-completion', { params: { days, type } })

// 用户
export const userPage = (params) => request.get('/user/page', { params })
export const maintainerList = () => request.get('/user/maintainers')
export const addUser = (data) => request.post('/user', data)
export const updateUser = (id, data) => request.put(`/user/${id}`, data)
export const deleteUser = (id) => request.delete(`/user/${id}`)
