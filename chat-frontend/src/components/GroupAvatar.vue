<template>
  <view class="group-avatar" :style="{ width: size + 'rpx', height: size + 'rpx' }">
    <!-- 有成员头像：拼接网格 -->
    <view v-if="validAvatars.length > 0" class="avatar-grid" :class="gridClass">
      <image
        v-for="(avatar, idx) in gridAvatars"
        :key="idx"
        class="grid-avatar"
        :src="avatar"
        mode="aspectFill"
      />
    </view>
    <!-- 无成员头像：显示群默认图标 -->
    <image v-else class="default-avatar" :src="defaultSrc" mode="aspectFill" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  avatars: string[]
  size?: number
  defaultSrc?: string
}>(), {
  avatars: () => [],
  size: 96,
  defaultSrc: '/static/default-group.svg'
})

/** 过滤空值，最多取 9 个头像（微信风格） */
const validAvatars = computed(() => {
  return props.avatars.filter(a => a && a.trim()).slice(0, 9)
})

/** 需要展示在第几个格子 */
const gridAvatars = computed(() => {
  const list = [...validAvatars.value]
  // 至少需要 1 个才走网格模式
  return list
})

/** 根据数量决定网格布局 */
const gridClass = computed(() => {
  const count = validAvatars.value.length
  if (count <= 0) return ''
  if (count <= 4) return 'grid-2x2'
  return 'grid-3x3'
})
</script>

<style lang="scss" scoped>
.group-avatar {
  flex-shrink: 0;
  border-radius: 12rpx;
  overflow: hidden;
  background-color: #e0e0e0;
}

.avatar-grid {
  width: 100%;
  height: 100%;
  display: grid;
  gap: 1rpx;
  background-color: #fff;
}

.grid-2x2 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}

.grid-3x3 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
}

.grid-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background-color: #e8e8e8;
}

.default-avatar {
  width: 100%;
  height: 100%;
}
</style>
