<template>
  <view class="create-page">
    <!-- 内容输入 -->
    <view class="input-area">
      <textarea
        v-model="content"
        class="content-input"
        placeholder="这一刻的想法..."
        placeholder-class="placeholder"
        maxlength="2000"
        auto-height
        :focus="true"
      />
    </view>

    <!-- 已选图片 -->
    <view v-if="imageList.length > 0" class="image-area">
      <view v-for="(img, idx) in imageList" :key="idx" class="image-item">
        <image :src="img" class="uploaded-image" mode="aspectFill" @click="previewImage(idx)" />
        <view class="image-remove" @click="removeImage(idx)">✕</view>
      </view>
      <view v-if="imageList.length < 9" class="add-image" @click="chooseImage">
        <text class="add-icon">+</text>
      </view>
    </view>
    <view v-else class="image-btn" @click="chooseImage">
      <text class="btn-icon">📷</text>
      <text class="btn-text">添加图片</text>
    </view>

    <!-- 位置 -->
    <view class="location-row" @click="chooseLocation">
      <text class="location-icon">📍</text>
      <text class="location-text" v-if="location">{{ location }}</text>
      <text class="location-text placeholder" v-else>所在位置</text>
      <text class="location-arrow">›</text>
    </view>

    <!-- 发布按钮 -->
    <view class="submit-bar">
      <button
        class="submit-btn"
        :disabled="!canSubmit || uploading"
        :loading="uploading"
        @click="handleSubmit"
      >
        发表
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { createMoment, uploadImage } from '@/utils/api'

const content = ref('')
const imageList = ref<string[]>([])
const location = ref('')
const uploading = ref(false)

const canSubmit = computed(() => {
  return content.value.trim() || imageList.value.length > 0
})

function chooseImage() {
  const remain = 9 - imageList.value.length
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      imageList.value.push(...res.tempFilePaths)
    }
  })
}

function removeImage(idx: number) {
  imageList.value.splice(idx, 1)
}

function previewImage(idx: number) {
  uni.previewImage({
    urls: imageList.value,
    current: idx
  })
}

function chooseLocation() {
  uni.showToast({ title: '位置功能开发中', icon: 'none' })
}

async function handleSubmit() {
  if (!canSubmit.value || uploading.value) return

  uploading.value = true
  try {
    // 先上传图片
    let imageUrls = ''
    if (imageList.value.length > 0) {
      const uploadPromises = imageList.value.map(async (filePath) => {
        const res = await uploadImage(filePath)
        if (res.code === 200 && res.data?.url) {
          return res.data.url
        }
        return filePath
      })
      const urls = await Promise.all(uploadPromises)
      imageUrls = JSON.stringify(urls.filter(Boolean))
    }

    await createMoment({
      content: content.value.trim() || undefined,
      images: imageUrls || undefined,
      location: location.value || undefined
    })

    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e.message || '发布失败', icon: 'none' })
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.create-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx 30rpx;
}

.input-area {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 30rpx;

  .content-input {
    width: 100%;
    min-height: 200rpx;
    font-size: 30rpx;
    color: #333;
    line-height: 1.6;
  }

  .placeholder {
    color: #bbb;
  }
}

.image-btn {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx 30rpx;
  margin-top: 20rpx;

  .btn-icon {
    font-size: 36rpx;
    margin-right: 16rpx;
  }

  .btn-text {
    font-size: 28rpx;
    color: #666;
  }
}

.image-area {
  display: flex;
  flex-wrap: wrap;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-top: 20rpx;
}

.image-item {
  position: relative;
  margin-right: 16rpx;
  margin-bottom: 16rpx;

  .uploaded-image {
    width: 200rpx;
    height: 200rpx;
    border-radius: 8rpx;
  }

  .image-remove {
    position: absolute;
    top: -10rpx;
    right: -10rpx;
    width: 40rpx;
    height: 40rpx;
    background-color: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 24rpx;
  }
}

.add-image {
  width: 200rpx;
  height: 200rpx;
  border: 2rpx dashed #ddd;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  .add-icon {
    font-size: 60rpx;
    color: #ccc;
  }
}

.location-row {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx 30rpx;
  margin-top: 20rpx;

  .location-icon {
    font-size: 32rpx;
    margin-right: 16rpx;
  }

  .location-text {
    flex: 1;
    font-size: 28rpx;
    color: #333;

    &.placeholder {
      color: #bbb;
    }
  }

  .location-arrow {
    font-size: 36rpx;
    color: #ccc;
  }
}

.submit-bar {
  margin-top: 60rpx;

  .submit-btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    background-color: #07C160;
    color: #fff;
    font-size: 32rpx;
    font-weight: bold;
    border-radius: 44rpx;
    border: none;

    &:disabled {
      background-color: #a8e6c1;
    }
  }
}
</style>
