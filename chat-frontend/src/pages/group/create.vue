<template>
  <view class="create-group-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="back-text">取消</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">创建群聊</text>
      </view>
      <view class="nav-right" @click="submitCreate">
        <text class="confirm-text" :class="{ active: groupName && selectedIds.length > 0 }">创建</text>
      </view>
    </view>

    <view class="form-section">
      <view class="section-title">群名称</view>
      <input v-model="groupName" class="name-input" placeholder="请输入群名称" placeholder-class="placeholder" />
    </view>

    <view class="form-section">
      <view class="section-title">选择成员（至少 1 人）</view>
      <view class="selected-tags" v-if="selectedIds.length > 0">
        <view v-for="id in selectedIds" :key="id" class="tag" @click="removeMember(id)">
          <text>{{ getFriendName(id) }}</text>
          <text class="tag-remove">×</text>
        </view>
      </view>
    </view>

    <view class="section-title pad-left">好友列表</view>
    <view class="friend-list">
      <view v-if="friends.length === 0" class="empty-tip">
        <text>暂无好友，请先添加好友</text>
      </view>
      <view v-for="f in friends" :key="f.id" class="friend-item" @click="toggleSelect(f.id)">
        <image class="friend-avatar" :src="f.avatar || '/static/default-avatar.svg'" mode="aspectFill" />
        <view class="friend-info">
          <text class="friend-name">{{ f.nickname }}</text>
        </view>
        <view class="check-box" :class="{ checked: selectedIds.includes(f.id) }">
          <text v-if="selectedIds.includes(f.id)" class="check-icon">✓</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFriendList } from '@/utils/api'
import { useGroupStore } from '@/store/group'

const groupStore = useGroupStore()
const groupName = ref('')
const friends = ref<any[]>([])
const selectedIds = ref<number[]>([])

onMounted(async () => {
  try {
    const res = await getFriendList()
    if (res.code === 200 && res.data) friends.value = res.data
  } catch { }
})

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function removeMember(id: number) { toggleSelect(id) }

function getFriendName(id: number): string {
  return friends.value.find(f => f.id === id)?.nickname || ''
}

async function submitCreate() {
  if (!groupName.value.trim()) { uni.showToast({ title: '请输入群名称', icon: 'none' }); return }
  if (selectedIds.value.length === 0) { uni.showToast({ title: '请至少选择一位好友', icon: 'none' }); return }
  const groupId = await groupStore.doCreateGroup(groupName.value.trim(), selectedIds.value)
  if (groupId) {
    uni.redirectTo({ url: `/pages/group/detail?groupId=${groupId}&groupName=${encodeURIComponent(groupName.value)}` })
  }
}

function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
.create-group-page { height: 100vh; background-color: #f5f5f5; }
.nav-bar { height: 88rpx; padding-top: var(--status-bar-height, 44px); background-color: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 30rpx; border-bottom: 1rpx solid #e5e5e5; }
.nav-left, .nav-right { width: 120rpx; }
.nav-center { flex: 1; text-align: center; }
.nav-title { font-size: 34rpx; font-weight: bold; color: #333; }
.back-text { font-size: 30rpx; color: #333; }
.confirm-text { font-size: 30rpx; color: #ccc; &.active { color: #07C160; font-weight: bold; } }
.form-section { background-color: #fff; margin-bottom: 16rpx; padding: 24rpx 30rpx; }
.section-title { font-size: 26rpx; color: #999; margin-bottom: 16rpx; &.pad-left { padding: 24rpx 30rpx 0; } }
.name-input { font-size: 30rpx; color: #333; border-bottom: 1rpx solid #e5e5e5; padding: 12rpx 0; }
.placeholder { color: #bbb; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 12rpx; }
.tag { display: flex; align-items: center; background-color: #e8f5e9; color: #07C160; font-size: 24rpx; padding: 8rpx 16rpx; border-radius: 8rpx; }
.tag-remove { margin-left: 8rpx; font-size: 28rpx; font-weight: bold; }
.friend-list { background-color: #fff; }
.empty-tip { padding: 60rpx; text-align: center; color: #999; font-size: 28rpx; }
.friend-item { display: flex; align-items: center; padding: 24rpx 30rpx; border-bottom: 1rpx solid #f0f0f0; }
.friend-avatar { width: 80rpx; height: 80rpx; border-radius: 8rpx; background-color: #e0e0e0; }
.friend-info { flex: 1; margin-left: 24rpx; }
.friend-name { font-size: 30rpx; color: #333; }
.check-box { width: 40rpx; height: 40rpx; border: 2rpx solid #ccc; border-radius: 50%; display: flex; align-items: center; justify-content: center; &.checked { background-color: #07C160; border-color: #07C160; } .check-icon { font-size: 24rpx; color: #fff; } }
</style>
