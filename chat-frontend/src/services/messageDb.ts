/**
 * 本地消息数据库（IndexedDB）
 * 消息持久化在客户端，服务端仅做中转，减轻数据库压力
 */

const DB_NAME = 'MychantMsgDB'
const DB_VERSION = 1
const STORE_NAME = 'messages'

interface MessageRecord {
  id: number
  userId: number        // 当前登录用户 ID（用于分区）
  friendId: number      // 对话好友 ID
  senderId: number
  senderNickname: string
  senderAvatar: string
  receiverId: number
  content: string
  messageType: number
  status: number
  createdAt: string
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' })
        store.createIndex('conversation', ['userId', 'friendId'], { unique: false })
        store.createIndex('createdAt', 'createdAt', { unique: false })
      }
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

/**
 * 保存单条消息（如果已存在则更新）
 */
export async function saveMessage(msg: MessageRecord): Promise<void> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put(msg)
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      console.error('[MsgDB] 保存消息失败', tx.error)
      resolve() // 静默失败，不影响用户体验
    }
  })
}

/**
 * 批量保存消息
 */
export async function saveMessages(msgs: MessageRecord[]): Promise<void> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    msgs.forEach(msg => store.put(msg))
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      console.error('[MsgDB] 批量保存失败', tx.error)
      resolve()
    }
  })
}

/**
 * 获取与某好友的聊天记录（按时间正序）
 */
export async function getMessages(userId: number, friendId: number): Promise<MessageRecord[]> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const index = tx.objectStore(STORE_NAME).index('conversation')
    const range = IDBKeyRange.only([userId, friendId])
    const request = index.getAll(range)

    request.onsuccess = () => {
      db.close()
      // 按 createdAt 正序排列
      const msgs = request.result || []
      msgs.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
      resolve(msgs)
    }
    request.onerror = () => {
      db.close()
      console.error('[MsgDB] 查询消息失败', request.error)
      resolve([])
    }
  })
}

/**
 * 获取所有会话的最后一条消息（用于会话列表）
 * 返回每个 friendId 对应的最新一条消息
 */
export async function getConversations(userId: number): Promise<MessageRecord[]> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const request = tx.objectStore(STORE_NAME).getAll()

    request.onsuccess = () => {
      db.close()
      const all = (request.result || []) as MessageRecord[]
      // 过滤出当前用户的会话
      const myMsgs = all.filter(m => m.userId === userId)
      // 按 friendId 分组，取每组最新的一条
      const groupMap = new Map<number, MessageRecord>()
      for (const m of myMsgs) {
        const existing = groupMap.get(m.friendId)
        if (!existing || (m.createdAt || '') > (existing.createdAt || '')) {
          groupMap.set(m.friendId, m)
        }
      }
      // 按时间倒序排列
      const result = Array.from(groupMap.values())
      result.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
      resolve(result)
    }
    request.onerror = () => {
      db.close()
      resolve([])
    }
  })
}

/**
 * 获取与某好友的未读消息数
 */
export async function getUnreadCount(userId: number, friendId: number): Promise<number> {
  const msgs = await getMessages(userId, friendId)
  return msgs.filter(m => m.senderId === friendId && m.status === 0).length
}

/**
 * 获取总未读消息数
 */
export async function getTotalUnreadCount(userId: number): Promise<number> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const request = tx.objectStore(STORE_NAME).getAll()
    request.onsuccess = () => {
      db.close()
      const all = (request.result || []) as MessageRecord[]
      const count = all.filter(m => m.userId === userId && m.senderId !== userId && m.status === 0).length
      resolve(count)
    }
    request.onerror = () => {
      db.close()
      resolve(0)
    }
  })
}

/**
 * 标记与某好友的所有消息为已读
 */
export async function markAsReadLocal(userId: number, friendId: number): Promise<void> {
  const msgs = await getMessages(userId, friendId)
  const updated = msgs.filter(m => m.senderId === friendId && m.status === 0)
  if (updated.length === 0) return

  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    for (const m of updated) {
      m.status = 1
      store.put(m)
    }
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      resolve()
    }
  })
}

/**
 * 删除与某好友的所有本地消息（用于删除会话）
 */
export async function deleteConversation(userId: number, friendId: number): Promise<void> {
  const msgs = await getMessages(userId, friendId)
  if (msgs.length === 0) return

  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    for (const m of msgs) {
      store.delete(m.id)
    }
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      resolve()
    }
  })
}

/**
 * 清理所有数据（用于重置）
 */
export async function clearAll(): Promise<void> {
  const db = await openDB()
  return new Promise((resolve) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).clear()
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      resolve()
    }
  })
}
