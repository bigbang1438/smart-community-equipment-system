import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => {
    let saved = null
    try { saved = JSON.parse(localStorage.getItem('sc_user') || 'null') } catch (e) { saved = null }
    return {
      token: localStorage.getItem('sc_token') || '',
      user: saved
    }
  },
  getters: {
    isLogin: (s) => !!s.token,
    role: (s) => s.user?.role || '',
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    setLogin({ token, user }) {
      this.token = token
      this.user = user
      localStorage.setItem('sc_token', token)
      localStorage.setItem('sc_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('sc_token')
      localStorage.removeItem('sc_user')
    }
  }
})
