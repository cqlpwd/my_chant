/**
 * 群聊状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getGroupList,
  getGroupDetail,
  getGroupChatHistory,
  sendGroupMessage as sendGroupMsgApi,
  createGroup,
  leaveGroup,
  dismissGroup,
  addGroupMembers
} from '@/utils/api'
import wsManager from '@/utils/websocket'

export interface GroupInfo {
  id: number
  name: string
  avatar: string
  ownerId: number
  memberCount: number
  memberAvatars?: string[]
  lastMessage?: string
  lastMessageTime?: string
}

export interface GroupMember {
  id: number
  userId: number
  groupId: number
  role: number
  nicknameInGroup?: string
  nickname?: string
  avatar?: string
  joinedAt: string
}

export interface GroupMessage {
  id: number
  groupId: number
  senderId: number
  senderNickname: string
  senderAvatar: string
  content: string
  messageType: number
  createdAt: string
}

export const useGroupStore = defineStore('group', () => {
  const groups = ref<GroupInfo[]>([])
  const currentGroupId = ref<number>(0)
  const currentMessages = ref<GroupMessage[]>([])
  const currentMembers = ref<GroupMember[]>([])
  const currentGroupInfo = ref<GroupInfo | null>(null)
  /** 群聊未读数 { groupId: count } */
  const groupUnreadCounts = ref<Record<number, number>>({})

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

  /** 加载群聊列表 */
  async function loadGroups() {
    try {
      const res = await getGroupList()
      if (res.code === 200 && res.data) {
        groups.value = res.data
      }
    } catch { /* 静默 */ }
  }

  /** 加载群聊详情（含成员） */
  async function loadGroupDetail(groupId: number) {
    try {
      const res = await getGroupDetail(groupId)
      if (res.code === 200 && res.data) {
        currentGroupInfo.value = res.data
        currentMembers.value = res.data.members || []
      }
    } catch (e: any) {
      uni.showToast({ title: e.message || '加载失败', icon: 'none' })
    }
  }

  /** 加载群聊历史消息 */
  async function loadGroupHistory(groupId: number) {
    currentGroupId.value = groupId
    try {
      const res = await getGroupChatHistory(groupId)
      if (res.code === 200 && res.data) {
        currentMessages.value = res.data
      }
    } catch {
      uni.showToast({ title: '加载消息失败', icon: 'none' })
    }
  }

  /** 发送群消息 */
  async function sendMessage(groupId: number, content: string, messageType = 0) {
    const userId = getUserId()

    // 乐观更新
    const tempId = Date.now() * 1000 + Math.floor(Math.random() * 1000)
    const tempMsg: GroupMessage = {
      id: tempId,
      groupId,
      senderId: userId,
      senderNickname: '',
      senderAvatar: '',
      content,
      messageType,
      createdAt: formatTime(new Date().toISOString())
    }
    currentMessages.value.push(tempMsg)

    // WebSocket 发送
    wsManager.sendGroupMessage(groupId, content, messageType)

    // HTTP 保底
    try {
      const res = await sendGroupMsgApi(groupId, { content, messageType })
      if (res.code === 200 && res.data) {
        const serverMsg = res.data as GroupMessage
        const index = currentMessages.value.findIndex(m => m.id === tempId)
        if (index > -1) {
          currentMessages.value.splice(index, 1, serverMsg)
        }
      } else {
        uni.showToast({ title: res.message || '发送失败', icon: 'none' })
        const index = currentMessages.value.findIndex(m => m.id === tempId)
        if (index > -1) currentMessages.value.splice(index, 1)
      }
    } catch (e: any) {
      uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      const index = currentMessages.value.findIndex(m => m.id === tempId)
      if (index > -1) currentMessages.value.splice(index, 1)
    }
  }

  /** 处理 WebSocket 群消息 */
  function handleGroupWsMessage(msg: any) {
    const userId = getUserId()
    switch (msg.type) {
      case 'GROUP_CHAT': {
        const groupId = msg.groupId
        const isCurrentGroup = groupId === currentGroupId.value
        // 去重
        if (!currentMessages.value.find(m => m.id === msg.id)) {
          if (isCurrentGroup) {
            currentMessages.value.push(msg as GroupMessage)
            // 滚动到底部由页面处理
          }
        }
        // 更新群列表中的最后一条消息
        const groupIndex = groups.value.findIndex(g => g.id === groupId)
        if (groupIndex > -1) {
          const g = groups.value[groupIndex]
          g.lastMessage = msg.content
          g.lastMessageTime = msg.createdAt
          groups.value.splice(groupIndex, 1)
          groups.value.unshift(g)
        } else {
          loadGroups()
        }
        // 不在当前群聊页面时，累加未读数
        if (!isCurrentGroup && msg.senderId !== userId) {
          const currentCount = groupUnreadCounts.value[groupId] || 0
          groupUnreadCounts.value = { ...groupUnreadCounts.value, [groupId]: currentCount + 1 }
        }
        break
      }
      case 'GROUP_CREATED':
      case 'GROUP_ADDED':
        loadGroups()
        uni.showToast({ title: msg.type === 'GROUP_CREATED' ? '你被邀请加入群聊' : '你已加入群聊', icon: 'none' })
        break
    }
  }

  /** 创建群聊 */
  async function doCreateGroup(name: string, memberIds: number[]) {
    try {
      const res = await createGroup({ name, memberIds })
      if (res.code === 200) {
        uni.showToast({ title: '创建成功', icon: 'success' })
        await loadGroups()
        return res.data?.groupId || 0
      } else {
        uni.showToast({ title: res.message || '创建失败', icon: 'none' })
        return 0
      }
    } catch (e: any) {
      uni.showToast({ title: e.message || '创建失败', icon: 'none' })
      return 0
    }
  }

  /** 退出群聊 */
  async function doLeaveGroup(groupId: number) {
    try {
      const res = await leaveGroup(groupId)
      if (res.code === 200) {
        uni.showToast({ title: '已退出', icon: 'success' })
        groups.value = groups.value.filter(g => g.id !== groupId)
      } else {
        uni.showToast({ title: res.message, icon: 'none' })
      }
    } catch (e: any) {
      uni.showToast({ title: e.message, icon: 'none' })
    }
  }

  /** 解散群聊 */
  async function doDismissGroup(groupId: number) {
    try {
      const res = await dismissGroup(groupId)
      if (res.code === 200) {
        uni.showToast({ title: '已解散', icon: 'success' })
        groups.value = groups.value.filter(g => g.id !== groupId)
        currentMessages.value = []
        currentGroupId.value = 0
      } else {
        uni.showToast({ title: res.message, icon: 'none' })
      }
    } catch (e: any) {
      uni.showToast({ title: e.message, icon: 'none' })
    }
  }

  /** 邀请成员 */
  async function doAddMembers(groupId: number, memberIds: number[]) {
    try {
      const res = await addGroupMembers(groupId, memberIds)
      if (res.code === 200) {
        uni.showToast({ title: '邀请成功', icon: 'success' })
        await loadGroupDetail(groupId)
      } else {
        uni.showToast({ title: res.message, icon: 'none' })
      }
    } catch (e: any) {
      uni.showToast({ title: e.message, icon: 'none' })
    }
  }

  /** 清除群聊未读数 */
  function clearGroupUnread(groupId: number) {
    groupUnreadCounts.value = { ...groupUnreadCounts.value, [groupId]: 0 }
  }

  /** 获取群聊未读数 */
  function getGroupUnreadCount(groupId: number): number {
    return groupUnreadCounts.value[groupId] || 0
  }

  return {
    groups,
    currentGroupId,
    currentMessages,
    currentMembers,
    currentGroupInfo,
    groupUnreadCounts,
    loadGroups,
    loadGroupDetail,
    loadGroupHistory,
    sendMessage,
    handleGroupWsMessage,
    clearGroupUnread,
    getGroupUnreadCount,
    doCreateGroup,
    doLeaveGroup,
    doDismissGroup,
    doAddMembers
  }
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    if (d.toDateString() === new Date().toDateString()) {
      return `${hour}:${min}`
    }
    return `${month}-${day} ${hour}:${min}`
  } catch {
    return dateStr
  }
}
