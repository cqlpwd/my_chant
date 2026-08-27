import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 通话阶段 */
export type CallPhase = 'idle' | 'outgoing' | 'incoming' | 'active' | 'ended'

/**
 * 视频通话状态管理（H5 WebRTC）
 * - outgoing: 我已发起呼叫，等待对方接听
 * - incoming: 对方呼叫我，等待我接听/拒绝
 * - active:   通话中
 * - ended:    通话已结束（短暂显示后自动回到 idle）
 */
export const useCallStore = defineStore('call', () => {
  const phase = ref<CallPhase>('idle')
  const peerId = ref<number>(0)
  const peerName = ref('')
  const peerAvatar = ref('')
  const isVideo = ref(true)
  /** 结束原因，用于结束页展示 */
  const endReason = ref('通话已结束')

  function reset() {
    phase.value = 'idle'
    peerId.value = 0
    peerName.value = ''
    peerAvatar.value = ''
    isVideo.value = true
    endReason.value = '通话已结束'
  }

  return { phase, peerId, peerName, peerAvatar, isVideo, endReason, reset }
})
