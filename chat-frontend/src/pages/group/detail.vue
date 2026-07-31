<template>
  <view class="group-chat-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center" @click="goGroupInfo">
        <GroupAvatar class="nav-avatar" :avatars="memberAvatars" :size="64" />
        <view class="nav-text">
          <text class="nav-title">{{ groupName }}</text>
          <text class="nav-sub">{{ groupStore.currentGroupInfo?.memberCount || 0 }} 人</text>
        </view>
      </view>
      <view class="nav-right" @click="goGroupInfo">
        <text class="more-icon">···</text>
      </view>
    </view>

    <scroll-view class="message-list" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <!-- 时间标签 -->
      <template v-for="(msg, index) in displayMessages" :key="msg.id || index">
        <view v-if="msg.showTime" class="time-divider">
          <text class="time-text">{{ msg.timeLabel }}</text>
        </view>
        <!-- 他人消息 -->
        <view v-if="msg.senderId !== userId" class="message-row msg-received">
          <view class="avatar-col">
            <image v-if="msg.showAvatar" class="msg-avatar" :src="msg.senderAvatar || '/static/default-avatar.svg'" mode="aspectFill" />
            <view v-else class="avatar-placeholder" />
          </view>
          <view class="msg-body">
            <text v-if="msg.showName" class="msg-name">{{ msg.senderNickname || '未知' }}</text>
            <view class="msg-bubble msg-bubble-received">
              <text v-if="msg.messageType === 0" class="msg-text">{{ msg.content }}</text>
              <image v-else-if="msg.messageType === 1" :src="msg.content" mode="widthFix" class="msg-image" />
              <text v-else class="msg-text">[其他消息]</text>
            </view>
          </view>
        </view>
        <!-- 自己消息 -->
        <view v-else class="message-row msg-sent">
          <view class="msg-body msg-body-sent">
            <view class="msg-bubble msg-bubble-sent">
              <text v-if="msg.messageType === 0" class="msg-text">{{ msg.content }}</text>
              <image v-else-if="msg.messageType === 1" :src="msg.content" mode="widthFix" class="msg-image" />
              <text v-else class="msg-text">[其他消息]</text>
            </view>
          </view>
          <view class="avatar-col">
            <image v-if="msg.showAvatar" class="msg-avatar" :src="userStore.userInfo?.avatar || '/static/default-avatar.svg'" mode="aspectFill" />
            <view v-else class="avatar-placeholder" />
          </view>
        </view>
      </template>
      <view class="end-tip" v-if="groupStore.currentMessages.length > 0">
        <text class="end-text">— 以上是全部消息 —</text>
      </view>
    </scroll-view>

    <view class="input-bar">
      <view class="input-box">
        <input v-model="inputText" class="msg-input" type="text" placeholder="输入消息..." placeholder-class="input-placeholder" confirm-type="send" @confirm="sendMsg" />
      </view>
      <button v-if="inputText.trim()" class="send-btn" @click="sendMsg">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useGroupStore } from '@/store/group'
import { useUserStore } from '@/store/user'
import wsManager from '@/utils/websocket'
import GroupAvatar from '@/components/GroupAvatar.vue'

const groupStore = useGroupStore()
const userStore = useUserStore()

const groupId = ref<number>(0)
const groupName = ref('')
const inputText = ref('')
const scrollTop = ref(0)

const userId = computed(() => userStore.userInfo?.id || 0)

/** 成员头像列表（用于导航栏拼接头像） */
const memberAvatars = computed(() => {
  return groupStore.currentMembers.map(m => m.avatar || '').filter(a => a)
})

/** 计算连续消息合并信息，模仿微信样式 */
const displayMessages = computed(() => {
  const msgs = groupStore.currentMessages
  return msgs.map((msg, index) => {
    const prev = index > 0 ? msgs[index - 1] : null
    // 同一人连续发消息（间隔 60 秒内）合并头像和昵称
    const isSameSender = prev && prev.senderId === msg.senderId && timeDiff(prev.createdAt, msg.createdAt) <= 60
    const showAvatar = !isSameSender
    const showName = showAvatar && msg.senderId !== userId.value
    // 间隔超过 5 分钟显示时间标签
    const showTime = !prev || timeDiff(prev.createdAt, msg.createdAt) > 300
    const timeLabel = showTime ? formatMsgTime(msg.createdAt) : ''
    return { ...msg, showAvatar, showName, showTime, timeLabel }
  })
})

function timeDiff(t1: string, t2: string): number {
  try {
    return Math.abs(new Date(t2).getTime() - new Date(t1).getTime()) / 1000
  } catch { return 999 }
}

function formatMsgTime(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    const now = new Date()
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const isToday = d.toDateString() === now.toDateString()
    const yesterday = new Date(now.getTime() - 86400000)
    const isYesterday = d.toDateString() === yesterday.toDateString()
    if (isToday) return `${h}:${m}`
    if (isYesterday) return `昨天 ${h}:${m}`
    if (d.getFullYear() === now.getFullYear()) return `${month}-${day} ${h}:${m}`
    return `${d.getFullYear()}-${month}-${day} ${h}:${m}`
  } catch { return dateStr }
}

let unsubWs: (() => void) | null = null

onLoad((options: any) => {
  groupId.value = parseInt(options.groupId)
  groupName.value = decodeURIComponent(options.groupName || '')
})

onMounted(async () => {
  await groupStore.loadGroupDetail(groupId.value)
  await groupStore.loadGroupHistory(groupId.value)
  groupStore.clearGroupUnread(groupId.value)
  scrollToBottom()

  unsubWs = wsManager.onMessage((msg) => {
    groupStore.handleGroupWsMessage(msg)
  })
})

onUnmounted(() => {
  if (unsubWs) { unsubWs(); unsubWs = null }
  // 离开群聊页面时重置 currentGroupId，确保消息列表页能正确显示未读提醒
  groupStore.currentGroupId = 0
})

function sendMsg() {
  const text = inputText.value.trim()
  if (!text) return
  groupStore.sendMessage(groupId.value, text, 0)
  inputText.value = ''
  nextTick(() => scrollToBottom())
}

function goGroupInfo() {
  uni.navigateTo({ url: `/pages/group/info?groupId=${groupId.value}` })
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/chat/list' })
}

function scrollToBottom() {
  nextTick(() => { scrollTop.value = 999999 })
}
</script>

<style lang="scss" scoped>
.group-chat-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background-color: #ededed;
}

/* 顶部导航栏 — 固定 */
.nav-bar {
  flex-shrink: 0;
  padding-top: var(--status-bar-height, 44px);
  height: calc(88rpx + var(--status-bar-height, 44px));
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 30rpx;
  padding-right: 30rpx;
  border-bottom: 1rpx solid #e5e5e5;
}
.nav-left, .nav-right { width: 100rpx; }
.nav-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.nav-avatar { width: 64rpx; height: 64rpx; border-radius: 8rpx; margin-right: 12rpx; flex-shrink: 0; }
.nav-text { display: flex; flex-direction: column; align-items: flex-start; overflow: hidden; }
.nav-title { font-size: 34rpx; font-weight: bold; color: #333; }
.nav-sub { font-size: 22rpx; color: #999; display: block; }
.back-icon { font-size: 40rpx; color: #333; }
.more-icon { font-size: 36rpx; color: #333; letter-spacing: 4rpx; }

/* 消息列表 — 可滚动 */
.message-list {
  flex: 1;
  padding: 16rpx 20rpx;
  overflow: hidden;
}

/* 时间分隔 */
.time-divider { display: flex; justify-content: center; margin: 20rpx 0; }
.time-text { font-size: 22rpx; color: #b0b0b0; background-color: #dcdcdc; padding: 6rpx 16rpx; border-radius: 8rpx; }

/* 消息行 */
.message-row { display: flex; margin-bottom: 4rpx; align-items: flex-start; }
.msg-sent { justify-content: flex-end; }
.msg-received { justify-content: flex-start; }

/* 头像列（固定宽度保持对齐） */
.avatar-col { width: 80rpx; flex-shrink: 0; }
.msg-received .avatar-col { margin-right: 14rpx; display: flex; justify-content: flex-start; }
.msg-sent .avatar-col { margin-left: 14rpx; display: flex; justify-content: flex-end; }

.avatar-placeholder { width: 72rpx; height: 72rpx; }

.msg-avatar { width: 72rpx; height: 72rpx; border-radius: 8rpx; flex-shrink: 0; background-color: #e0e0e0; }

/* 消息体 */
.msg-body { display: flex; flex-direction: column; max-width: 70%; }
.msg-body-sent { align-items: flex-end; }

.msg-name { font-size: 22rpx; color: #576b95; margin-bottom: 4rpx; margin-left: 8rpx; line-height: 1.2; }

.msg-bubble { padding: 16rpx 22rpx; border-radius: 12rpx; }
.msg-bubble-received { background-color: #fff; border-top-left-radius: 4rpx; }
.msg-bubble-sent { background-color: #95ec69; border-top-right-radius: 4rpx; }

.msg-text { font-size: 30rpx; color: #333; word-break: break-all; line-height: 1.5; }
.msg-image { max-width: 300rpx; border-radius: 8rpx; }

.end-tip { text-align: center; padding: 30rpx; .end-text { font-size: 24rpx; color: #ccc; } }

/* 底部输入栏 — 固定 */
.input-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  background-color: #f7f7f7;
  padding: 12rpx 20rpx;
  padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e5e5e5;
}
.input-box { flex: 1; margin-right: 16rpx; }
.msg-input { height: 72rpx; background-color: #fff; border-radius: 8rpx; padding: 0 20rpx; font-size: 30rpx; }
.input-placeholder { color: #bbb; }
.send-btn { height: 64rpx; line-height: 64rpx; padding: 0 28rpx; background-color: #07C160; color: #fff; font-size: 28rpx; border-radius: 10rpx; border: none; }
</style>
