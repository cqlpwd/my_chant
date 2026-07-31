/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser } from '@/utils/api'
import wsManager from '@/utils/websocket'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref<any>(null)
  const token = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录
   */
  async function login(phone: string, password: string) {
    const res = await loginApi({ phone, password })
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      userInfo.value = res.data.user

      // 保存到本地存储
      uni.setStorageSync('userInfo', {
        token: res.data.token,
        user: res.data.user
      })

      // 连接 WebSocket
      wsManager.connect(res.data.token, String(res.data.user.id)).catch(() => {
        console.warn('WebSocket 连接失败')
      })

      return res.data
    }
    throw new Error(res.message || '登录失败')
  }

  /**
   * 注册
   */
  async function register(username: string, password: string, nickname: string, phone: string, captchaKey: string, captchaCode: number) {
    const { register } = await import('@/utils/api')
    const res = await register({ username, password, nickname, phone, captchaKey, captchaCode })
    if (res.code === 200) {
      return res.data
    }
    throw new Error(res.message || '注册失败')
  }

  /**
   * 检查登录状态
   */
  function checkLogin() {
    try {
      const stored = uni.getStorageSync('userInfo')
      if (stored && stored.token) {
        token.value = stored.token
        userInfo.value = stored.user

        // 重新连接 WebSocket
        wsManager.connect(stored.token, String(stored.user.id)).catch(() => { })
      }
    } catch (e) {
      // ignore
    }
  }

  /**
   * 刷新用户信息
   */
  async function refreshUserInfo() {
    try {
      const res = await getCurrentUser()
      if (res.code === 200 && res.data) {
        userInfo.value = res.data

        // 更新本地存储
        const stored = uni.getStorageSync('userInfo') || {}
        stored.user = res.data
        uni.setStorageSync('userInfo', stored)
      }
    } catch (e) {
      console.warn('刷新用户信息失败')
    }
  }

  /**
   * 退出登录
   */
  function logout() {
    token.value = ''
    userInfo.value = null
    wsManager.disconnect()
    uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/login/index' })
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    login,
    register,
    checkLogin,
    refreshUserInfo,
    logout
  }
})
