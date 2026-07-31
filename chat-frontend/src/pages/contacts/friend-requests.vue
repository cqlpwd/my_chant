<template>
  <view class="friend-requests-page">
    <!-- 好友请求列表 -->
    <view v-if="requests.length > 0" class="request-list">
      <view
        v-for="req in requests"
        :key="req.id"
        class="request-item"
      >
        <image
          class="avatar-sm"
          :src="req.senderAvatar || '/static/default-avatar.svg'"
          mode="aspectFill"
        />
        <view class="request-info">
          <text class="sender-name">{{ req.senderNickname }}</text>
          <text class="request-message text-ellipsis">{{ req.message }}</text>
          <text class="request-time">{{ req.createdAt }}</text>
        </view>
        <view class="request-actions">
          <button class="accept-btn" size="mini" @click="handleRequest(req.id, 1)">同意</button>
          <button class="reject-btn" size="mini" @click="handleRequest(req.id, 2)">拒绝</button>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <text class="empty-icon">📭</text>
      <text class="empty-text">没有新的好友请求</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFriendRequests, handleFriendRequest } from '@/utils/api'

const requests = ref<any[]>([])

onMounted(async () => {
  await loadRequests()
})

async function loadRequests() {
  try {
    const res = await getFriendRequests()
    if (res.code === 200 && res.data) {
      requests.value = res.data
    }
  } catch (e) {
    console.error('加载好友请求失败', e)
  }
}

async function handleRequest(requestId: number, status: number) {
  try {
    await handleFriendRequest(requestId, status)
    uni.showToast({
      title: status === 1 ? '已同意' : '已拒绝',
      icon: 'success'
    })
    await loadRequests()
  } catch (e: any) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.friend-requests-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.request-item {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.request-info {
  flex: 1;
  margin-left: 24rpx;
  min-width: 0;

  .sender-name {
    font-size: 30rpx;
    color: #333;
    display: block;
    margin-bottom: 6rpx;
  }

  .request-message {
    font-size: 24rpx;
    color: #999;
    display: block;
    margin-bottom: 4rpx;
  }

  .request-time {
    font-size: 22rpx;
    color: #bbb;
  }
}

.request-actions {
  display: flex;
  flex-direction: column;
  gap: 12rpx;

  .accept-btn {
    background-color: #07C160;
    color: #fff;
    border: none;
  }

  .reject-btn {
    background-color: #f5f5f5;
    color: #999;
    border: none;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 100rpx;
    margin-bottom: 30rpx;
  }

  .empty-text {
    font-size: 32rpx;
    color: #999;
  }
}
</style>
