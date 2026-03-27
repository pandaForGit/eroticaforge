import type { HealthResponse } from '../types/health'
import { buildApiUrl } from './client'

/**
 * 健康检查接口返回裸 Map，非 {@link ApiResponse} 包裹。
 */
export async function fetchHealth(): Promise<HealthResponse> {
  const url = buildApiUrl('/health')
  const res = await fetch(url, { method: 'GET' })
  const text = await res.text()
  if (!text) {
    throw new Error('健康检查返回空响应')
  }
  const body = JSON.parse(text) as unknown
  if (!res.ok || typeof body !== 'object' || body === null) {
    throw new Error(text.length > 200 ? `${text.slice(0, 200)}…` : text)
  }
  return body as HealthResponse
}
