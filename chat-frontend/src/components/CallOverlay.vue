<template>
  <view v-if="callStore.phase !== 'idle'" class="call-overlay">
    <!-- 通话中：视频→远端大屏；语音→头像占位 -->
    <video
      v-if="callStore.phase === 'active' && callStore.isVideo"
      ref="remoteVideoRef"
      class="remote-video"
      autoplay
      playsinline
      object-fit="cover"
    />
    <!-- 语音通话中 / 呼叫 / 结束阶段：头像占位 -->
    <view v-else class="avatar-area" :class="{ 'avatar-area-calling': callStore.phase === 'active' }">
      <image
        class="peer-avatar"
        :src="callStore.peerAvatar || '/static/default-avatar.svg'"
        mode="aspectFill"
      />
      <text class="peer-name">{{ callStore.peerName }}</text>
      <text class="status-text">{{ statusText }}</text>
    </view>

    <!-- 通话中：本地视频预览小窗（仅视频通话） -->
    <video
      v-if="callStore.phase === 'active' && callStore.isVideo"
      ref="localVideoRef"
      class="local-video"
      autoplay
      playsinline
      muted
      object-fit="cover"
    />

    <!-- 底部操作按钮 -->
    <view class="action-bar">
      <!-- 来电：拒绝 / 接听 -->
      <template v-if="callStore.phase === 'incoming'">
        <view class="action-btn btn-reject" @click="$emit('reject')">
          <text class="btn-icon">✕</text>
          <text class="btn-label">拒绝</text>
        </view>
        <view class="action-btn btn-accept" @click="$emit('accept')">
          <text class="btn-icon">📞</text>
          <text class="btn-label">接听</text>
        </view>
      </template>

      <!-- 去电：取消 -->
      <view v-else-if="callStore.phase === 'outgoing'" class="action-btn btn-reject" @click="$emit('hangup')">
        <text class="btn-icon">✕</text>
        <text class="btn-label">取消</text>
      </view>

      <!-- 通话中：挂断 -->
      <view v-else-if="callStore.phase === 'active'" class="action-btn btn-hangup" @click="$emit('hangup')">
        <text class="btn-icon">📵</text>
        <text class="btn-label">挂断</text>
      </view>

      <!-- 已结束：关闭（防止自动关闭失败时无法退出） -->
      <view v-else-if="callStore.phase === 'ended'" class="action-btn btn-close" @click="$emit('close')">
        <text class="btn-icon">✕</text>
        <text class="btn-label">关闭</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useCallStore } from '@/store/call'
import rtcService from '@/utils/rtc'

const callStore = useCallStore()

const remoteVideoRef = ref<any>(null)
const localVideoRef = ref<any>(null)

const statusText = computed(() => {
  switch (callStore.phase) {
    case 'outgoing': return '正在呼叫...'
    case 'incoming': return callStore.isVideo ? '邀请你视频通话' : '邀请你语音通话'
    case 'active': return '通话中'
    case 'ended': return callStore.endReason
    default: return ''
  }
})

/** 把本地/远端流绑定到 video 元素（H5 通过 srcObject，仅视频通话） */
function bindVideos() {
  if (!callStore.isVideo) return
  const remoteEl = remoteVideoRef.value?.$el || remoteVideoRef.value
  const localEl = localVideoRef.value?.$el || localVideoRef.value
  rtcService.bindRemoteVideo(remoteEl)
  rtcService.bindLocalVideo(localEl)
}

// 进入通话中阶段时绑定视频流
watch(() => callStore.phase, async (p) => {
  if (p === 'active') {
    await nextTick()
    bindVideos()
  }
})

onMounted(() => {
  // 远端流可能在任何时刻就绪（ontrack 回调），注册后自动绑定
  rtcService.onRemoteStream(() => {
    nextTick(() => {
      const remoteEl = remoteVideoRef.value?.$el || remoteVideoRef.value
      rtcService.bindRemoteVideo(remoteEl)
    })
  })
})
</script>

<style lang="scss" scoped>
.call-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background-color: #1a1a1a;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.remote-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #000;
}

.avatar-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .peer-avatar {
    width: 200rpx;
    height: 200rpx;
    border-radius: 100rpx;
    background-color: #333;
    margin-bottom: 40rpx;
  }

  .peer-name {
    font-size: 40rpx;
    color: #fff;
    font-weight: bold;
    margin-bottom: 20rpx;
  }

  .status-text {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.7);
  }

  /* 语音通话中：大头像，微信风格 */
  &.avatar-area-calling {
    .peer-avatar {
      width: 280rpx;
      height: 280rpx;
      border-radius: 140rpx;
      margin-bottom: 60rpx;
    }

    .peer-name {
      font-size: 44rpx;
    }
  }
}

.local-video {
  position: absolute;
  top: calc(var(--status-bar-height, 44px) + 20rpx);
  right: 20rpx;
  width: 200rpx;
  height: 280rpx;
  border-radius: 16rpx;
  background-color: #000;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  z-index: 1;
}

.action-bar {
  position: absolute;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 80rpx;
  z-index: 2;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;

  .btn-icon {
    width: 110rpx;
    height: 110rpx;
    line-height: 110rpx;
    text-align: center;
    font-size: 48rpx;
    color: #fff;
    border-radius: 55rpx;
    background-color: rgba(255, 255, 255, 0.15);
  }

  .btn-label {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
  }
}

.btn-reject .btn-icon {
  background-color: #e64340;
}

.btn-accept .btn-icon {
  background-color: #07C160;
}

.btn-hangup .btn-icon {
  background-color: #e64340;
}
</style>
