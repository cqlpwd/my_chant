<template>
  <view class="moments-page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-left" @click="goBack">
        <text class="nav-back">←</text>
      </view>
      <text class="nav-title">朋友圈</text>
      <view class="nav-right" @click="goCreate">
        <text class="nav-camera">📷</text>
      </view>
    </view>

    <!-- 朋友圈列表 -->
    <scroll-view
      class="moment-list"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      refresher-default-style="black"
      refresher-background="#f5f5f5"
      :refresher-threshold="45"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view v-if="moments.length === 0 && !loading" class="empty-state">
        <text class="empty-text">还没有动态，快去发布吧～</text>
      </view>

      <view v-for="moment in moments" :key="moment.id" class="moment-item">
        <!-- 用户头像 -->
        <image class="user-avatar" :src="moment.user?.avatar || '/static/default-avatar.png'" mode="aspectFill" />

        <view class="moment-body">
          <!-- 用户昵称 -->
          <text class="user-nickname">{{ moment.user?.nickname || '未知用户' }}</text>

          <!-- 文字内容 -->
          <view v-if="moment.content" class="moment-content">{{ moment.content }}</view>

          <!-- 图片 -->
          <view v-if="moment.images" class="moment-images" :class="'grid-' + getImageCount(moment.images)">
            <image
              v-for="(img, idx) in parseImages(moment.images)"
              :key="idx"
              :src="img"
              class="moment-image"
              mode="aspectFill"
              @click="previewImage(moment.images, idx)"
            />
          </view>

          <!-- 位置 -->
          <view v-if="moment.location" class="moment-location">📍 {{ moment.location }}</view>

          <!-- 时间和操作 -->
          <view class="moment-footer">
            <text class="moment-time">{{ formatTime(moment.createdAt) }}</text>
            <view class="moment-actions">
              <view class="action-btn" @click="handleLike(moment)">
                <text :class="moment.liked ? 'liked' : ''">{{ moment.liked ? '❤️' : '🤍' }}</text>
                <text class="action-count" v-if="moment.likeCount > 0">{{ moment.likeCount }}</text>
              </view>
              <view class="action-btn" @click="openComment(moment)">
                <text>💬</text>
                <text class="action-count" v-if="moment.commentCount > 0">{{ moment.commentCount }}</text>
              </view>
              <view v-if="isMine(moment)" class="action-btn" @click="handleDelete(moment)">
                <text>🗑</text>
              </view>
            </view>
          </view>

          <!-- 点赞信息 -->
          <view v-if="moment.likeCount > 0" class="like-bar">
            <text class="like-icon">❤️</text>
            <text class="like-users" @click="showLikes(moment)">
              {{ moment.likeUserNames || moment.likeCount + '人' }}
            </text>
          </view>

          <!-- 评论列表 -->
          <view v-if="moment.comments && moment.comments.length > 0" class="comment-area">
            <view v-for="comment in moment.comments" :key="comment.id" class="comment-item">
              <text class="comment-nickname" @click="replyComment(moment, comment)">{{ comment.nickname }}</text>
              <text v-if="comment.replyToNickname" class="comment-reply"> 回复 </text>
              <text v-if="comment.replyToNickname" class="comment-nickname">{{ comment.replyToNickname }}</text>
              <text class="comment-colon">：</text>
              <text class="comment-content">{{ comment.content }}</text>
              <text v-if="comment.userId === userStore.userInfo?.id" class="comment-delete" @click="deleteComment(moment, comment)"> 删除</text>
            </view>
          </view>

          <!-- 评论输入框 -->
          <view v-if="commentMomentId === moment.id" class="comment-input-box">
            <input
              class="comment-input"
              :placeholder="replyTarget ? '回复 ' + replyTarget.nickname : '评论'"
              v-model="commentText"
              confirm-type="send"
              @confirm="submitComment(moment)"
            />
            <text class="comment-send" @click="submitComment(moment)">发送</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-text">加载中...</view>
      <view v-if="noMore && moments.length > 0" class="no-more">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getMomentList,
  toggleMomentLike,
  getMomentComments,
  addMomentComment,
  deleteMomentComment,
  deleteMoment,
  uploadImage
} from '@/utils/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

const moments = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const commentMomentId = ref<number | null>(null)
const commentText = ref('')
const replyTarget = ref<any>(null)

onMounted(() => {
  loadMoments()
})

onShow(() => {
  loadMoments()
})

async function loadMoments() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMomentList()
    if (res.data) {
      moments.value = res.data.map((m: any) => ({
        ...m,
        comments: [],
        commentsLoaded: false
      }))
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadMoments()
  refreshing.value = false
}

function loadMore() {
  // 单页加载，暂无分页
  noMore.value = true
}

function parseImages(images: string): string[] {
  try {
    return JSON.parse(images)
  } catch {
    return images.split(',').map(s => s.trim()).filter(Boolean)
  }
}

function getImageCount(images: string): number {
  return Math.min(parseImages(images).length, 9)
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  if (d.toDateString() === now.toDateString()) {
    return pad(d.getHours()) + ':' + pad(d.getMinutes())
  }
  return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

function previewImage(images: string, current: number) {
  const urls = parseImages(images)
  uni.previewImage({ urls, current })
}

function isMine(moment: any): boolean {
  return moment.user?.id === userStore.userInfo?.id
}

async function handleLike(moment: any) {
  try {
    const res = await toggleMomentLike(moment.id)
    if (res.data) {
      moment.liked = res.data.liked
      moment.likeCount = res.data.likeCount
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

async function openComment(moment: any) {
  if (commentMomentId.value === moment.id) {
    commentMomentId.value = null
    return
  }
  commentMomentId.value = moment.id
  replyTarget.value = null
  commentText.value = ''

  if (!moment.commentsLoaded) {
    try {
      const res = await getMomentComments(moment.id)
      moment.comments = res.data || []
      moment.commentsLoaded = true
    } catch (e) { /* ignore */ }
  }
}

function replyComment(moment: any, comment: any) {
  commentMomentId.value = moment.id
  replyTarget.value = comment
  commentText.value = ''
}

async function submitComment(moment: any) {
  const text = commentText.value.trim()
  if (!text) return

  try {
    const res = await addMomentComment(moment.id, {
      content: text,
      replyTo: replyTarget.value?.id || undefined
    })
    if (res.data) {
      if (!moment.comments) moment.comments = []
      moment.comments.push(res.data)
      moment.commentCount = (moment.commentCount || 0) + 1
      moment.commentsLoaded = true
    }
    commentText.value = ''
    replyTarget.value = null
  } catch (e: any) {
    uni.showToast({ title: e.message || '评论失败', icon: 'none' })
  }
}

async function deleteComment(moment: any, comment: any) {
  uni.showModal({
    title: '提示',
    content: '确定删除该评论？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteMomentComment(comment.id)
          moment.comments = moment.comments.filter((c: any) => c.id !== comment.id)
          moment.commentCount = Math.max(0, (moment.commentCount || 0) - 1)
        } catch (e: any) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleDelete(moment: any) {
  uni.showModal({
    title: '提示',
    content: '确定删除这条动态？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteMoment(moment.id)
          moments.value = moments.value.filter(m => m.id !== moment.id)
          uni.showToast({ title: '已删除', icon: 'success' })
        } catch (e: any) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}

function showLikes(moment: any) {
  // 简化：显示点赞数量
  uni.showToast({ title: moment.likeCount + '人赞过', icon: 'none' })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/moments/create' })
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
.moments-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: #ededed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 30rpx;
  height: 88rpx;
  border-bottom: 1rpx solid #d9d9d9;

  .nav-left, .nav-right {
    width: 80rpx;
    display: flex;
    align-items: center;
  }

  .nav-right {
    justify-content: flex-end;
  }

  .nav-back {
    font-size: 40rpx;
    color: #333;
  }

  .nav-camera {
    font-size: 36rpx;
  }

  .nav-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #333;
  }
}

.moment-list {
  height: calc(100vh - 88rpx - var(--status-bar-height, 44px));
}

.empty-state {
  display: flex;
  justify-content: center;
  padding-top: 300rpx;

  .empty-text {
    font-size: 28rpx;
    color: #999;
  }
}

.moment-item {
  display: flex;
  padding: 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.user-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 10rpx;
  flex-shrink: 0;
  background-color: #eee;
}

.moment-body {
  flex: 1;
  margin-left: 20rpx;
}

.user-nickname {
  font-size: 28rpx;
  font-weight: bold;
  color: #576b95;
}

.moment-content {
  font-size: 30rpx;
  color: #333;
  margin-top: 12rpx;
  line-height: 1.6;
}

.moment-images {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;

  .moment-image {
    border-radius: 8rpx;
    background-color: #eee;
    margin-right: 8rpx;
    margin-bottom: 8rpx;
  }

  &.grid-1 .moment-image {
    width: 360rpx;
    height: 360rpx;
  }

  &:not(.grid-1) .moment-image {
    width: 210rpx;
    height: 210rpx;
  }
}

.moment-location {
  font-size: 24rpx;
  color: #576b95;
  margin-top: 10rpx;
}

.moment-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;

  .moment-time {
    font-size: 22rpx;
    color: #999;
  }

  .moment-actions {
    display: flex;
    align-items: center;
    gap: 24rpx;
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 4rpx;
    font-size: 26rpx;

    text {
      font-size: 26rpx;
    }

    .liked {
      font-size: 26rpx;
    }

    .action-count {
      font-size: 22rpx;
      color: #999;
    }
  }
}

.like-bar {
  background-color: #f5f5f5;
  padding: 10rpx 16rpx;
  border-radius: 6rpx;
  margin-top: 12rpx;
  display: flex;
  align-items: center;

  .like-icon {
    font-size: 22rpx;
    margin-right: 8rpx;
  }

  .like-users {
    font-size: 24rpx;
    color: #576b95;
  }
}

.comment-area {
  background-color: #f5f5f5;
  border-radius: 6rpx;
  padding: 12rpx 16rpx;
  margin-top: 8rpx;
}

.comment-item {
  font-size: 26rpx;
  line-height: 1.6;
  padding: 4rpx 0;

  .comment-nickname {
    color: #576b95;
  }

  .comment-content {
    color: #333;
  }

  .comment-delete {
    color: #999;
    font-size: 22rpx;
    margin-left: 8rpx;
  }

  .comment-reply {
    color: #666;
  }

  .comment-colon {
    color: #999;
  }
}

.comment-input-box {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  padding: 8rpx 16rpx;
  margin-top: 12rpx;

  .comment-input {
    flex: 1;
    height: 56rpx;
    font-size: 26rpx;
    color: #333;
  }

  .comment-send {
    font-size: 26rpx;
    color: #07C160;
    font-weight: bold;
    padding-left: 16rpx;
  }
}

.loading-text,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #999;
}
</style>
