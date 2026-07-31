<template>
  <view class="group-list-page">
    <!-- 导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">群聊</text>
      </view>
      <view class="nav-right" @click="goCreateGroup">
        <text class="create-icon">+</text>
      </view>
    </view>

    <!-- 群列表 -->
    <view class="group-list">
      <view v-if="groupStore.groups.length === 0" class="empty-state">
        <text class="empty-icon">👥</text>
        <text class="empty-text">暂无群聊</text>
        <text class="empty-sub">点击右上角 + 创建群聊</text>
      </view>

      <view
        v-for="group in groupStore.groups"
        :key="group.id"
        class="group-item"
        @click="goGroupChat(group)"
      >
        <GroupAvatar
          class="group-avatar"
          :avatars="group.memberAvatars || []"
          :size="96"
        />
        <view class="group-info">
          <view class="group-header">
            <text class="group-name">{{ group.name }}</text>
            <text class="group-time">{{ group.lastMessageTime }}</text>
          </view>
          <view class="group-footer">
            <text class="group-last-msg">{{ group.lastMessage || group.memberCount + ' 名成员' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useGroupStore } from '@/store/group'
import GroupAvatar from '@/components/GroupAvatar.vue'

const groupStore = useGroupStore()

onMounted(() => {
  groupStore.loadGroups()
})

function goGroupChat(group: any) {
  uni.navigateTo({
    url: `/pages/group/detail?groupId=${group.id}&groupName=${encodeURIComponent(group.name)}`
  })
}

function goCreateGroup() {
  uni.navigateTo({ url: '/pages/group/create' })
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/chat/list' })
}
</script>

<style lang="scss" scoped>
.group-list-page {
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  height: 88rpx;
  padding-top: var(--status-bar-height, 44px);
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  border-bottom: 1rpx solid #e5e5e5;
}

.nav-left, .nav-right {
  width: 100rpx;
}

.nav-center {
  flex: 1;
  text-align: center;
}

.nav-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.back-icon {
  font-size: 40rpx;
  color: #333;
}

.create-icon {
  font-size: 50rpx;
  color: #07C160;
  font-weight: bold;
}

.group-list {
  flex: 1;
  background-color: #fff;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #999;
  margin-bottom: 10rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: #bbb;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.group-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 12rpx;
  background-color: #e0e0e0;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  margin-left: 24rpx;
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-name {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
}

.group-time {
  font-size: 22rpx;
  color: #bbb;
}

.group-footer {
  margin-top: 8rpx;
}

.group-last-msg {
  font-size: 26rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}
</style>
