/// <reference types="@dcloudio/types" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'sockjs-client'
declare module 'webstomp-client'

interface UniApp {
  getStorageSync(key: string): any
  setStorageSync(key: string, data: any): void
  removeStorageSync(key: string): void
  showToast(options: UniNamespace.ShowToastOptions): void
  showLoading(options: UniNamespace.ShowLoadingOptions): void
  hideLoading(): void
  showModal(options: UniNamespace.ShowModalOptions): void
  showActionSheet(options: UniNamespace.ShowActionSheetOptions): void
  navigateTo(options: UniNamespace.NavigateToOptions): void
  navigateBack(options?: UniNamespace.NavigateBackOptions): void
  switchTab(options: UniNamespace.SwitchTabOptions): void
  reLaunch(options: UniNamespace.ReLaunchOptions): void
  chooseImage(options: UniNamespace.ChooseImageOptions): void
  request(options: UniNamespace.RequestOptions): UniNamespace.RequestTask
}

declare const uni: UniApp
