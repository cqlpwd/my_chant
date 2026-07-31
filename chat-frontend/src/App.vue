<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(async () => {
  console.log('App 启动成功')
  try {
    const { useUserStore } = await import('@/store/user')
    const userStore = useUserStore()
    userStore.checkLogin()
  } catch (e) {
    console.error('init error:', e)
  }
})
</script>

<style lang="scss">
@use '@/styles/common.scss' as *;
</style>
