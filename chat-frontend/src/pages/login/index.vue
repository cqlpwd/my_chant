<template>
  <view class="login-page">
    <!-- 背景装饰 -->
    <view class="bg-decoration">
      <view class="bg-circle bg-circle-1"></view>
      <view class="bg-circle bg-circle-2"></view>
      <view class="bg-circle bg-circle-3"></view>
    </view>

    <!-- Logo 区域 -->
    <view class="logo-area">
      <view class="logo-icon">
        <text class="logo-emoji">💬</text>
      </view>
      <text class="app-name">Mychant</text>
      <text class="app-subtitle">随时随地，畅快聊天</text>
    </view>

    <!-- 登录表单卡片 -->
    <view class="form-card">
      <view class="form-title">欢迎回来</view>

      <view class="input-item">
        <view class="input-icon-wrap">
          <text class="input-icon">📱</text>
        </view>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          placeholder="请输入手机号"
          placeholder-class="placeholder"
          maxlength="11"
        />
      </view>

      <view class="input-item">
        <view class="input-icon-wrap">
          <text class="input-icon">🔒</text>
        </view>
        <input
          v-model="form.password"
          class="input"
          type="password"
          placeholder="请输入密码"
          placeholder-class="placeholder"
          maxlength="30"
        />
      </view>

      <button
        class="login-btn"
        :class="{ 'btn-active': canLogin }"
        :disabled="!canLogin || loading"
        :loading="loading"
        @click="handleLogin"
      >
        登录
      </button>

      <view class="register-link" @click="goRegister">
        还没有账号？<text class="link-text">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const form = ref({
  phone: '',
  password: ''
})
const loading = ref(false)

const PHONE_REGEX = /^1[3-9]\d{9}$/

const canLogin = computed(() => {
  return PHONE_REGEX.test(form.value.phone.trim()) && form.value.password.trim()
})

async function handleLogin() {
  if (!canLogin.value || loading.value) return

  loading.value = true
  try {
    await userStore.login(form.value.phone.trim(), form.value.password.trim())
    uni.showToast({ title: '登录成功', icon: 'success' })
    // 成功后保持 loading 锁定，避免跳转前的 500ms 窗口内再次点击触发重复请求
    setTimeout(() => {
      uni.switchTab({ url: '/pages/chat/list' })
    }, 500)
  } catch (e: any) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
    // 仅失败时解锁，允许重新尝试
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #e8f5e9 0%, #c8e6c9 30%, #a5d6a7 70%, #81c784 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆形 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
}

.bg-circle-1 {
  width: 500rpx;
  height: 500rpx;
  background: #07C160;
  top: -100rpx;
  right: -150rpx;
}

.bg-circle-2 {
  width: 360rpx;
  height: 360rpx;
  background: #43a047;
  bottom: -80rpx;
  left: -100rpx;
}

.bg-circle-3 {
  width: 200rpx;
  height: 200rpx;
  background: #66bb6a;
  top: 55%;
  right: -80rpx;
}

/* Logo 区域 */
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 50rpx;
  position: relative;
  z-index: 1;

  .logo-icon {
    width: 140rpx;
    height: 140rpx;
    background: linear-gradient(135deg, #07C160 0%, #00a854 100%);
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 28rpx;
    box-shadow: 0 12rpx 40rpx rgba(7, 193, 96, 0.35);
  }

  .logo-emoji {
    font-size: 64rpx;
  }

  .app-name {
    font-size: 40rpx;
    font-weight: 700;
    color: #2e7d32;
    letter-spacing: 4rpx;
  }

  .app-subtitle {
    font-size: 26rpx;
    color: #66bb6a;
    margin-top: 10rpx;
    letter-spacing: 2rpx;
  }
}

/* 表单卡片 */
.form-card {
  width: 86%;
  max-width: 640rpx;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20rpx);
  border-radius: 32rpx;
  padding: 50rpx 40rpx 40rpx;
  box-shadow: 0 8rpx 50rpx rgba(0, 0, 0, 0.06), 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  position: relative;
  z-index: 1;
}

.form-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 40rpx;
  text-align: center;
}

.input-item {
  display: flex;
  align-items: center;
  height: 100rpx;
  background: #f8faf8;
  border-radius: 20rpx;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s;

  &:focus-within {
    border-color: #07C160;
    background: #fff;
    box-shadow: 0 0 0 8rpx rgba(7, 193, 96, 0.06);
  }

  .input-icon-wrap {
    width: 56rpx;
    height: 56rpx;
    border-radius: 16rpx;
    background: #e8f5e9;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  .input-icon {
    font-size: 28rpx;
  }

  .input {
    flex: 1;
    height: 100%;
    font-size: 30rpx;
    color: #333;
  }

  .placeholder {
    color: #bdbdbd;
    font-size: 28rpx;
  }
}

.login-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  background: #e0e0e0;
  color: #999;
  font-size: 34rpx;
  font-weight: 600;
  border-radius: 24rpx;
  border: none;
  margin-top: 36rpx;
  transition: all 0.3s;

  &.btn-active {
    background: linear-gradient(135deg, #07C160 0%, #00a854 100%);
    color: #ffffff;
    box-shadow: 0 8rpx 30rpx rgba(7, 193, 96, 0.35);

    &:active {
      transform: scale(0.98);
      opacity: 0.9;
    }
  }

  &:disabled {
    background: #e0e0e0;
    color: #999;
  }
}

.register-link {
  margin-top: 36rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;

  .link-text {
    color: #07C160;
    font-weight: 600;
    margin-left: 6rpx;
  }
}
</style>
