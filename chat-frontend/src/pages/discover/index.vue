<template>
  <view class="discover-page page-tab">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <text class="nav-title">发现</text>
    </view>

    <!-- 朋友圈入口 -->
    <view class="section">
      <view class="section-item" @click="goMoments">
        <view class="item-icon icon-moments">🟢</view>
        <text class="item-label">朋友圈</text>
        <view class="item-right">
          <view class="preview-dot" v-if="false"></view>
          <image class="preview-avatar avatar-sm" v-if="userStore.userInfo?.avatar"
            :src="userStore.userInfo.avatar" mode="aspectFill" />
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- AI 入口 -->
    <view class="section">
      <view class="section-item" @click="goAskAI">
        <view class="item-icon icon-ai">🤖</view>
        <text class="item-label">询问AI</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="section">
      <view class="section-item" @click="showToast('扫一扫')">
        <view class="item-icon icon-scan">📷</view>
        <text class="item-label">扫一扫</text>
        <text class="arrow">›</text>
      </view>
      <view class="section-item" @click="showToast('小程序')">
        <view class="item-icon icon-miniapp">📱</view>
        <text class="item-label">小程序</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section">
      <view class="section-item" @click="showToast('附近的人')">
        <view class="item-icon icon-nearby">📍</view>
        <text class="item-label">附近的人</text>
        <text class="arrow">›</text>
      </view>
      <view class="section-item" @click="showToast('摇一摇')">
        <view class="item-icon icon-shake">📳</view>
        <text class="item-label">摇一摇</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <CustomTabBar />
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import CustomTabBar from '@/custom-tab-bar/index.vue'

const userStore = useUserStore()

function goMoments() {
  uni.navigateTo({ url: '/pages/moments/index' })
}

function goAskAI() {
  uni.navigateTo({ url: '/pages/ai/chat' })
}

function showToast(title: string) {
  uni.showToast({ title: `${title}功能开发中`, icon: 'none' })
}
</script>

<style lang="scss" scoped>
.discover-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  height: 88rpx;
  padding-top: var(--status-bar-height, 44px);
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1rpx solid #e5e5e5;

  .nav-title {
    font-size: 36rpx;
    font-weight: bold;
    color: #333;
  }
}

.section {
  background-color: #ffffff;
  margin-bottom: 20rpx;
}

.section-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background-color: #f5f5f5;
  }
}

.item-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  font-size: 32rpx;
}

.item-label {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.item-right {
  display: flex;
  align-items: center;
}

.preview-dot {
  width: 16rpx;
  height: 16rpx;
  background-color: #f56c6c;
  border-radius: 50%;
  margin-right: 10rpx;
}

.preview-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 8rpx;
  margin-right: 10rpx;
}

.arrow {
  font-size: 36rpx;
  color: #ccc;
}
</style>
