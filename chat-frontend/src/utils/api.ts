/**
 * API 接口封装
 */
import http from './request'

// ==================== 认证 ====================

/** 登录 */
export const login = (data: { phone: string; password: string }) =>
  http.post<any>('/api/auth/login', data)

/** 获取验证码 */
export const getCaptcha = () => http.get<any>('/api/auth/captcha')

/** 注册 */
export const register = (data: {
  username: string
  password: string
  nickname: string
  phone: string
  captchaKey: string
  captchaCode: number
}) => http.post<any>('/api/auth/register', data)

// ==================== 用户 ====================

/** 获取当前用户信息 */
export const getCurrentUser = () => http.get<any>('/api/user/me')

/** 修改个人信息 */
export const updateProfile = (data: any) => http.put<any>('/api/user/profile', data)

/** 搜索用户 */
export const searchUsers = (keyword: string) =>
  http.get<any>('/api/user/search', { keyword })

/** 获取用户信息 */
export const getUserById = (id: number) => http.get<any>(`/api/user/${id}`)

// ==================== 好友 ====================

/** 发送好友请求 */
export const sendFriendRequest = (receiverId: number, message?: string) =>
  http.post<any>('/api/friend/request', { receiverId, message })

/** 处理好友请求 */
export const handleFriendRequest = (requestId: number, status: number) =>
  http.put<any>(`/api/friend/request/${requestId}`, { status })

/** 获取待处理的好友请求 */
export const getFriendRequests = () => http.get<any>('/api/friend/requests')

/** 获取好友列表 */
export const getFriendList = () => http.get<any>('/api/friend/list')

/** 删除好友 */
export const deleteFriend = (friendId: number) =>
  http.delete<any>(`/api/friend/${friendId}`)

// ==================== 消息 ====================

/** 获取聊天记录 */
export const getChatHistory = (friendId: number) =>
  http.get<any>(`/api/message/history/${friendId}`)

/** 获取会话列表 */
export const getConversations = () => http.get<any>('/api/message/conversations')

/** 标记已读 */
export const markAsRead = (friendId: number) =>
  http.put<any>(`/api/message/read/${friendId}`)

/** 获取未读消息数 */
export const getUnreadCount = (friendId?: number) =>
  http.get<any>('/api/message/unread', { friendId })

/** 发送消息（HTTP） */
export const sendApiMessage = (data: { receiverId: number; content: string; messageType?: number }) =>
  http.post<any>('/api/message/send', data)

// ==================== 文件上传 ====================

/** 上传图片 */
export const uploadImage = (filePath: string) =>
  http.upload<any>('/api/upload/image', filePath)

// ==================== 朋友圈 ====================

/** 发布动态 */
export const createMoment = (data: { content?: string; images?: string; location?: string }) =>
  http.post<any>('/api/moment/create', data)

/** 获取朋友圈列表 */
export const getMomentList = () => http.get<any>('/api/moment/list')

/** 点赞/取消点赞 */
export const toggleMomentLike = (momentId: number) =>
  http.post<any>(`/api/moment/${momentId}/like`)

/** 获取点赞列表 */
export const getMomentLikes = (momentId: number) =>
  http.get<any>(`/api/moment/${momentId}/likes`)

/** 发表评论 */
export const addMomentComment = (momentId: number, data: { content: string; replyTo?: number }) =>
  http.post<any>(`/api/moment/${momentId}/comment`, data)

/** 获取评论列表 */
export const getMomentComments = (momentId: number) =>
  http.get<any>(`/api/moment/${momentId}/comments`)

/** 删除评论 */
export const deleteMomentComment = (commentId: number) =>
  http.delete<any>(`/api/moment/comment/${commentId}`)

/** 删除动态 */
export const deleteMoment = (momentId: number) =>
  http.delete<any>(`/api/moment/${momentId}`)

// ==================== AI 聊天 ====================

/** 向 AI 发送消息 */
export const askAI = (messages: Array<{ role: string; content: string }>) =>
  http.post<any>('/api/ai/chat', { messages })

// ==================== 群聊 ====================

/** 创建群聊 */
export const createGroup = (data: { name: string; memberIds: number[] }) =>
  http.post<any>('/api/group/create', data)

/** 获取我的群聊列表 */
export const getGroupList = () => http.get<any>('/api/group/list')

/** 获取群详情 */
export const getGroupDetail = (groupId: number) =>
  http.get<any>(`/api/group/${groupId}`)

/** 获取群聊历史消息 */
export const getGroupChatHistory = (groupId: number) =>
  http.get<any>(`/api/group/${groupId}/history`)

/** 发送群消息（HTTP） */
export const sendGroupMessage = (groupId: number, data: { content: string; messageType?: number }) =>
  http.post<any>(`/api/group/${groupId}/send`, data)

/** 邀请成员入群 */
export const addGroupMembers = (groupId: number, memberIds: number[]) =>
  http.post<any>(`/api/group/${groupId}/members`, { memberIds })

/** 退出群聊 */
export const leaveGroup = (groupId: number) =>
  http.post<any>(`/api/group/${groupId}/leave`)

/** 解散群聊 */
export const dismissGroup = (groupId: number) =>
  http.delete<any>(`/api/group/${groupId}`)

/** 修改群名称 */
export const updateGroupName = (groupId: number, name: string) =>
  http.put<any>(`/api/group/${groupId}/name`, { name })

/** 修改群公告 */
export const updateGroupAnnouncement = (groupId: number, announcement: string) =>
  http.put<any>(`/api/group/${groupId}/announcement`, { announcement })
