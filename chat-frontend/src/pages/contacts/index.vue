<template>
  <view class="contacts-page page-tab">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <text class="nav-title">通讯录</text>
    </view>

    <!-- 功能入口 -->
    <view class="function-entries">
      <view class="entry-item" @click="goToFriendRequests">
        <view class="entry-icon icon-friend-request">
          <text class="icon-text">👤+</text>
        </view>
        <text class="entry-label">新的朋友</text>
        <view v-if="pendingCount > 0" class="entry-badge">{{ pendingCount }}</view>
        <text class="arrow">›</text>
      </view>

      <view class="entry-item" @click="goToAddFriend">
        <view class="entry-icon icon-add-friend">
          <text class="icon-text">🔍</text>
        </view>
        <text class="entry-label">添加好友</text>
        <text class="arrow">›</text>
      </view>

      <view class="entry-item" @click="goToGroups">
        <view class="entry-icon icon-group">
          <text class="icon-text">👥</text>
        </view>
        <text class="entry-label">群聊</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 好友列表 -->
    <scroll-view
      v-if="!loadError"
      class="friend-list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="section-title" v-if="friends.length > 0">
        <text>我的好友 ({{ friends.length }})</text>
      </view>

      <view
        v-for="friend in friends"
        :key="friend.id"
        class="friend-item"
        @click="openChat(friend)"
        @longpress="showFriendMenu(friend)"
      >
        <view class="avatar-wrap">
          <image
            class="avatar-sm"
            :src="friend.avatar || '/static/default-avatar.svg'"
            mode="aspectFill"
          />
          <view v-if="friend.status === 1" class="online-dot"></view>
        </view>
        <view class="friend-info">
          <text class="friend-name">{{ friend.nickname }}</text>
          <text class="friend-signature text-ellipsis">{{ friend.signature || '这个人很懒，什么都没写' }}</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-if="friends.length === 0 && !loading && !loadError" class="empty-state">
        <text class="empty-icon">👥</text>
        <text class="empty-text">暂无好友</text>
        <text class="empty-desc">快去添加好友开始聊天吧</text>
      </view>
    </scroll-view>

    <!-- 加载错误状态（放在 scroll-view 外面，避免 click 被拦截） -->
    <view v-if="loadError" class="error-state" @click="loadData">
      <text class="error-icon">⚠️</text>
      <text class="error-text">无法连接服务器</text>
      <text class="error-desc">请确认后端已启动后点击重试</text>
    </view>

    <CustomTabBar />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFriendList, getFriendRequests, deleteFriend } from '@/utils/api'
import CustomTabBar from '@/custom-tab-bar/index.vue'

const friends = ref<any[]>([])
const pendingCount = ref(0)
const refreshing = ref(false)
const loading = ref(false)
const loadError = ref(false)

onMounted(async () => {
  await loadData()
})

onShow(async () => {
  await loadData()
})

async function loadData() {
  loadError.value = false
  loading.value = true
  try {
    await loadFriends()
    await loadPendingCount()
  } catch (e) {
    loadError.value = true
    console.error('加载数据失败', e)
  } finally {
    loading.value = false
  }
}

async function loadFriends() {
  const res = await getFriendList()
  if (res.code === 200 && res.data) {
    friends.value = res.data
  }
}

async function loadPendingCount() {
  const res = await getFriendRequests()
  if (res.code === 200 && res.data) {
    pendingCount.value = res.data.length
  }
}

function openChat(friend: any) {
  uni.navigateTo({
    url: `/pages/chat/detail?friendId=${friend.id}&friendName=${encodeURIComponent(friend.nickname)}`
  })
}

function showFriendMenu(friend: any) {
  uni.showActionSheet({
    itemList: ['发消息', '删除好友'],
    success: async (res) => {
      if (res.tapIndex === 0) {
        openChat(friend)
      } else if (res.tapIndex === 1) {
        uni.showModal({
          title: '提示',
          content: `确定删除好友 ${friend.nickname} 吗？`,
          success: async (modalRes) => {
            if (modalRes.confirm) {
              try {
                await deleteFriend(friend.id)
                uni.showToast({ title: '删除成功', icon: 'success' })
                await loadFriends()
              } catch (e) {
                uni.showToast({ title: '删除失败', icon: 'none' })
              }
            }
          }
        })
      }
    }
  })
}

function goToFriendRequests() {
  uni.navigateTo({ url: '/pages/contacts/friend-requests' })
}

function goToAddFriend() {
  uni.navigateTo({ url: '/pages/contacts/add-friend' })
}

function goToGroups() {
  uni.navigateTo({ url: '/pages/group/list' })
}

async function onRefresh() {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}
</script>

<style lang="scss" scoped>
.contacts-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;
  border-radius: 32rpx;
  overflow: hidden;
}

.nav-bar {
  height: 88rpx;
  padding-top: var(--status-bar-height, 44px);
  background-color: #f0f0f0;
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

.function-entries {
  background-color: #ffffff;
  padding: 20rpx 0;
  margin-bottom: 20rpx;
}

.entry-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:active {
    background-color: #f5f5f5;
  }
}

.entry-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-friend-request {
  background-color: #fff3e0;
}

.icon-add-friend {
  background-color: #e8f5e9;
}

.icon-text {
  font-size: 36rpx;
}

.entry-label {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.entry-badge {
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  background-color: #f56c6c;
  color: #fff;
  font-size: 22rpx;
  text-align: center;
  border-radius: 18rpx;
  padding: 0 10rpx;
  margin-right: 10rpx;
}

.arrow {
  font-size: 40rpx;
  color: #ccc;
}

.friend-list {
  flex: 1;
  background-color: #ffffff;
}

.section-title {
  padding: 20rpx 30rpx;
  font-size: 26rpx;
  color: #999;
  background-color: #f5f5f5;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:active {
    background-color: #f5f5f5;
  }
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;

  .online-dot {
    position: absolute;
    right: -4rpx;
    bottom: -4rpx;
    width: 18rpx;
    height: 18rpx;
    background-color: #07C160;
    border-radius: 50%;
    border: 4rpx solid #ffffff;
  }
}

.friend-info {
  flex: 1;
  min-width: 0;

  .friend-name {
    font-size: 30rpx;
    color: #333;
    display: block;
    margin-bottom: 8rpx;
  }

  .friend-signature {
    font-size: 24rpx;
    color: #999;
    display: block;
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
    margin-bottom: 10rpx;
  }

  .empty-desc {
    font-size: 26rpx;
    color: #bbb;
  }
}

/* 错误状态 */
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  .error-icon {
    font-size: 100rpx;
    margin-bottom: 30rpx;
  }

  .error-text {
    font-size: 32rpx;
    color: #666;
    margin-bottom: 10rpx;
  }

  .error-desc {
    font-size: 26rpx;
    color: #07C160;
    text-decoration: underline;
  }
}
</style>
