/**
 * 聊天状态管理（客户端持久化优先，服务端作补充）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getConversations,
  getChatHistory,
  markAsRead,
  sendApiMessage
} from '@/utils/api'
import {
  saveMessage,
  saveMessages,
  getMessages,
  getConversations as getLocalConversations,
  markAsReadLocal,
  getTotalUnreadCount,
  getUnreadCount,
  deleteConversation as deleteLocalConversation
} from '@/services/messageDb'
import wsManager from '@/utils/websocket'
import { useGroupStore } from './group'

export interface Conversation {
  friendId: number
  groupId?: number
  type?: 'private' | 'group'
  friendNickname: string
  friendAvatar: string
  friendStatus: number
  lastMessage: string
  lastMessageType: number
  lastMessageSenderId: number
  lastMessageTime: string
  unreadCount: number
}

export interface ChatMessage {
  id: number
  senderId: number
  senderNickname: string
  senderAvatar: string
  receiverId: number
  content: string
  messageType: number
  status: number
  createdAt: string
}

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentMessages = ref<ChatMessage[]>([])
  const currentFriendId = ref<number>(0)
  const typingUserId = ref<number>(0)

  const groupStore = useGroupStore()

  const unreadTotal = computed(() => {
    const privateUnread = conversations.value.reduce((sum, c) => sum + c.unreadCount, 0)
    const groupUnread = Object.values(groupStore.groupUnreadCounts.value).reduce((sum, c) => sum + (c || 0), 0)
    return privateUnread + groupUnread
  })

  /** 合并私聊和群聊的会话列表（用于消息列表页展示） */
  const mergedConversations = computed(() => {
    // 私聊会话
    const privateConvs = conversations.value.map(c => ({ ...c, type: 'private' as const }))
    // 群聊会话
    const groupConvs = groupStore.groups.map(g => ({
      friendId: g.id,
      groupId: g.id,
      type: 'group' as const,
      friendNickname: g.name,
      friendAvatar: g.avatar || '',
      friendStatus: 0,
      lastMessage: g.lastMessage || '',
      lastMessageType: 0,
      lastMessageSenderId: 0,
      lastMessageTime: g.lastMessageTime || '',
      unreadCount: groupStore.getGroupUnreadCount(g.id)
    } as Conversation))
    const all = [...privateConvs, ...groupConvs]
    all.sort((a, b) => (b.lastMessageTime || '').localeCompare(a.lastMessageTime || ''))
    return all
  })

  /** 获取当前用户 ID */
  function getUserId(): number {
    try {
      const stored = uni.getStorageSync('userInfo')
      if (stored) {
        return JSON.parse(stored).user?.id || 0
      }
    } catch { }
    return 0
  }

  /**
   * 加载会话列表（优先本地 DB，服务端补充）
   */
  async function loadConversations() {
    const userId = getUserId()
    if (!userId) {
      // 没有用户信息，走服务端
      const res = await getConversations()
      if (res.code === 200 && res.data) {
        conversations.value = res.data
      }
      groupStore.loadGroups().catch(() => { })
      return
    }

    // 1. 先从本地 DB 获取最近消息构建会话列表
    const localConvs = await getLocalConversations(userId)
    const localConvMap = new Map<number, any>()
    const friendIds = new Set<number>()

    for (const msg of localConvs) {
      friendIds.add(msg.friendId)
      localConvMap.set(msg.friendId, {
        friendId: msg.friendId,
        lastMessage: msg.content,
        lastMessageType: msg.messageType,
        lastMessageSenderId: msg.senderId,
        lastMessageTime: formatTime(msg.createdAt),
        friendNickname: msg.senderId === userId ? '' : msg.senderNickname,
        friendAvatar: msg.senderId === userId ? '' : msg.senderAvatar
      })
    }

    // 2. 从服务端获取好友信息（昵称、头像、状态等补充字段）
    try {
      const res = await getConversations()
      if (res.code === 200 && res.data) {
        const serverConvs = res.data as any[]
        for (const sc of serverConvs) {
          const local = localConvMap.get(sc.friendId)
          if (local) {
            // 服务端数据补充好友信息，本地数据保留最新消息
            local.friendNickname = sc.friendNickname || local.friendNickname
            local.friendAvatar = sc.friendAvatar || local.friendAvatar
            local.friendStatus = sc.friendStatus ?? 0
            if (!local.lastMessageTime || (sc.lastMessageTime > local.lastMessageTime)) {
              local.lastMessage = sc.lastMessage
              local.lastMessageType = sc.lastMessageType
              local.lastMessageSenderId = sc.lastMessageSenderId
              local.lastMessageTime = sc.lastMessageTime
            }
            localConvMap.set(sc.friendId, local)
          } else {
            // 服务端有但本地没有的会话，直接用服务端的
            localConvMap.set(sc.friendId, {
              friendId: sc.friendId,
              friendNickname: sc.friendNickname,
              friendAvatar: sc.friendAvatar,
              friendStatus: sc.friendStatus ?? 0,
              lastMessage: sc.lastMessage,
              lastMessageType: sc.lastMessageType,
              lastMessageSenderId: sc.lastMessageSenderId,
              lastMessageTime: sc.lastMessageTime
            })
          }
        }
      }
    } catch { /* 服务端不可用时，纯本地也能用 */ }

    // 3. 补充未读数
    const convList: Conversation[] = []
    for (const [, conv] of localConvMap) {
      const unread = await getUnreadCount(userId, conv.friendId)
      convList.push({ ...conv, unreadCount: unread })
    }

    // 按最后消息时间倒序
    convList.sort((a, b) => (b.lastMessageTime || '').localeCompare(a.lastMessageTime || ''))
    conversations.value = convList

    // 同时加载群聊列表，合并到消息列表展示
    groupStore.loadGroups().catch(() => { })
  }

  /**
   * 加载聊天记录（优先本地 DB，服务端补充最近消息）
   */
  async function loadChatHistory(friendId: number) {
    currentFriendId.value = friendId
    const userId = getUserId()

    if (userId) {
      // 1. 先从本地 DB 加载
      const localMsgs = await getMessages(userId, friendId)
      if (localMsgs.length > 0) {
        currentMessages.value = localMsgs as ChatMessage[]
      }

      // 2. 尝试从服务端获取最近几天的新消息（本地可能丢失的部分）
      try {
        const res = await getChatHistory(friendId)
        if (res.code === 200 && res.data) {
          const serverMsgs = res.data as ChatMessage[]
          // 合并去重，以服务端为准更新同一 ID 的消息
          const msgMap = new Map<number, ChatMessage>()
          for (const m of currentMessages.value) msgMap.set(m.id, m)
          for (const m of serverMsgs) {
            msgMap.set(m.id, m)
            // 同时保存到本地 DB（带上 userId 字段）
            saveMessage({
              ...m,
              userId,
              friendId: m.senderId === userId ? m.receiverId : m.senderId
            }).catch(() => { })
          }
          // 排序后显示
          currentMessages.value = Array.from(msgMap.values())
            .sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
        }
      } catch {
        // 服务端不可用，纯本地消息也够用
        if (currentMessages.value.length === 0) {
          uni.showToast({ title: '暂无聊天记录', icon: 'none' })
        }
      }
    } else {
      // 无 userId 时走纯服务端
      const res = await getChatHistory(friendId)
      if (res.code === 200 && res.data) {
        currentMessages.value = res.data
      }
    }

    return { code: 200 }
  }

  /**
   * 发送消息
   */
  async function sendMessage(receiverId: number, content: string, messageType = 0) {
    const userId = getUserId()

    // 乐观更新：先保存到本地 DB
    const tempId = Date.now() * 1000 + Math.floor(Math.random() * 1000)
    const tempMsg: ChatMessage = {
      id: tempId,
      senderId: userId,
      senderNickname: '',
      senderAvatar: '',
      receiverId,
      content,
      messageType,
      status: 0,
      createdAt: formatTime(new Date().toISOString())
    }

    // 立即显示在界面上
    currentMessages.value.push(tempMsg)

    // 保存到本地 DB
    saveMessage({
      ...tempMsg,
      userId,
      friendId: receiverId
    }).catch(() => { })

    try {
      const res = await sendApiMessage({ receiverId, content, messageType })
      if (res.code === 200 && res.data) {
        const serverMsg = res.data as ChatMessage
        // 用服务端返回的真实消息替换本地临时消息
        const index = currentMessages.value.findIndex(m => m.id === tempId)
        if (index > -1) {
          currentMessages.value.splice(index, 1, serverMsg)
        }
        // 更新本地 DB
        saveMessage({
          ...serverMsg,
          userId,
          friendId: receiverId
        }).catch(() => { })

        // 更新会话列表
        updateConversationInList(receiverId, serverMsg.content, serverMsg.messageType, serverMsg.senderId, serverMsg.createdAt)
      } else {
        uni.showToast({ title: res.message || '发送失败', icon: 'none' })
        // 移除失败的临时消息
        const index = currentMessages.value.findIndex(m => m.id === tempId)
        if (index > -1) currentMessages.value.splice(index, 1)
      }
    } catch (e: any) {
      uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      const index = currentMessages.value.findIndex(m => m.id === tempId)
      if (index > -1) currentMessages.value.splice(index, 1)
    }
  }

  /**
   * 标记已读
   */
  async function readMessages(friendId: number) {
    const userId = getUserId()

    // 本地标记已读
    if (userId) {
      await markAsReadLocal(userId, friendId)
    }

    // 服务端标记已读
    try {
      await markAsRead(friendId)
    } catch { /* 静默 */ }

    // 更新会话列表中的未读数
    const conv = conversations.value.find(c => c.friendId === friendId)
    if (conv) {
      conv.unreadCount = 0
    }
  }

  /**
   * 处理 WebSocket 收到的消息
   */
  function handleWsMessage(msg: any) {
    const userId = getUserId()

    switch (msg.type) {
      case 'CHAT': {
        // 先保存到本地 DB
        const friendId = msg.senderId === userId ? msg.receiverId : msg.senderId
        saveMessage({
          ...msg,
          userId,
          friendId
        }).catch(() => { })

        const isCurrentChat = friendId === currentFriendId.value

        // 去重后添加到当前消息列表
        if (!currentMessages.value.find(m => m.id === msg.id)) {
          if (isCurrentChat) {
            currentMessages.value.push(msg as ChatMessage)
          }
        }

        // 更新会话列表
        const convIndex = conversations.value.findIndex(c => c.friendId === friendId)
        if (convIndex > -1) {
          const conv = conversations.value[convIndex]
          conv.lastMessage = msg.content
          conv.lastMessageType = msg.messageType
          conv.lastMessageSenderId = msg.senderId
          conv.lastMessageTime = msg.createdAt
          if (msg.senderId !== userId) {
            conv.unreadCount++
          }
          conversations.value.splice(convIndex, 1)
          conversations.value.unshift(conv)
        } else {
          loadConversations()
        }

        break
      }

      case 'READ_RECEIPT':
        break

      case 'TYPING':
        typingUserId.value = msg.typing ? msg.userId : 0
        break

      case 'FRIEND_REQUEST':
        uni.showToast({ title: '收到新的好友请求', icon: 'none' })
        break

      case 'FRIEND_STATUS':
        const conv2 = conversations.value.find(c => c.friendId === msg.userId)
        if (conv2) {
          conv2.friendStatus = msg.status
        }
        break

      case 'ERROR':
        uni.showToast({ title: msg.content || '发送失败', icon: 'none', duration: 2000 })
        break

      case 'GROUP_CHAT': {
        const gs = useGroupStore()
        gs.handleGroupWsMessage(msg)
        break
      }
      case 'GROUP_CREATED':
      case 'GROUP_ADDED':
        // 转发给群聊 store 处理
        useGroupStore().handleGroupWsMessage(msg)
        break
    }
  }

  /** 删除本地会话 */
  async function removeConversation(friendId: number) {
    const userId = getUserId()
    if (userId) {
      await deleteLocalConversation(userId, friendId)
    }
    conversations.value = conversations.value.filter(c => c.friendId !== friendId)
  }

  /** 更新会话列表中的某条记录 */
  function updateConversationInList(friendId: number, content: string, messageType: number, senderId: number, time: string) {
    const convIndex = conversations.value.findIndex(c => c.friendId === friendId)
    if (convIndex > -1) {
      const conv = conversations.value[convIndex]
      conv.lastMessage = content
      conv.lastMessageType = messageType
      conv.lastMessageSenderId = senderId
      conv.lastMessageTime = time
      conversations.value.splice(convIndex, 1)
      conversations.value.unshift(conv)
    } else {
      loadConversations()
    }
  }

  return {
    conversations,
    mergedConversations,
    currentMessages,
    currentFriendId,
    typingUserId,

    unreadTotal,
    loadConversations,
    loadChatHistory,
    sendMessage,
    readMessages,
    handleWsMessage,
    removeConversation
  }
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    const now = new Date()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')

    if (d.toDateString() === now.toDateString()) {
      return `${hour}:${min}`
    }
    return `${month}-${day} ${hour}:${min}`
  } catch {
    return dateStr
  }
}
