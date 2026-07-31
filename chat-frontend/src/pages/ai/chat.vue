<template>
  <view class="ai-chat-page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-left" @click="goBack">
        <text class="nav-back">←</text>
      </view>
      <text class="nav-title">AI 助手</text>
      <view class="nav-right"></view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      class="chat-area"
      scroll-y
      :scroll-into-view="scrollToId"
      :scroll-with-animation="true"
    >
      <view v-if="messages.length === 0" class="welcome-area">
        <text class="welcome-icon">🤖</text>
        <text class="welcome-text">你好！我是 DeepSeek AI 助手</text>
        <text class="welcome-sub">有什么我可以帮你的吗？</text>
      </view>

      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        :id="'msg-' + idx"
        class="message-row"
        :class="msg.role === 'user' ? 'msg-right' : 'msg-left'"
      >
        <image
          v-if="msg.role === 'assistant'"
          class="msg-avatar"
          src="/static/default-avatar.png"
          mode="aspectFill"
        />
        <view class="msg-bubble" :class="msg.role === 'user' ? 'bubble-right' : 'bubble-left'">
          <text class="msg-text">{{ msg.content }}</text>
        </view>
        <image
          v-if="msg.role === 'user' && userStore.userInfo?.avatar"
          class="msg-avatar"
          :src="userStore.userInfo.avatar"
          mode="aspectFill"
        />
      </view>

      <view v-if="loading" class="typing-indicator">
        <text>AI 正在思考...</text>
      </view>

      <view id="msg-bottom" style="height: 20rpx;"></view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-bar">
      <input
        class="msg-input"
        v-model="inputText"
        placeholder="输入消息..."
        confirm-type="send"
        :disabled="loading"
        @confirm="sendMessage"
      />
      <view class="send-btn" :class="{ disabled: loading || !inputText.trim() }" @click="sendMessage">
        <text>发送</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { askAI } from '@/utils/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const scrollToId = ref('')

function scrollToBottom() {
  nextTick(() => {
    scrollToId.value = 'msg-bottom'
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await askAI(messages.value)
    if (res.data?.reply) {
      messages.value.push({ role: 'assistant', content: res.data.reply })
    }
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂时不可用，请稍后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/discover/index' })
  }
}
</script>

<style lang="scss" scoped>
.ai-chat-page {
  min-height: 100vh;
  background-color: #ededed;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  background-color: #ededed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 30rpx;
  height: 88rpx;
  border-bottom: 1rpx solid #d9d9d9;
  flex-shrink: 0;

  .nav-left, .nav-right {
    width: 80rpx;
    display: flex;
    align-items: center;
  }

  .nav-back {
    font-size: 40rpx;
    color: #333;
  }

  .nav-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #333;
  }
}

.chat-area {
  flex: 1;
  padding: 20rpx 24rpx;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .welcome-icon {
    font-size: 100rpx;
    margin-bottom: 30rpx;
  }

  .welcome-text {
    font-size: 32rpx;
    color: #333;
    font-weight: bold;
    margin-bottom: 10rpx;
  }

  .welcome-sub {
    font-size: 26rpx;
    color: #999;
  }
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 28rpx;

  &.msg-right {
    flex-direction: row-reverse;
  }
}

.msg-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 8rpx;
  background-color: #ddd;
  flex-shrink: 0;
}

.msg-left .msg-avatar {
  margin-right: 16rpx;
}

.msg-right .msg-avatar {
  margin-left: 16rpx;
}

.msg-bubble {
  max-width: 70%;
  padding: 18rpx 24rpx;
  border-radius: 12rpx;
  position: relative;

  .msg-text {
    font-size: 30rpx;
    line-height: 1.5;
    word-break: break-all;
  }
}

.bubble-left {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 4rpx;
}

.bubble-right {
  background-color: #95ec69;
  color: #000;
  border-top-right-radius: 4rpx;
}

.typing-indicator {
  text-align: center;
  padding: 16rpx;
  font-size: 24rpx;
  color: #999;
}

.input-bar {
  display: flex;
  align-items: center;
  background-color: #f7f7f7;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e0e0e0;
  flex-shrink: 0;

  .msg-input {
    flex: 1;
    height: 70rpx;
    background-color: #fff;
    border-radius: 10rpx;
    padding: 0 20rpx;
    font-size: 30rpx;
    color: #333;
  }

  .send-btn {
    width: 120rpx;
    height: 70rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #07C160;
    border-radius: 10rpx;
    margin-left: 16rpx;

    text {
      font-size: 28rpx;
      color: #fff;
      font-weight: bold;
    }

    &.disabled {
      background-color: #a8e6c1;
    }
  }
}
</style>
