import axios from 'axios'
import { STORAGE_KEYS } from '../constants/storage'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  // withCredentials lets the browser send the XSRF-TOKEN cookie that Spring's
  // CookieCsrfTokenRepository sets on the first GET response. The manual
  // request interceptor below echoes it back in the X-XSRF-TOKEN header on
  // state-changing requests, which is what Spring Security validates against.
  withCredentials: true,
})

// Read a single cookie by name. Returns null if absent.
function readCookie(name: string): string | null {
  const match = document.cookie
    .split('; ')
    .find(row => row.startsWith(name + '='))
  return match ? decodeURIComponent(match.split('=')[1]) : null
}

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  // CSRF token relay. The XSRF-TOKEN cookie is set by Spring on the first
  // non-exempt response; we read it here and forward as X-XSRF-TOKEN on
  // state-changing requests. GET/HEAD/OPTIONS don't need it.
  const method = (config.method ?? 'get').toLowerCase()
  if (method !== 'get' && method !== 'head' && method !== 'options') {
    const csrf = readCookie('XSRF-TOKEN')
    if (csrf) {
      config.headers['X-XSRF-TOKEN'] = csrf
    }
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem(STORAGE_KEYS.TOKEN)) {
      localStorage.removeItem(STORAGE_KEYS.TOKEN)
      localStorage.removeItem(STORAGE_KEYS.USER)
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
