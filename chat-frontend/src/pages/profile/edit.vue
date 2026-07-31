<template>
  <view class="edit-profile-page">
    <!-- 头像 -->
    <view class="edit-section">
      <view class="edit-item" @click="changeAvatar">
        <text class="edit-label">头像</text>
        <view class="edit-value">
          <image
            class="avatar-sm"
            :src="form.avatar || '/static/default-avatar.svg'"
            mode="aspectFill"
          />
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="edit-section">
      <view class="edit-item">
        <text class="edit-label">昵称</text>
        <view class="edit-value">
          <input
            v-model="form.nickname"
            class="edit-input"
            type="text"
            placeholder="请输入昵称"
            placeholder-class="placeholder"
            maxlength="30"
          />
        </view>
      </view>

      <view class="edit-item" @click="selectGender">
        <text class="edit-label">性别</text>
        <view class="edit-value">
          <text class="edit-text">{{ genderText }}</text>
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="edit-item">
        <text class="edit-label">手机号</text>
        <view class="edit-value">
          <input
            v-model="form.phone"
            class="edit-input"
            type="number"
            placeholder="请输入手机号"
            placeholder-class="placeholder"
            maxlength="11"
          />
        </view>
      </view>

      <view class="edit-item">
        <text class="edit-label">邮箱</text>
        <view class="edit-value">
          <input
            v-model="form.email"
            class="edit-input"
            type="text"
            placeholder="请输入邮箱"
            placeholder-class="placeholder"
          />
        </view>
      </view>
    </view>

    <!-- 个性签名 -->
    <view class="edit-section">
      <view class="edit-item signature-item">
        <text class="edit-label">个性签名</text>
        <textarea
          v-model="form.signature"
          class="signature-input"
          placeholder="请输入个性签名"
          placeholder-class="placeholder"
          maxlength="100"
        />
      </view>
    </view>

    <!-- 保存按钮 -->
    <view class="save-section">
      <button class="save-btn" :disabled="saving" :loading="saving" @click="handleSave">
        保存
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { updateProfile, uploadImage } from '@/utils/api'

const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  gender: 0,
  signature: ''
})

const genderText = computed(() => {
  const map: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
  return map[form.gender] || '未知'
})

onMounted(() => {
  const user = userStore.userInfo
  if (user) {
    form.nickname = user.nickname || ''
    form.avatar = user.avatar || ''
    form.phone = user.phone || ''
    form.email = user.email || ''
    form.gender = user.gender || 0
    form.signature = user.signature || ''
  }
})

function selectGender() {
  uni.showActionSheet({
    itemList: ['男', '女', '未知'],
    success: (res) => {
      const map: Record<number, number> = { 0: 1, 1: 2, 2: 0 }
      form.gender = map[res.tapIndex]
    }
  })
}

async function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      try {
        const result = await uploadImage(filePath)
        if (result.data?.url) {
          form.avatar = result.data.url
          uni.showToast({ title: '头像已更新', icon: 'success' })
        }
      } catch (e) {
        console.error('头像上传失败', e)
      }
    }
  })
}

async function handleSave() {
  if (saving.value) return

  saving.value = true
  try {
    const res = await updateProfile(form)
    if (res.code === 200) {
      // 刷新用户信息
      await userStore.refreshUserInfo()
      uni.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.edit-profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60rpx;
}

.edit-section {
  background-color: #ffffff;
  margin-bottom: 20rpx;
}

.edit-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  .edit-label {
    width: 160rpx;
    font-size: 30rpx;
    color: #333;
  }

  .edit-value {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }

  .edit-input {
    flex: 1;
    text-align: right;
    height: 60rpx;
    font-size: 30rpx;
    color: #333;
  }

  .edit-text {
    font-size: 30rpx;
    color: #666;
  }

  .placeholder {
    color: #bbb;
  }

  .arrow {
    font-size: 36rpx;
    color: #ccc;
    margin-left: 10rpx;
  }
}

.signature-item {
  align-items: flex-start;

  .signature-input {
    flex: 1;
    height: 120rpx;
    font-size: 30rpx;
    color: #333;
    text-align: right;
  }
}

.save-section {
  padding: 40rpx 30rpx;

  .save-btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    background-color: #07C160;
    color: #ffffff;
    font-size: 32rpx;
    border-radius: 12rpx;
    border: none;
  }
}
</style>
