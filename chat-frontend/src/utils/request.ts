/**
 * HTTP 请求工具（基于 uni.request 封装）
 */

// 后端 API 基础地址
// H5：使用相对路径，走 Vite 代理（/api、/uploads），
// 这样 HTTPS 页面不会因请求 http 后端触发混合内容拦截，也能避免跨域
// 小程序/App：无法走浏览器代理，使用局域网绝对地址
// #ifdef H5
const BASE_URL = ''
// #endif

// #ifndef H5
const BASE_URL = 'http://10.116.22.160:8080'
// #endif

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
}

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

class HttpRequest {
  private baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }

  private getToken(): string {
    try {
      const userInfo = uni.getStorageSync('userInfo')
      return userInfo?.token || ''
    } catch {
      return ''
    }
  }

  request<T = any>(options: RequestOptions): Promise<ApiResponse<T>> {
    const { url, method = 'GET', data, header = {}, showLoading = false } = options

    if (showLoading) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    const token = this.getToken()

    return new Promise((resolve, reject) => {
      uni.request({
        url: this.baseUrl + url,
        method,
        data,
        timeout: 10000, // 10 秒超时
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : '',
          ...header
        },
        success: (res: any) => {
          if (showLoading) uni.hideLoading()

          const resData = res.data as ApiResponse<T>

          if (res.statusCode === 200) {
            if (resData.code === 200) {
              resolve(resData)
            } else {
              uni.showToast({ title: resData.message || '请求失败', icon: 'none' })
              reject(resData)
            }
          } else if (res.statusCode === 401) {
            // Token 过期，跳转登录页
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/login/index' })
            reject(resData)
          } else {
            uni.showToast({ title: `服务器错误 (${res.statusCode})`, icon: 'none' })
            reject(resData)
          }
        },
        fail: (err: any) => {
          if (showLoading) uni.hideLoading()
          const msg = err.errMsg || ''
          if (msg.includes('timeout')) {
            uni.showToast({ title: '连接服务器超时，请重试', icon: 'none', duration: 2500 })
          } else if (msg.includes('fail')) {
            uni.showToast({ title: '无法连接服务器，请检查后端是否启动', icon: 'none', duration: 3000 })
          } else {
            uni.showToast({ title: '网络错误，请检查网络', icon: 'none' })
          }
          reject(err)
        }
      })
    })
  }

  get<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'GET', data })
  }

  post<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'POST', data })
  }

  put<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'PUT', data })
  }

  delete<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>({ url, method: 'DELETE', data })
  }

  /**
   * 文件上传（multipart/form-data）
   */
  upload<T = any>(url: string, filePath: string, formData?: Record<string, string>): Promise<ApiResponse<T>> {
    return new Promise((resolve, reject) => {
      const token = this.getToken()
      uni.showLoading({ title: '上传中...', mask: true })

      uni.uploadFile({
        url: this.baseUrl + url,
        filePath,
        name: 'file',
        formData,
        header: {
          'Authorization': token ? `Bearer ${token}` : ''
        },
        timeout: 30000,
        success: (res: any) => {
          uni.hideLoading()
          try {
            const resData = JSON.parse(res.data) as ApiResponse<T>
            if (res.statusCode === 200 && resData.code === 200) {
              resolve(resData)
            } else if (res.statusCode === 401) {
              uni.removeStorageSync('userInfo')
              uni.reLaunch({ url: '/pages/login/index' })
              reject(resData)
            } else {
              uni.showToast({ title: resData.message || '上传失败', icon: 'none' })
              reject(resData)
            }
          } catch {
            uni.showToast({ title: '解析响应失败', icon: 'none' })
            reject(new Error('解析失败'))
          }
        },
        fail: (err: any) => {
          uni.hideLoading()
          const msg = err.errMsg || ''
          if (msg.includes('timeout')) {
            uni.showToast({ title: '上传超时，请重试', icon: 'none' })
          } else {
            uni.showToast({ title: '上传失败，请检查网络', icon: 'none' })
          }
          reject(err)
        }
      })
    })
  }
}

export const http = new HttpRequest(BASE_URL)
export default http
