import type { ApiErrorResponse, ApiResponse } from '../types/api'

export type { ApiErrorResponse, ApiResponse } from '../types/api'

export class ApiClientError extends Error {
  readonly httpStatus: number
  readonly apiCode: number
  readonly detail: string

  constructor(httpStatus: number, apiCode: number, shortMessage: string, detail: string) {
    const combined = detail ? `${shortMessage}：${detail}` : shortMessage
    super(combined)
    this.name = 'ApiClientError'
    this.httpStatus = httpStatus
    this.apiCode = apiCode
    this.detail = detail
  }
}

export function getApiBase(): string {
  const v = import.meta.env.VITE_API_BASE
  if (typeof v === 'string' && v.length > 0) {
    return v.replace(/\/$/, '')
  }
  return '/api'
}

export function buildApiUrl(path: string): string {
  const base = getApiBase()
  const p = path.startsWith('/') ? path : `/${path}`
  return `${base}${p}`
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

function isApiErrorBody(v: unknown): v is ApiErrorResponse {
  if (!isRecord(v)) return false
  return (
    typeof v.code === 'number' &&
    typeof v.message === 'string' &&
    typeof v.error === 'string'
  )
}

function isApiSuccessEnvelope(v: unknown): v is ApiResponse<unknown> {
  if (!isRecord(v)) return false
  return typeof v.code === 'number' && typeof v.message === 'string' && 'data' in v
}

export async function parseJsonResponse<T>(res: Response): Promise<T> {
  const text = await res.text()
  if (!text) {
    throw new Error('空响应体')
  }
  return JSON.parse(text) as T
}

function mergeHeaders(init?: RequestInit): Headers {
  const h = new Headers(init?.headers)
  if (init?.body && !(init.body instanceof FormData) && !h.has('Content-Type')) {
    h.set('Content-Type', 'application/json')
  }
  return h
}

/**
 * 统一 JSON 请求：成功时返回 `data`；失败抛出 {@link ApiClientError}（含后端 code/message/error）。
 */
export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const url = path.startsWith('http://') || path.startsWith('https://') ? path : buildApiUrl(path)
  let res: Response
  try {
    res = await fetch(url, { ...init, headers: mergeHeaders(init) })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new ApiClientError(0, 0, '网络请求失败', msg)
  }

  const text = await res.text()
  let body: unknown
  if (text) {
    try {
      body = JSON.parse(text) as unknown
    } catch {
      throw new ApiClientError(
        res.status,
        res.status,
        '响应不是合法 JSON',
        text.length > 200 ? `${text.slice(0, 200)}…` : text,
      )
    }
  } else {
    body = null
  }

  if (res.ok) {
    if (!isApiSuccessEnvelope(body)) {
      throw new ApiClientError(
        res.status,
        res.status,
        '成功响应格式异常',
        text.length > 120 ? `${text.slice(0, 120)}…` : text || '(空)',
      )
    }
    if (body.code !== 200) {
      const errPart = isApiErrorBody(body) ? body.error : JSON.stringify(body)
      throw new ApiClientError(res.status, body.code, body.message, errPart)
    }
    return body.data as T
  }

  if (isApiErrorBody(body)) {
    throw new ApiClientError(res.status, body.code, body.message, body.error)
  }

  throw new ApiClientError(
    res.status,
    res.status,
    res.statusText || '请求失败',
    typeof body === 'object' && body !== null ? JSON.stringify(body) : String(body),
  )
}

export function formatApiClientError(err: unknown): string {
  if (err instanceof ApiClientError) {
    return err.message
  }
  if (err instanceof Error) {
    return err.message
  }
  return String(err)
}
