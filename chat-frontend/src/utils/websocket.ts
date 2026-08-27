/**
 * WebSocket（STOMP）连接管理
 * 使用 SockJS + STOMP 子协议
 */

// H5 开发环境使用相对路径，走 Vite 代理；其他平台使用实际地址
const WS_BASE_URL = 'http://10.116.22.160:8080'

class WebSocketManager {
  private stompClient: any = null
  private subscriptions: Map<string, any> = new Map()
  private messageHandlers: Array<(msg: any) => void> = []
  private isConnected = false
  private reconnectTimer: number | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private userId: string = ''

  /** 动态加载 STOMP/SockJS 依赖 */
  private async loadDeps() {
    const [SockJS, StompModule] = await Promise.all([
      import('sockjs-client'),
      import('@stomp/stompjs')
    ])
    return {
      SockJS: (SockJS as any).default || SockJS,
      Stomp: (StompModule as any).Stomp
    }
  }

  /**
   * 连接 WebSocket
   */
  async connect(token: string, userId: string): Promise<void> {
    this.userId = userId

    if (this.isConnected && this.stompClient) return

    try {
      const { SockJS, Stomp } = await this.loadDeps()
      const socket = new SockJS(`${WS_BASE_URL}/ws`)

      return new Promise((resolve, reject) => {
        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = () => { }

        this.stompClient.connect(
          { Authorization: `Bearer ${token}` },
          () => {
            this.isConnected = true
            this.reconnectAttempts = 0
            console.log('WebSocket 连接成功')
            this.subscribeUserMessages()
            resolve()
          },
          (error: any) => {
            this.isConnected = false
            console.error('WebSocket 连接失败:', error)
            reject(error)
            this.scheduleReconnect()
          }
        )
      })
    } catch (error) {
      console.error('WebSocket 初始化失败:', error)
      this.scheduleReconnect()
      throw error
    }
  }

  /**
   * 订阅个人的消息队列
   */
  private subscribeUserMessages() {
    if (!this.stompClient || !this.userId) return

    console.log('[WS] 订阅消息队列, userId=', this.userId)

    const chatSub = this.stompClient.subscribe(
      `/user/queue/chat`,
      (message: any) => {
        const body = JSON.parse(message.body)
        console.log('[WS] 收到 CHAT 消息:', body)
        this.messageHandlers.forEach(handler => handler(body))
      }
    )
    this.subscriptions.set('chat', chatSub)

    const notifySub = this.stompClient.subscribe(
      `/user/queue/notification`,
      (message: any) => {
        const body = JSON.parse(message.body)
        console.log('[WS] 收到通知:', body)
        this.messageHandlers.forEach(handler => handler(body))
      }
    )
    this.subscriptions.set('notification', notifySub)

    const callSub = this.stompClient.subscribe(
      `/user/queue/call`,
      (message: any) => {
        const body = JSON.parse(message.body)
        console.log('[WS] 收到通话信令:', body)
        this.messageHandlers.forEach(handler => handler(body))
      }
    )
    this.subscriptions.set('call', callSub)

    const statusSub = this.stompClient.subscribe(
      `/user/queue/friend-status`,
      (message: any) => {
        const body = JSON.parse(message.body)
        console.log('[WS] 收到好友状态:', body)
        this.messageHandlers.forEach(handler => handler({
          ...body,
          type: 'FRIEND_STATUS'
        }))
      }
    )
    this.subscriptions.set('friend-status', statusSub)
  }

  sendMessage(receiverId: number, content: string, messageType = 0) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/chat.send',
      JSON.stringify({ receiverId, content, messageType }),
      {}
    )
  }

  sendReadReceipt(friendId: number) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/chat.read',
      JSON.stringify({ friendId }),
      {}
    )
  }

  sendTyping(receiverId: number, typing: boolean) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/chat.typing',
      JSON.stringify({ receiverId, typing }),
      {}
    )
  }

  sendFriendRequestNotify(receiverId: number) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/friend.request',
      JSON.stringify({ receiverId }),
      {}
    )
  }

  /** 发送视频通话信令（WebRTC） */
  sendCallSignal(receiverId: number, type: string, data: Record<string, any> = {}) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/call.signal',
      JSON.stringify({ receiverId, type, ...data }),
      {}
    )
  }

  /** 发送群聊消息 */
  sendGroupMessage(groupId: number, content: string, messageType = 0) {
    if (!this.stompClient || !this.isConnected) return
    this.stompClient.send(
      '/app/group.send',
      JSON.stringify({ groupId, content, messageType }),
      {}
    )
  }

  onMessage(handler: (msg: any) => void) {
    this.messageHandlers.push(handler)
    return () => {
      const index = this.messageHandlers.indexOf(handler)
      if (index > -1) this.messageHandlers.splice(index, 1)
    }
  }

  private unsubscribeAll() {
    this.subscriptions.forEach(sub => {
      try { sub.unsubscribe() } catch (e) { }
    })
    this.subscriptions.clear()
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('WebSocket 重连次数已达上限，停止重连')
      return
    }
    this.reconnectAttempts++
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000) // 指数退避，最大 30s
    console.log(`WebSocket 将在 ${delay / 1000}s 后第 ${this.reconnectAttempts} 次重连...`)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      const stored = localStorage.getItem('userInfo')
      if (stored) {
        try {
          const userInfo = JSON.parse(stored)
          if (userInfo.token) {
            this.connect(userInfo.token, String(userInfo.user?.id || '')).catch(() => { })
          }
        } catch { }
      }
    }, delay)
  }

  disconnect() {
    this.reconnectAttempts = this.maxReconnectAttempts // 阻止自动重连
    this.unsubscribeAll()
    if (this.stompClient) {
      try { this.stompClient.disconnect() } catch (e) { }
      this.stompClient = null
    }
    this.isConnected = false
    this.messageHandlers = []
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  get connected(): boolean {
    return this.isConnected
  }

  get currentUserId(): string {
    return this.userId
  }
}

export const wsManager = new WebSocketManager()
export default wsManager
