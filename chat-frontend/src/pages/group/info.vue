<template>
  <view class="group-info-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">群聊信息</text>
      </view>
      <view class="nav-right"></view>
    </view>

    <view class="group-header">
      <GroupAvatar
        class="group-avatar"
        :avatars="memberAvatars"
        :size="120"
      />
      <text class="group-name">{{ groupStore.currentGroupInfo?.name }}</text>
      <text class="group-id">群号: {{ groupStore.currentGroupInfo?.id }}</text>
    </view>

    <view class="info-section">
      <view class="section-title">群公告</view>
      <view class="section-content">
        <text>{{ groupStore.currentGroupInfo?.announcement || '暂无群公告' }}</text>
      </view>
    </view>

    <view class="info-section">
      <view class="section-title">群成员（{{ groupStore.currentMembers.length }}）</view>
      <view class="member-list">
        <view v-for="m in groupStore.currentMembers" :key="m.userId" class="member-item">
          <image class="member-avatar" :src="m.avatar || '/static/default-avatar.svg'" mode="aspectFill" />
          <view class="member-info">
            <text class="member-name">
              {{ m.nickname || '成员' }}
              <text v-if="m.role === 2" class="role-tag owner">群主</text>
              <text v-else-if="m.role === 1" class="role-tag admin">管理员</text>
            </text>
          </view>
        </view>
      </view>
      <view class="action-row" @click="showInvite">
        <text class="action-text" style="color: #07C160">+ 邀请好友</text>
      </view>
    </view>

    <view class="action-section">
      <view class="action-item danger" @click="handleLeave">
        <text>退出群聊</text>
      </view>
      <view v-if="isOwner" class="action-item danger" @click="handleDismiss">
        <text>解散群聊</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useGroupStore } from '@/store/group'
import { useUserStore } from '@/store/user'
import { getFriendList } from '@/utils/api'
import GroupAvatar from '@/components/GroupAvatar.vue'

const groupStore = useGroupStore()
const userStore = useUserStore()
const groupId = ref<number>(0)
const userId = computed(() => userStore.userInfo?.id || 0)
const isOwner = computed(() => groupStore.currentGroupInfo?.ownerId === userId.value)

/** 成员头像列表（用于拼接头像） */
const memberAvatars = computed(() => {
  return groupStore.currentMembers.map(m => m.avatar || '').filter(a => a)
})

onLoad((options: any) => {
  groupId.value = parseInt(options.groupId)
})

onMounted(async () => {
  await groupStore.loadGroupDetail(groupId.value)
})

function handleLeave() {
  uni.showModal({
    title: '退出群聊',
    content: '确定退出该群聊吗？',
    success: async (res) => {
      if (res.confirm) {
        await groupStore.doLeaveGroup(groupId.value)
        uni.navigateBack()
      }
    }
  })
}

function handleDismiss() {
  uni.showModal({
    title: '解散群聊',
    content: '确定解散该群聊吗？此操作不可撤销。',
    success: async (res) => {
      if (res.confirm) {
        await groupStore.doDismissGroup(groupId.value)
        uni.navigateBack()
      }
    }
  })
}

async function showInvite() {
  try {
    const res = await getFriendList()
    const friends = res.data || []
    const memberSet = new Set(groupStore.currentMembers.map(m => m.userId))
    const available = friends.filter((f: any) => !memberSet.has(f.friendId))

    if (available.length === 0) {
      uni.showToast({ title: '没有可邀请的好友', icon: 'none' })
      return
    }

    const names = available.map((f: any) => f.friendNickname)
    uni.showActionSheet({
      itemList: names,
      success: async (r) => {
        const friend = available[r.tapIndex]
        await groupStore.doAddMembers(groupId.value, [friend.friendId])
      }
    })
  } catch { }
}

function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
.group-info-page { height: 100vh; background-color: #f5f5f5; }
.nav-bar { height: 88rpx; padding-top: var(--status-bar-height, 44px); background-color: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 30rpx; border-bottom: 1rpx solid #e5e5e5; }
.nav-left, .nav-right { width: 100rpx; }
.nav-center { flex: 1; text-align: center; }
.nav-title { font-size: 34rpx; font-weight: bold; color: #333; }
.back-icon { font-size: 40rpx; color: #333; }
.group-header { background-color: #fff; padding: 40rpx 30rpx; display: flex; flex-direction: column; align-items: center; margin-bottom: 16rpx; }
.group-avatar { width: 120rpx; height: 120rpx; border-radius: 16rpx; background-color: #e0e0e0; margin-bottom: 16rpx; }
.group-name { font-size: 34rpx; font-weight: bold; color: #333; margin-bottom: 8rpx; }
.group-id { font-size: 24rpx; color: #bbb; }
.info-section { background-color: #fff; margin-bottom: 16rpx; padding: 24rpx 30rpx; }
.section-title { font-size: 26rpx; color: #999; margin-bottom: 16rpx; }
.section-content { font-size: 28rpx; color: #666; line-height: 1.6; }
.member-list { display: flex; flex-wrap: wrap; gap: 20rpx; }
.member-item { display: flex; align-items: center; width: calc(50% - 10rpx); padding: 12rpx 0; }
.member-avatar { width: 64rpx; height: 64rpx; border-radius: 8rpx; background-color: #e0e0e0; }
.member-info { margin-left: 16rpx; flex: 1; }
.member-name { font-size: 28rpx; color: #333; display: flex; align-items: center; gap: 8rpx; }
.role-tag { font-size: 20rpx; padding: 2rpx 8rpx; border-radius: 4rpx; }
.role-tag.owner { background-color: #fff3e0; color: #ff9800; }
.role-tag.admin { background-color: #e3f2fd; color: #2196f3; }
.action-row { padding: 16rpx 0; border-top: 1rpx solid #f0f0f0; margin-top: 16rpx; }
.action-text { font-size: 28rpx; }
.action-section { background-color: #fff; }
.action-item { padding: 28rpx 30rpx; text-align: center; font-size: 30rpx; border-bottom: 1rpx solid #f0f0f0; }
.action-item.danger { color: #e74c3c; }
</style>
