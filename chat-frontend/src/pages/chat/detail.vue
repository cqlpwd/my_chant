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
      <view class="nav-right">
        <text class="more-icon" @click="showFriendInfo">···</text>
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

    <!-- 视频通话 -->
    <CallOverlay @accept="acceptCall" @reject="rejectCall" @hangup="hangupCall" @close="closeCallOverlay" />

    <!-- 底部输入栏 -->
    <view class="input-bar">
      <view class="input-tools">
        <text class="tool-icon" :class="{ 'tool-icon-active': showToolsPanel }" @click="toggleToolsPanel">
          {{ showToolsPanel ? '✕' : '➕' }}
        </text>
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
          class="send-btn"
          :class="{ 'send-btn-disabled': !inputText.trim() }"
          :disabled="!inputText.trim()"
          @click="sendTextMessage"
        >
          发送
        </button>
      </view>
    </view>

    <!-- 工具抽屉弹层（图片 / 语音 / 视频） -->
    <view v-if="showToolsPanel" class="tools-mask" @click="closeToolsPanel"></view>
    <view class="tools-panel" :class="{ 'tools-panel-show': showToolsPanel }">
      <view class="tools-panel-inner">
        <!-- 图片 -->
        <view class="tool-item" @click="openImagePicker">
          <view class="tool-item-icon tool-item-photo">🖼️</view>
          <text class="tool-item-label">图片</text>
        </view>
        <!-- 语音通话 -->
        <view class="tool-item" @click="startAudioCall">
          <view class="tool-item-icon tool-item-audio">📞</view>
          <text class="tool-item-label">语音通话</text>
        </view>
        <!-- 视频通话 -->
        <view class="tool-item" @click="startVideoCall">
          <view class="tool-item-icon tool-item-video">📹</view>
          <text class="tool-item-label">视频通话</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'
import { useCallStore } from '@/store/call'
import { uploadImage } from '@/utils/api'
import wsManager from '@/utils/websocket'
import rtcService from '@/utils/rtc'
import CallOverlay from '@/components/CallOverlay.vue'

const chatStore = useChatStore()
const userStore = useUserStore()
const callStore = useCallStore()

/** 来电时暂存的 offer SDP，等待用户接听 */
let pendingSdp: RTCSessionDescriptionInit | null = null
/** 响铃定时器 */
let ringTimer: number | null = null
let ringCtx: AudioContext | null = null

const friendId = ref<number>(0)
const friendName = ref<string>('')
const inputText = ref('')
const scrollTop = ref(0)
const hasMore = ref(false)
const typingTimer = ref<any>(null)
/** 工具抽屉（图片/视频）是否展开 */
const showToolsPanel = ref(false)

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
    handleCallSignal(msg)
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
  // 页面销毁时停止响铃并释放媒体资源
  stopRingtone()
  rtcService.hangup()
  callStore.reset()
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

/** 开关工具抽屉 */
function toggleToolsPanel() {
  showToolsPanel.value = !showToolsPanel.value
}

/** 关闭工具抽屉 */
function closeToolsPanel() {
  showToolsPanel.value = false
}

async function openImagePicker() {
  closeToolsPanel()
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

// ==================== 视频通话（H5 WebRTC） ====================

/** 主叫：发起语音通话 */
async function startAudioCall() {
  closeToolsPanel()
  if (callStore.phase !== 'idle') return
  callStore.isVideo = false
  callStore.phase = 'outgoing'
  callStore.peerId = friendId.value
  callStore.peerName = friendName.value
  callStore.peerAvatar = ''
  try {
    await rtcService.startCall(friendId.value, false)
  } catch (e) {
    console.error('发起呼叫失败', e)
    uni.showToast({
      title: (e as Error)?.name === 'NotAllowedError'
        ? '麦克风权限被拒绝'
        : '请使用 localhost 或 HTTPS 访问以启用语音通话',
      icon: 'none',
      duration: 3000
    })
    endCall('呼叫失败')
  }
}

/** 主叫：发起视频通话 */
async function startVideoCall() {
  closeToolsPanel()
  if (callStore.phase !== 'idle') return
  callStore.isVideo = true
  callStore.phase = 'outgoing'
  callStore.peerId = friendId.value
  callStore.peerName = friendName.value
  callStore.peerAvatar = ''
  try {
    await rtcService.startCall(friendId.value, true)
  } catch (e) {
    console.error('发起呼叫失败', e)
    uni.showToast({
      title: (e as Error)?.name === 'NotAllowedError'
        ? '摄像头/麦克风权限被拒绝'
        : '请使用 localhost 或 HTTPS 访问以启用音视频',
      icon: 'none',
      duration: 3000
    })
    endCall('呼叫失败')
  }
}

/** 被叫：接听 */
async function acceptCall() {
  try {
    await rtcService.answerCall(callStore.peerId, pendingSdp!, callStore.isVideo)
    callStore.phase = 'active'
    stopRingtone()
  } catch (e) {
    console.error('接听失败', e)
    uni.showToast({
      title: (e as Error)?.name === 'NotAllowedError'
        ? '摄像头/麦克风权限被拒绝'
        : '请使用 localhost 或 HTTPS 访问以启用音视频',
      icon: 'none',
      duration: 3000
    })
    rejectCall()
  }
}

/** 被叫：拒绝 */
function rejectCall() {
  wsManager.sendCallSignal(callStore.peerId, 'CALL_REJECT', {})
  endCall('已拒绝')
}

/** 双方：挂断 */
function hangupCall() {
  wsManager.sendCallSignal(callStore.peerId, 'CALL_HANGUP', {})
  endCall('通话已结束')
}

/** 统一结束通话 */
function endCall(reason: string) {
  stopRingtone()
  rtcService.hangup()
  callStore.endReason = reason
  callStore.phase = 'ended'
  setTimeout(() => {
    // 仅当仍处于"已结束"状态时才自动复位，避免覆盖新的通话状态
    if (callStore.phase === 'ended') {
      callStore.reset()
    }
  }, 1500)
}

/** 通话结束界面：手动关闭 */
function closeCallOverlay() {
  stopRingtone()
  rtcService.hangup()
  callStore.reset()
}

/** 处理 WebSocket 通话信令 */
async function handleCallSignal(msg: any) {
  const type = msg?.type

  // 通话已结束（idle/ended）后到达的迟到信令一律忽略，
  // 避免把界面从"已结束"状态拉回"通话中"导致关闭不了
  if (type !== 'CALL_OFFER' && callStore.phase === 'idle') return

  if (type === 'CALL_OFFER') {
    if (callStore.phase !== 'idle') {
      // 自己正在通话/呼叫中，回忙线
      wsManager.sendCallSignal(msg.fromId, 'CALL_BUSY', {})
      return
    }
    pendingSdp = msg.sdp
    callStore.isVideo = msg.video !== false
    callStore.phase = 'incoming'
    callStore.peerId = msg.fromId
    callStore.peerName = msg.fromNickname || friendName.value
    callStore.peerAvatar = msg.fromAvatar || ''
    startRingtone()
  } else if (type === 'CALL_ANSWER') {
    // 只有主叫呼叫中（outgoing）才接受 answer，取消后迟到的 answer 直接忽略
    if (callStore.phase !== 'outgoing') return
    stopRingtone()
    callStore.phase = 'active'
    await rtcService.handleAnswer(msg.sdp)
  } else if (type === 'CALL_ICE') {
    // 只有通话中才接收 ICE 候选
    if (callStore.phase !== 'active') return
    await rtcService.handleIce(msg.candidate)
  } else if (type === 'CALL_HANGUP') {
    endCall('对方已挂断')
  } else if (type === 'CALL_REJECT') {
    endCall('对方已拒绝')
  } else if (type === 'CALL_BUSY') {
    endCall('对方忙线')
  }
}

/** 响铃（Web Audio 生成嘟嘟声，避免引入音频文件） */
function startRingtone() {
  stopRingtone()
  const win = window as any
  const Ctx = win.AudioContext || win.webkitAudioContext
  if (!Ctx) return
  ringCtx = new Ctx()
  const beep = () => {
    if (!ringCtx) return
    const osc = ringCtx.createOscillator()
    const gain = ringCtx.createGain()
    osc.type = 'sine'
    osc.frequency.value = 880
    gain.gain.setValueAtTime(0.001, ringCtx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.25, ringCtx.currentTime + 0.02)
    gain.gain.exponentialRampToValueAtTime(0.001, ringCtx.currentTime + 0.8)
    osc.connect(gain)
    gain.connect(ringCtx.destination)
    osc.start()
    osc.stop(ringCtx.currentTime + 0.9)
  }
  beep()
  ringTimer = window.setInterval(beep, 1200)
}

function stopRingtone() {
  if (ringTimer) {
    clearInterval(ringTimer)
    ringTimer = null
  }
  if (ringCtx) {
    try { ringCtx.close() } catch (e) { /* 忽略 */ }
    ringCtx = null
  }
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
  border-radius: 32rpx;
  overflow: hidden;
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
  border-radius: 50%;
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
  padding: 20rpx 28rpx;
  border-radius: 24rpx;
  position: relative;
}

.msg-bubble-received {
  background-color: #ffffff;
  border-top-left-radius: 10rpx;
}

.msg-bubble-sent {
  background-color: #95ec69;
  border-top-right-radius: 10rpx;
}

.msg-text {
  font-size: 30rpx;
  color: #333;
  word-break: break-all;
  line-height: 1.5;
}

.msg-image {
  max-width: 300rpx;
  border-radius: 20rpx;
}

.end-tip {
  text-align: center;
  padding: 30rpx;

  .end-text {
    font-size: 24rpx;
    color: #ccc;
  }
}

/* 底部输入栏 — 固定（微信风格） */
.input-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  background-color: #f7f7f7;
  padding: 14rpx 16rpx;
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e5e5e5;
}

.input-tools {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-left: 8rpx;
}

.tool-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  line-height: 1;
  border-radius: 50%;
  background-color: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;

  &.tool-icon-active {
    transform: rotate(90deg);
  }
}

.input-box {
  flex: 1;
  margin: 0 16rpx;

  .message-input {
    height: 72rpx;
    background-color: #ffffff;
    border-radius: 36rpx;
    padding: 0 30rpx;
    font-size: 30rpx;
    box-shadow: inset 0 1rpx 2rpx rgba(0, 0, 0, 0.04);
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
  padding: 0 32rpx;
  background-color: #07C160;
  color: #ffffff;
  font-size: 28rpx;
  border-radius: 32rpx;
  border: none;

  &.send-btn-disabled {
    background-color: #d0d0d0;
  }
}

/* ==================== 工具抽屉弹层 ==================== */
.tools-mask {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  background-color: rgba(0, 0, 0, 0.4);
  z-index: 998;
}

.tools-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ffffff;
  border-radius: 32rpx;
  z-index: 999;
  transform: translateY(100%);
  transition: transform 0.25s ease;
  padding-bottom: env(safe-area-inset-bottom);

  &.tools-panel-show {
    transform: translateY(0);
  }

  .tools-panel-inner {
    display: flex;
    justify-content: space-around;
    padding: 40rpx 30rpx 30rpx;

    .tool-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .tool-item-icon {
        width: 112rpx;
        height: 112rpx;
        border-radius: 32rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 56rpx;
        background-color: #ffffff;
        box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
      }

      .tool-item-photo {
        background-color: #e8f4ff;
      }

      .tool-item-audio {
        background-color: #eafaf0;
      }

      .tool-item-video {
        background-color: #ffecec;
      }

      .tool-item-label {
        margin-top: 14rpx;
        font-size: 26rpx;
        color: #555;
      }
    }
  }
}
</style>
