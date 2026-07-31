<template>
  <view class="profile-page page-tab">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <text class="nav-title">我</text>
    </view>

    <!-- 个人信息卡片 -->
    <view class="profile-card" @click="goToEdit">
      <image
        class="avatar-lg"
        :src="userStore.userInfo?.avatar || '/static/default-avatar.svg'"
        mode="aspectFill"
      />
      <view class="profile-info">
        <text class="profile-name">{{ userStore.userInfo?.nickname || '未设置' }}</text>
        <text class="profile-account">用户名: {{ userStore.userInfo?.username || '' }}</text>
        <text class="profile-signature" v-if="userStore.userInfo?.signature">
          {{ userStore.userInfo.signature }}
        </text>
      </view>
      <text class="arrow-right">›</text>
    </view>

    <!-- 功能列表 -->
    <view class="section">
      <view class="section-item" @click="showToast">
        <text class="item-label">我的收藏</text>
        <text class="arrow">›</text>
      </view>
      <view class="section-item" @click="showToast">
        <text class="item-label">相册</text>
        <text class="arrow">›</text>
      </view>
      <view class="section-item" @click="showToast">
        <text class="item-label">文件</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section">
      <view class="section-item" @click="showToast">
        <text class="item-label">设置</text>
        <text class="arrow">›</text>
      </view>
      <view class="section-item" @click="showToast">
        <text class="item-label">关于</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

    <CustomTabBar />
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import CustomTabBar from '@/custom-tab-bar/index.vue'

const userStore = useUserStore()

function goToEdit() {
  uni.navigateTo({ url: '/pages/profile/edit' })
}

function showToast() {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.profile-page {
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

.profile-card {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  padding: 40rpx 30rpx;
  margin-bottom: 20rpx;

  &:active {
    background-color: #f5f5f5;
  }

  .avatar-lg {
    width: 120rpx;
    height: 120rpx;
    border-radius: 16rpx;
    margin-right: 30rpx;
  }

  .profile-info {
    flex: 1;

    .profile-name {
      font-size: 36rpx;
      color: #333;
      font-weight: bold;
      display: block;
      margin-bottom: 8rpx;
    }

    .profile-account {
      font-size: 26rpx;
      color: #999;
      display: block;
      margin-bottom: 8rpx;
    }

    .profile-signature {
      font-size: 26rpx;
      color: #666;
    }
  }

  .arrow-right {
    font-size: 40rpx;
    color: #ccc;
  }
}

.section {
  background-color: #ffffff;
  margin-bottom: 20rpx;
}

.section-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background-color: #f5f5f5;
  }

  .item-label {
    flex: 1;
    font-size: 30rpx;
    color: #333;
  }

  .arrow {
    font-size: 36rpx;
    color: #ccc;
  }
}

.logout-section {
  padding: 40rpx 30rpx;

  .logout-btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    background-color: #ffffff;
    color: #f56c6c;
    font-size: 32rpx;
    border-radius: 12rpx;
    border: none;
  }
}
</style>
