/**
 * WebRTC 通话服务（仅 H5 端可用，浏览器原生能力）
 *
 * 职责：
 * - 采集本地音视频流（getUserMedia）
 * - 管理 RTCPeerConnection（offer / answer / ICE 交换）
 * - 把本地/远端流绑定到 <video> 元素（H5 端通过 srcObject）
 *
 * 信令通道复用现有 STOMP WebSocket（见 wsManager.sendCallSignal），
 * 媒体流走 P2P 直连，不经过服务器。
 */

import wsManager from './websocket'

// 生产环境建议配置 TURN 服务器以穿透对称 NAT：
// { urls: 'turn:your-turn-server:3478', username: 'xx', credential: 'xx' }
const ICE_SERVERS: RTCConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ]
}

class RtcService {
  private pc: RTCPeerConnection | null = null
  private localStream: MediaStream | null = null
  private remoteStream: MediaStream | null = null
  /** 当前通话的对端用户 ID（用于把 ICE 候选发给对方） */
  private peerId = 0
  /** 远端流就绪时的回调（用于把流绑定到 video 元素） */
  private remoteCallbacks: Array<(stream: MediaStream) => void> = []

  /** H5 需要 HTTPS（或 localhost）才有摄像头/麦克风权限 */
  async getLocalStream(video = true): Promise<MediaStream> {
    if (this.localStream) return this.localStream
    this.localStream = await navigator.mediaDevices.getUserMedia({
      video: video ? { width: 1280, height: 720 } : false,
      audio: true
    })
    return this.localStream
  }

  /** 创建 PeerConnection 并挂接 ICE 与远端轨道回调 */
  private createPeerConnection(peerId: number) {
    this.peerId = peerId
    const pc = new RTCPeerConnection(ICE_SERVERS)

    // 本地 ICE 候选 → 通过信令发给对端
    pc.onicecandidate = (ev) => {
      if (ev.candidate && this.peerId) {
        wsManager.sendCallSignal(this.peerId, 'CALL_ICE', {
          candidate: ev.candidate.toJSON()
        })
      }
    }

    // 收到对端媒体轨道 → 生成远端流并回调
    pc.ontrack = (ev) => {
      if (ev.streams && ev.streams[0]) {
        this.remoteStream = ev.streams[0]
        this.remoteCallbacks.forEach(cb => cb(this.remoteStream!))
      }
    }

    this.pc = pc
    return pc
  }

  /**
   * 主叫：发起视频呼叫
   * 1. 采集本地流 → 2. 加入 PeerConnection → 3. 生成 offer → 4. 通过信令发给对方
   */
  async startCall(peerId: number, video = true): Promise<void> {
    const stream = await this.getLocalStream(video)
    const pc = this.createPeerConnection(peerId)
    stream.getTracks().forEach(track => pc.addTrack(track, stream))

    // 携带自己的昵称/头像，让对方来电界面能展示
    const stored = uni.getStorageSync('userInfo') || {}
    const my = stored.user || {}

    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    wsManager.sendCallSignal(peerId, 'CALL_OFFER', {
      sdp: offer,
      video: video,
      fromNickname: my.nickname || '',
      fromAvatar: my.avatar || ''
    })
  }

  /**
   * 被叫：接听
   * 1. 采集本地流 → 2. 加入 PeerConnection → 3. 应用 offer → 4. 生成 answer 发回
   */
  async answerCall(peerId: number, sdp: RTCSessionDescriptionInit, video = true): Promise<void> {
    const stream = await this.getLocalStream(video)
    const pc = this.createPeerConnection(peerId)
    stream.getTracks().forEach(track => pc.addTrack(track, stream))

    await pc.setRemoteDescription(sdp)
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    wsManager.sendCallSignal(peerId, 'CALL_ANSWER', { sdp: answer })
  }

  /** 主叫：收到对方的 answer */
  async handleAnswer(sdp: RTCSessionDescriptionInit): Promise<void> {
    if (!this.pc) return
    await this.pc.setRemoteDescription(sdp)
  }

  /** 双方：收到对端 ICE 候选 */
  async handleIce(candidate: RTCIceCandidateInit): Promise<void> {
    if (!this.pc) return
    try {
      await this.pc.addIceCandidate(candidate)
    } catch (e) {
      console.warn('[RTC] 添加 ICE 候选失败', e)
    }
  }

  /** 把本地流绑定到 video 元素（H5 用 srcObject） */
  bindLocalVideo(el: any) {
    if (el && this.localStream) {
      el.srcObject = this.localStream
      el.muted = true // 本地预览静音，避免回声
    }
  }

  /** 把远端流绑定到 video 元素 */
  bindRemoteVideo(el: any) {
    if (el && this.remoteStream) {
      el.srcObject = this.remoteStream
    }
  }

  /** 注册远端流就绪回调（组件挂载 video 后调用） */
  onRemoteStream(cb: (stream: MediaStream) => void) {
    this.remoteCallbacks.push(cb)
  }

  /** 挂断：关闭连接、释放摄像头/麦克风 */
  hangup() {
    if (this.pc) {
      try { this.pc.close() } catch (e) { /* 忽略 */ }
      this.pc = null
    }
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop())
      this.localStream = null
    }
    this.remoteStream = null
    this.remoteCallbacks = []
  }
}

export const rtcService = new RtcService()
export default rtcService
