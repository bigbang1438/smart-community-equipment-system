import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('sc_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    // 二维码图片等二进制响应
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      // 二进制响应也可能是后端错误 JSON（如 401），识别后按错误处理
      const ct = String(response.headers['content-type'] || '')
      if (ct.includes('application/json') && res instanceof Blob && res.size > 0) {
        return res.text().then(text => {
          try {
            const err = JSON.parse(text)
            if (err.code === 401) {
              localStorage.removeItem('sc_token')
              localStorage.removeItem('sc_user')
              router.push('/login')
              ElMessage.error(err.message || '登录已过期')
            } else {
              ElMessage.error(err.message || '操作失败')
            }
            return Promise.reject(new Error(err.message))
          } catch (e) {
            return response
          }
        })
      }
      return response
    }
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('sc_token')
      localStorage.removeItem('sc_user')
      router.push('/login')
      ElMessage.error(res.message || '登录已过期')
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    ElMessage.error(error.message || '网络异常，请检查后端服务')
    return Promise.reject(error)
  }
)

export default request
