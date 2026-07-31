<template>
  <view class="chat-detail-page">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">{{ friendName }}</text>
        <text v-if="typingText" class="typing-hint">{{ typingText }}</text>
      </view>
      <view class="nav-right" @click="showFriendInfo">
        <text class="more-icon">···</text>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      class="message-list"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
      @scroll="onScroll"
    >
      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore">
        <text class="load-text">下拉加载更多</text>
      </view>

      <view
        v-for="(msg, index) in chatStore.currentMessages"
        :key="msg.id || index"
        class="message-row"
        :class="msg.senderId === userId ? 'message-sent' : 'message-received'"
      >
        <!-- 收到的消息：头像在左边 -->
        <template v-if="msg.senderId !== userId">
          <image
            class="msg-avatar"
            :src="msg.senderAvatar || '/static/default-avatar.svg'"
            mode="aspectFill"
          />
          <view class="msg-body">
            <text class="msg-sender-name">{{ msg.senderNickname || friendName }}</text>
            <view class="msg-bubble msg-bubble-received">
              <text v-if="msg.messageType === 0" class="msg-text">{{ msg.content }}</text>
              <image v-else-if="msg.messageType === 1" :src="msg.content" mode="widthFix" class="msg-image" />
              <text v-else-if="msg.messageType === 2" class="msg-text">[语音]</text>
              <text v-else-if="msg.messageType === 4" class="msg-text">[文件]</text>
            </view>
          </view>
        </template>

        <!-- 发出的消息：头像在右边 -->
        <template v-else>
          <view class="msg-bubble msg-bubble-sent">
            <text v-if="msg.messageType === 0" class="msg-text">{{ msg.content }}</text>
            <image v-else-if="msg.messageType === 1" :src="msg.content" mode="widthFix" class="msg-image" />
            <text v-else-if="msg.messageType === 2" class="msg-text">[语音]</text>
            <text v-else-if="msg.messageType === 4" class="msg-text">[文件]</text>
          </view>
          <image
            class="msg-avatar"
            :src="userStore.userInfo?.avatar || '/static/default-avatar.svg'"
            mode="aspectFill"
          />
        </template>
      </view>

      <!-- 到底了 -->
      <view class="end-tip" v-if="chatStore.currentMessages.length > 0">
        <text class="end-text">— 以上是全部消息 —</text>
      </view>
    </scroll-view>

    <!-- 底部输入栏 -->
    <view class="input-bar">
      <view class="input-tools">
        <text class="tool-icon" @click="openImagePicker">🖼️</text>
      </view>
      <view class="input-box">
        <input
          v-model="inputText"
          class="message-input"
          type="text"
          placeholder="输入消息..."
          placeholder-class="input-placeholder"
          confirm-type="send"
          @confirm="sendTextMessage"
          @input="onInput"
        />
      </view>
      <view class="input-actions">
        <button
          v-if="inputText.trim()"
          class="send-btn"
          @click="sendTextMessage"
        >
          发送
        </button>
        <text v-else class="tool-icon" @click="openEmoji">😊</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'
import { uploadImage } from '@/utils/api'
import wsManager from '@/utils/websocket'

const chatStore = useChatStore()
const userStore = useUserStore()

const friendId = ref<number>(0)
const friendName = ref<string>('')
const inputText = ref('')
const scrollTop = ref(0)
const hasMore = ref(false)
const typingTimer = ref<any>(null)

const userId = computed(() => userStore.userInfo?.id || 0)
const typingText = computed(() => {
  return chatStore.typingUserId === friendId.value ? '对方正在输入...' : ''
})

let unsubWs: (() => void) | null = null

onLoad((options: any) => {
  friendId.value = parseInt(options.friendId)
  friendName.value = decodeURIComponent(options.friendName || '')
})

onMounted(async () => {
  try {
    await chatStore.loadChatHistory(friendId.value)
    // 标记已读
    await chatStore.readMessages(friendId.value)
  } catch (e) {
    uni.showToast({ title: '加载聊天记录失败，请下拉重试', icon: 'none', duration: 2000 })
  }

  // 注册 WebSocket 消息处理
  unsubWs = wsManager.onMessage((msg) => {
    chatStore.handleWsMessage(msg)
  })

  // 滚动到底部
  scrollToBottom()
})

onUnmounted(() => {
  if (typingTimer.value) clearTimeout(typingTimer.value)
  if (unsubWs) {
    unsubWs()
    unsubWs = null
  }
  // 重置 currentFriendId，确保消息列表页能正确显示未读提醒
  chatStore.currentFriendId = 0
})

function sendTextMessage() {
  const text = inputText.value.trim()
  if (!text) return

  chatStore.sendMessage(friendId.value, text, 0)
  inputText.value = ''

  // 立即滚动到底部
  nextTick(() => scrollToBottom())
}

function onInput() {
  // 发送"正在输入"状态
  wsManager.sendTyping(friendId.value, true)

  if (typingTimer.value) clearTimeout(typingTimer.value)
  typingTimer.value = setTimeout(() => {
    wsManager.sendTyping(friendId.value, false)
  }, 2000)
}

async function openImagePicker() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      try {
        const result = await uploadImage(filePath)
        if (result.data?.url) {
          chatStore.sendMessage(friendId.value, result.data.url, 1)
          nextTick(() => scrollToBottom())
        }
      } catch (e) {
        console.error('图片上传失败', e)
      }
    }
  })
}

function openEmoji() {
  uni.showToast({ title: '表情功能开发中', icon: 'none' })
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    // 页面栈为空时回退到消息 tab
    uni.switchTab({ url: '/pages/chat/list' })
  }
}

function showFriendInfo() {
  uni.showToast({ title: '查看好友信息', icon: 'none' })
}

function scrollToBottom() {
  nextTick(() => {
    scrollTop.value = 999999
  })
}

function onScroll() {
  // 滚动事件
}
</script>

<style lang="scss" scoped>
.chat-detail-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;
}

/* 导航栏 — 固定 */
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

  .nav-left, .nav-right {
    width: 100rpx;
  }

  .nav-center {
    flex: 1;
    text-align: center;

    .nav-title {
      font-size: 34rpx;
      font-weight: bold;
      color: #333;
    }

    .typing-hint {
      font-size: 22rpx;
      color: #07C160;
      display: block;
      margin-top: 4rpx;
    }
  }

  .back-icon {
    font-size: 40rpx;
    color: #333;
  }

  .more-icon {
    font-size: 36rpx;
    color: #333;
    letter-spacing: 4rpx;
  }
}

/* 消息列表 — 可滚动 */
.message-list {
  flex: 1;
  padding: 20rpx 24rpx;
  overflow: hidden;
}

.load-more {
  text-align: center;
  padding: 20rpx;

  .load-text {
    font-size: 24rpx;
    color: #999;
  }
}

.message-row {
  display: flex;
  margin-bottom: 30rpx;
  align-items: flex-start;
}

.message-sent {
  justify-content: flex-end;
}

.message-received {
  justify-content: flex-start;
}

.msg-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
  background-color: #e0e0e0;
}

.message-sent .msg-avatar {
  margin-left: 16rpx;
}

.message-received .msg-avatar {
  margin-right: 16rpx;
}

.msg-body {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.msg-sender-name {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 6rpx;
  margin-left: 8rpx;
}

.msg-bubble {
  max-width: 70%;
  padding: 18rpx 24rpx;
  border-radius: 12rpx;
  position: relative;
}

.msg-bubble-received {
  background-color: #ffffff;
  border-top-left-radius: 4rpx;
}

.msg-bubble-sent {
  background-color: #95ec69;
  border-top-right-radius: 4rpx;
}

.msg-text {
  font-size: 30rpx;
  color: #333;
  word-break: break-all;
  line-height: 1.5;
}

.msg-image {
  max-width: 300rpx;
  border-radius: 8rpx;
}

.end-tip {
  text-align: center;
  padding: 30rpx;

  .end-text {
    font-size: 24rpx;
    color: #ccc;
  }
}

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

.tool-icon {
  font-size: 48rpx;
  padding: 10rpx;
}

.input-box {
  flex: 1;
  margin: 0 16rpx;

  .message-input {
    height: 72rpx;
    background-color: #ffffff;
    border-radius: 8rpx;
    padding: 0 20rpx;
    font-size: 30rpx;
  }

  .input-placeholder {
    color: #bbb;
  }
}

.input-actions {
  display: flex;
  align-items: center;
}

.send-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 28rpx;
  background-color: #07C160;
  color: #ffffff;
  font-size: 28rpx;
  border-radius: 10rpx;
  border: none;
}
</style>
