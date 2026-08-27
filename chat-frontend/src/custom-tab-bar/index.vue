<template>
  <view class="custom-tab-bar">
    <!-- 消息 -->
    <view class="tab-item" :class="{ active: current === 'pages/chat/list' }" @click="switchTab('pages/chat/list')">
      <view class="tab-icon">
        <svg v-if="current === 'pages/chat/list'" width="26" height="26" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M24 7C15 7 8 13 8 21C8 25 10 29 14 32L11 40L21 35C22 35.5 23 35.7 24 35.7C33 35.7 40 29 40 21C40 13 33 7 24 7Z" fill="#07C160"/>
        </svg>
        <svg v-else width="26" height="26" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M24 7C15 7 8 13 8 21C8 25 10 29 14 32L11 40L21 35C22 35.5 23 35.7 24 35.7C33 35.7 40 29 40 21C40 13 33 7 24 7Z" stroke="#999999" stroke-width="3.2" fill="none" stroke-linejoin="round"/>
        </svg>
      </view>
      <text class="tab-text">消息</text>
      <view v-if="chatStore.unreadTotal > 0" class="tab-badge">
        {{ chatStore.unreadTotal > 99 ? '99+' : chatStore.unreadTotal }}
      </view>
    </view>

    <!-- 通讯录 -->
    <view class="tab-item" :class="{ active: current === 'pages/contacts/index' }" @click="switchTab('pages/contacts/index')">
      <view class="tab-icon">
        <svg width="26" height="26" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="18" cy="17" r="5.5" stroke="currentColor" stroke-width="3.2" fill="none"/>
          <path d="M8 39C8 30.5 12.5 25 18 25C23.5 25 28 30.5 28 39" stroke="currentColor" stroke-width="3.2" fill="none" stroke-linecap="round"/>
          <path d="M32 17H40" stroke="currentColor" stroke-width="3.2" stroke-linecap="round"/>
          <path d="M32 25H40" stroke="currentColor" stroke-width="3.2" stroke-linecap="round"/>
          <path d="M32 33H40" stroke="currentColor" stroke-width="3.2" stroke-linecap="round"/>
        </svg>
      </view>
      <text class="tab-text">通讯录</text>
    </view>

    <!-- 发现 -->
    <view class="tab-item" :class="{ active: current === 'pages/discover/index' }" @click="switchTab('pages/discover/index')">
      <view class="tab-icon">
        <svg width="26" height="26" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="24" cy="24" r="11" stroke="currentColor" stroke-width="3.2" fill="none"/>
          <path d="M24 24L31 15" stroke="currentColor" stroke-width="3.2" stroke-linecap="round"/>
          <path d="M24 24L17 33" stroke="currentColor" stroke-width="3.2" stroke-linecap="round"/>
        </svg>
      </view>
      <text class="tab-text">发现</text>
    </view>

    <!-- 我 -->
    <view class="tab-item" :class="{ active: current === 'pages/profile/index' }" @click="switchTab('pages/profile/index')">
      <view class="tab-icon">
        <svg width="26" height="26" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="24" cy="15" r="6.5" stroke="currentColor" stroke-width="3.2" fill="none"/>
          <path d="M10 40C10 31 16 25 24 25C32 25 38 31 38 40" stroke="currentColor" stroke-width="3.2" fill="none" stroke-linecap="round"/>
        </svg>
      </view>
      <text class="tab-text">我</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useChatStore } from '@/store/chat'

const chatStore = useChatStore()

const current = ref('pages/chat/list')

function syncActive() {
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const route = pages[pages.length - 1].route
    if (route) {
      if (route.includes('pages/chat/list')) current.value = 'pages/chat/list'
      else if (route.includes('pages/contacts/index')) current.value = 'pages/contacts/index'
      else if (route.includes('pages/discover/index')) current.value = 'pages/discover/index'
      else if (route.includes('pages/profile/index')) current.value = 'pages/profile/index'
    }
  }
}

function switchTab(path: string) {
  current.value = path
  uni.switchTab({ url: `/${path}` })
}

onMounted(() => {
  syncActive()
})
</script>

<style lang="scss" scoped>
.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background-color: #F7F7F7;
  border-top: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: 0 0 32rpx 32rpx;
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
  position: relative;
  padding-top: 4rpx;
}

.tab-icon {
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999999;
  transition: color 0.2s;

  svg {
    width: 52rpx;
    height: 52rpx;
  }
}

.tab-item.active .tab-icon {
  color: #07C160;
}

.tab-text {
  font-size: 20rpx;
  color: #999999;
  margin-top: 2rpx;
  transition: color 0.2s;
}

.tab-item.active .tab-text {
  color: #07C160;
}

.tab-badge {
  position: absolute;
  top: 4rpx;
  right: 50%;
  transform: translateX(36rpx);
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  background-color: #F43530;
  color: #fff;
  font-size: 18rpx;
  text-align: center;
  border-radius: 16rpx;
  padding: 0 8rpx;
}
</style>
