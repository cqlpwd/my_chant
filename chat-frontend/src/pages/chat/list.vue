<template>
  <view class="chat-list-page page-tab">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <text class="nav-title">消息</text>
    </view>

    <!-- 会话列表 -->
    <scroll-view
      v-if="!loadError"
      class="conversation-list"
      scroll-y
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view
        v-for="conv in chatStore.mergedConversations"
        :key="(conv.type || 'private') + '-' + conv.friendId"
        class="conversation-item"
        @click="openChat(conv)"
        @longpress="showDeleteMenu(conv)"
      >
        <view class="avatar-wrap">
          <!-- 群聊：WeChat 风格拼接头像 -->
          <GroupAvatar
            v-if="conv.type === 'group'"
            class="avatar"
            :avatars="getGroupMemberAvatars(conv.groupId)"
            :size="100"
          />
          <!-- 私聊：普通头像 -->
          <image
            v-else
            class="avatar"
            :src="conv.friendAvatar || '/static/default-avatar.svg'"
            mode="aspectFill"
          />
          <view v-if="conv.friendStatus === 1 && conv.type !== 'group'" class="online-dot"></view>
        </view>

        <view class="conv-content">
          <view class="conv-top">
            <text class="conv-name">{{ conv.friendNickname }}</text>
            <text class="conv-time">{{ conv.lastMessageTime }}</text>
          </view>
          <view class="conv-bottom">
            <text class="conv-preview text-ellipsis">{{ formatLastMessage(conv) }}</text>
            <view v-if="conv.unreadCount > 0" class="unread-badge">
              {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-if="chatStore.conversations.length === 0 && !loading && !loadError" class="empty-state">
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无消息</text>
        <text class="empty-desc">去通讯录添加好友开始聊天吧</text>
      </view>
    </scroll-view>

    <!-- 加载错误状态（放在 scroll-view 外面，避免 click 被 scroll-view 拦截） -->
    <view v-if="loadError" class="error-state" @click="retryLoad">
      <text class="error-icon">⚠️</text>
      <text class="error-text">无法连接服务器</text>
      <text class="error-desc">请确认后端已启动后点击重试</text>
    </view>

    <CustomTabBar />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useChatStore, type Conversation } from '@/store/chat'
import { useUserStore } from '@/store/user'
import { useGroupStore } from '@/store/group'
import wsManager from '@/utils/websocket'
import CustomTabBar from '@/custom-tab-bar/index.vue'
import GroupAvatar from '@/components/GroupAvatar.vue'

const chatStore = useChatStore()
const userStore = useUserStore()
const groupStore = useGroupStore()
const refreshing = ref(false)
const loading = ref(false)
const loadError = ref(false)

let unsubWs: (() => void) | null = null

async function loadData() {
  loadError.value = false
  loading.value = true
  try {
    await chatStore.loadConversations()
  } catch (e) {
    loadError.value = true
    console.error('加载会话列表失败', e)
  } finally {
    loading.value = false
  }
}

function retryLoad() {
  loadData()
}

function bindWsHandler() {
  // 先解绑旧的，防止重复注册
  if (unsubWs) {
    unsubWs()
    unsubWs = null
  }
  unsubWs = wsManager.onMessage((msg) => {
    chatStore.handleWsMessage(msg)
  })
}

onMounted(async () => {
  await loadData()
  bindWsHandler()
})

// tabBar 页面每次显示时重新绑定，确保 WebSocket 断开重连后处理器仍然生效
onShow(async () => {
  bindWsHandler()
  // 每次回到列表页都静默刷新（同步未读数等，不显示 loading）
  try {
    await chatStore.loadConversations()
  } catch { /* 静默失败，不影响当前展示 */ }
  loadError.value = false
})

onHide(() => {
  if (unsubWs) {
    unsubWs()
    unsubWs = null
  }
})

function formatLastMessage(conv: Conversation): string {
  if (!conv.lastMessage) return ''
  let prefix = ''
  if (conv.type !== 'group' && conv.lastMessageSenderId === userStore.userInfo?.id) {
    prefix = '我: '
  }
  if (conv.lastMessageType === 1) return prefix + '[图片]'
  if (conv.lastMessageType === 2) return prefix + '[语音]'
  if (conv.lastMessageType === 4) return prefix + '[文件]'
  return prefix + (conv.lastMessage || '')
}

function openChat(conv: Conversation) {
  if (conv.type === 'group' && conv.groupId) {
    groupStore.clearGroupUnread(conv.groupId)
    uni.navigateTo({
      url: `/pages/group/detail?groupId=${conv.groupId}&groupName=${encodeURIComponent(conv.friendNickname)}`
    })
  } else {
    chatStore.readMessages(conv.friendId)
    uni.navigateTo({
      url: `/pages/chat/detail?friendId=${conv.friendId}&friendName=${encodeURIComponent(conv.friendNickname)}`
    })
  }
}



function showDeleteMenu(conv: Conversation) {
  if (conv.type === 'group') return // 群聊暂不支持从消息列表删除
  uni.showActionSheet({
    itemList: ['删除会话'],
    success: (res) => {
      if (res.tapIndex === 0) {
        chatStore.removeConversation(conv.friendId)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

async function onRefresh() {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

function loadMore() {
  // 分页加载，当前不做分页
}

/** 获取群聊的成员头像列表（用于拼接头像） */
function getGroupMemberAvatars(groupId?: number): string[] {
  if (!groupId) return []
  const group = groupStore.groups.find(g => g.id === groupId)
  return group?.memberAvatars || []
}
</script>

<style lang="scss" scoped>
.chat-list-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
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



.conversation-list {
  flex: 1;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  background-color: #ffffff;
  margin-bottom: 1rpx;

  &:active {
    background-color: #f5f5f5;
  }
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;

  .avatar {
    width: 100rpx;
    height: 100rpx;
    border-radius: 12rpx;
    background-color: #e0e0e0;
  }

  .online-dot {
    position: absolute;
    right: -4rpx;
    bottom: -4rpx;
    width: 20rpx;
    height: 20rpx;
    background-color: #07C160;
    border-radius: 50%;
    border: 4rpx solid #ffffff;
  }
}

.conv-content {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;

  .conv-name {
    font-size: 32rpx;
    color: #333;
    font-weight: 500;
  }

  .conv-time {
    font-size: 24rpx;
    color: #999;
  }
}

.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .conv-preview {
    flex: 1;
    font-size: 26rpx;
    color: #999;
    margin-right: 20rpx;
  }

  .unread-badge {
    min-width: 36rpx;
    height: 36rpx;
    line-height: 36rpx;
    background-color: #f56c6c;
    color: #fff;
    font-size: 22rpx;
    text-align: center;
    border-radius: 18rpx;
    padding: 0 10rpx;
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
