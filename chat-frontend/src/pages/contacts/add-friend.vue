<template>
  <view class="add-friend-page">
    <!-- 搜索框 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="搜索用户名/昵称/手机号"
          placeholder-class="search-placeholder"
          @confirm="doSearch"
        />
        <text v-if="keyword" class="clear-btn" @click="clearSearch">✕</text>
      </view>
      <button class="search-btn" @click="doSearch">搜索</button>
    </view>

    <!-- 搜索结果 -->
    <view class="search-result" v-if="searched">
      <view
        v-for="user in users"
        :key="user.id"
        class="user-item"
        @click="showAddDialog(user)"
      >
        <image
          class="avatar-sm"
          :src="user.avatar || '/static/default-avatar.svg'"
          mode="aspectFill"
        />
        <view class="user-info">
          <text class="user-name">{{ user.nickname }}</text>
          <text class="user-account">用户名: {{ user.username }}</text>
          <text class="user-phone" v-if="user.phone">手机号: {{ user.phone }}</text>
        </view>
        <button class="add-btn" size="mini" @click.stop="showAddDialog(user)">添加</button>
      </view>

      <view v-if="users.length === 0 && searched" class="no-result">
        <text>未找到相关用户</text>
      </view>
    </view>

    <!-- 发送好友请求对话框 -->
    <view v-if="showDialog" class="dialog-mask" @click="showDialog = false">
      <view class="dialog-box" @click.stop>
        <text class="dialog-title">添加好友</text>
        <text class="dialog-subtitle">将发送好友请求给 {{ selectedUser?.nickname }}</text>
        <textarea
          v-model="verifyMessage"
          class="verify-input"
          placeholder="请输入验证消息（选填）"
          placeholder-class="verify-placeholder"
          maxlength="100"
        />
        <view class="dialog-buttons">
          <button class="dialog-btn cancel-btn" @click="showDialog = false">取消</button>
          <button class="dialog-btn confirm-btn" @click="sendRequest">发送</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchUsers, sendFriendRequest } from '@/utils/api'
import wsManager from '@/utils/websocket'

const keyword = ref('')
const users = ref<any[]>([])
const searched = ref(false)
const showDialog = ref(false)
const selectedUser = ref<any>(null)
const verifyMessage = ref('')

async function doSearch() {
  if (!keyword.value.trim()) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: '搜索中...' })
    const res = await searchUsers(keyword.value.trim())
    if (res.code === 200 && res.data) {
      users.value = res.data
      searched.value = true
    }
  } catch (e) {
    uni.showToast({ title: '搜索失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function clearSearch() {
  keyword.value = ''
  users.value = []
  searched.value = false
}

function showAddDialog(user: any) {
  selectedUser.value = user
  verifyMessage.value = ''
  showDialog.value = true
}

async function sendRequest() {
  if (!selectedUser.value) return

  try {
    uni.showLoading({ title: '发送中...' })
    await sendFriendRequest(selectedUser.value.id, verifyMessage.value || undefined)

    // 通过 WebSocket 通知对方
    wsManager.sendFriendRequestNotify(selectedUser.value.id)

    showDialog.value = false
    uni.hideLoading()
    uni.showToast({ title: '好友请求已发送', icon: 'success' })

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.add-friend-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;

  .search-input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    background-color: #ffffff;
    border-radius: 12rpx;
    padding: 0 20rpx;
    height: 72rpx;

    .search-icon {
      font-size: 32rpx;
      margin-right: 12rpx;
    }

    .search-input {
      flex: 1;
      height: 100%;
      font-size: 28rpx;
    }

    .search-placeholder {
      color: #bbb;
    }

    .clear-btn {
      font-size: 28rpx;
      color: #ccc;
      padding: 8rpx;
    }
  }

  .search-btn {
    height: 72rpx;
    line-height: 72rpx;
    background-color: #07C160;
    color: #ffffff;
    font-size: 28rpx;
    padding: 0 28rpx;
    margin-left: 16rpx;
    border: none;
    border-radius: 12rpx;
  }
}

.user-item {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  .user-info {
    flex: 1;
    margin-left: 24rpx;

    .user-name {
      font-size: 30rpx;
      color: #333;
      display: block;
    }

    .user-account {
      font-size: 24rpx;
      color: #999;
      display: block;
      margin-top: 6rpx;
    }

    .user-phone {
      font-size: 24rpx;
      color: #999;
      display: block;
      margin-top: 4rpx;
    }
  }

  .add-btn {
    background-color: #07C160;
    color: #ffffff;
    border: none;
  }
}

.no-result {
  text-align: center;
  padding: 100rpx;
  color: #999;
  font-size: 28rpx;
}

/* 对话框 */
.dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-box {
  width: 80%;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 40rpx;

  .dialog-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #333;
    display: block;
    text-align: center;
    margin-bottom: 12rpx;
  }

  .dialog-subtitle {
    font-size: 26rpx;
    color: #999;
    display: block;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .verify-input {
    width: 100%;
    height: 150rpx;
    background-color: #f5f5f5;
    border-radius: 12rpx;
    padding: 16rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }

  .verify-placeholder {
    color: #bbb;
  }

  .dialog-buttons {
    display: flex;
    margin-top: 30rpx;

    .dialog-btn {
      flex: 1;
      height: 76rpx;
      line-height: 76rpx;
      text-align: center;
      border-radius: 12rpx;
      font-size: 30rpx;
      border: none;
    }

    .cancel-btn {
      background-color: #f5f5f5;
      color: #666;
      margin-right: 20rpx;
    }

    .confirm-btn {
      background-color: #07C160;
      color: #ffffff;
    }
  }
}
</style>
