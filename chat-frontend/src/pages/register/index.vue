<template>
  <view class="register-page">
    <!-- 背景装饰 -->
    <view class="bg-decoration">
      <view class="bg-circle bg-circle-1"></view>
      <view class="bg-circle bg-circle-2"></view>
      <view class="bg-circle bg-circle-3"></view>
    </view>

    <!-- Logo 区域 -->
    <view class="logo-area">
      <view class="logo-icon">
        <text class="logo-emoji">✨</text>
      </view>
      <text class="app-name">注册账号</text>
      <text class="app-subtitle">加入我们，开始聊天吧</text>
    </view>

    <!-- 注册表单卡片 -->
    <view class="form-card">
      <view class="form-title">创建新账号</view>

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
          <text class="input-icon">👤</text>
        </view>
        <input
          v-model="form.username"
          class="input"
          type="text"
          placeholder="请设置用户名（3-30位）"
          placeholder-class="placeholder"
          maxlength="30"
        />
      </view>

      <view class="input-item">
        <view class="input-icon-wrap">
          <text class="input-icon">😊</text>
        </view>
        <input
          v-model="form.nickname"
          class="input"
          type="text"
          placeholder="给自己起个名字"
          placeholder-class="placeholder"
          maxlength="30"
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
          placeholder="请设置密码（8-30位，字母+数字）"
          placeholder-class="placeholder"
          maxlength="30"
        />
      </view>

      <!-- 验证码 -->
      <view class="input-item">
        <view class="input-icon-wrap">
          <text class="input-icon">🔐</text>
        </view>
        <input
          v-model="form.captchaCode"
          class="input input-captcha"
          type="number"
          placeholder="请输入计算结果"
          placeholder-class="placeholder"
        />
        <view class="captcha-box" @click="refreshCaptcha">
          <text v-if="captchaQuestion" class="captcha-text">{{ captchaQuestion }}</text>
          <text v-else class="captcha-loading">点击获取</text>
        </view>
      </view>

      <view class="form-hint">
        <text class="hint-icon">💡</text>
        密码需包含字母和数字，至少8位
      </view>

      <button
        class="register-btn"
        :class="{ 'btn-active': canRegister }"
        :disabled="!canRegister || loading"
        :loading="loading"
        @click="handleRegister"
      >
        注册
      </button>

      <view class="login-link" @click="goBack">
        已有账号？<text class="link-text">去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCaptcha } from '@/utils/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const form = ref({
  username: '',
  nickname: '',
  password: '',
  phone: '',
  captchaCode: ''
})
const captchaKey = ref('')
const captchaQuestion = ref('')
const loading = ref(false)

const PHONE_REGEX = /^1[3-9]\d{9}$/

const canRegister = computed(() => {
  return (
    PHONE_REGEX.test(form.value.phone.trim()) &&
    form.value.username.trim().length >= 3 &&
    form.value.nickname.trim() &&
    form.value.password.trim().length >= 8 &&
    form.value.captchaCode.trim()
  )
})

onMounted(() => {
  refreshCaptcha()
})

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    if (res.data) {
      captchaKey.value = res.data.captchaKey
      captchaQuestion.value = res.data.captchaQuestion
    }
    form.value.captchaCode = ''
  } catch (e) {
    uni.showToast({ title: '获取验证码失败', icon: 'none' })
  }
}

async function handleRegister() {
  if (!canRegister.value || loading.value) return

  const phone = form.value.phone.trim()
  if (!PHONE_REGEX.test(phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(form.value.password.trim())) {
    uni.showToast({ title: '密码必须同时包含字母和数字', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.register(
      form.value.username.trim(),
      form.value.password.trim(),
      form.value.nickname.trim(),
      phone,
      captchaKey.value,
      Number(form.value.captchaCode.trim())
    )
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      goBack()
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e.message || '注册失败', icon: 'none' })
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
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
  opacity: 0.10;
}

.bg-circle-1 {
  width: 480rpx;
  height: 480rpx;
  background: #07C160;
  top: -80rpx;
  right: -120rpx;
}

.bg-circle-2 {
  width: 340rpx;
  height: 340rpx;
  background: #43a047;
  bottom: -60rpx;
  left: -80rpx;
}

.bg-circle-3 {
  width: 180rpx;
  height: 180rpx;
  background: #66bb6a;
  top: 40%;
  right: -60rpx;
}

/* Logo 区域 */
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
  position: relative;
  z-index: 1;

  .logo-icon {
    width: 130rpx;
    height: 130rpx;
    background: linear-gradient(135deg, #07C160 0%, #00a854 100%);
    border-radius: 34rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24rpx;
    box-shadow: 0 10rpx 36rpx rgba(7, 193, 96, 0.32);
  }

  .logo-emoji {
    font-size: 60rpx;
  }

  .app-name {
    font-size: 38rpx;
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
  padding: 44rpx 40rpx 40rpx;
  box-shadow: 0 8rpx 50rpx rgba(0, 0, 0, 0.06), 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  position: relative;
  z-index: 1;
}

.form-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 34rpx;
  text-align: center;
}

.input-item {
  display: flex;
  align-items: center;
  height: 96rpx;
  background: #f8faf8;
  border-radius: 20rpx;
  padding: 0 24rpx;
  margin-bottom: 20rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s;

  &:focus-within {
    border-color: #07C160;
    background: #fff;
    box-shadow: 0 0 0 8rpx rgba(7, 193, 96, 0.06);
  }

  .input-icon-wrap {
    width: 52rpx;
    height: 52rpx;
    border-radius: 14rpx;
    background: #e8f5e9;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 18rpx;
    flex-shrink: 0;
  }

  .input-icon {
    font-size: 26rpx;
  }

  .input {
    flex: 1;
    height: 100%;
    font-size: 28rpx;
    color: #333;
  }

  .input-captcha {
    max-width: 52%;
  }

  .placeholder {
    color: #bdbdbd;
    font-size: 26rpx;
  }

  .captcha-box {
    padding: 10rpx 22rpx;
    background: linear-gradient(135deg, rgba(7, 193, 96, 0.1), rgba(0, 168, 84, 0.1));
    border-radius: 14rpx;
    min-width: 170rpx;
    text-align: center;
    flex-shrink: 0;
    margin-left: 12rpx;
    border: 1rpx solid rgba(7, 193, 96, 0.15);
  }

  .captcha-text {
    font-size: 28rpx;
    color: #07C160;
    font-weight: 700;
    letter-spacing: 2rpx;
  }

  .captcha-loading {
    font-size: 24rpx;
    color: #999;
  }
}

.form-hint {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 24rpx;
  padding-left: 10rpx;

  .hint-icon {
    margin-right: 8rpx;
    font-size: 26rpx;
  }
}

.register-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  background: #e0e0e0;
  color: #999;
  font-size: 34rpx;
  font-weight: 600;
  border-radius: 24rpx;
  border: none;
  margin-top: 20rpx;
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

.login-link {
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
