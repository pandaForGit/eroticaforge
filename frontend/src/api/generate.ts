import type { GenerateRequest, GenerateSyncResponse } from '../types/generate'
import type { ApiErrorResponse } from '../types/api'
import {
  ApiClientError,
  buildApiUrl,
  formatApiClientError,
  requestJson,
} from './client'

function isApiErrorBody(v: unknown): v is ApiErrorResponse {
  return (
    typeof v === 'object' &&
    v !== null &&
    typeof (v as ApiErrorResponse).code === 'number' &&
    typeof (v as ApiErrorResponse).message === 'string' &&
    typeof (v as ApiErrorResponse).error === 'string'
  )
}

export async function generateSync(
  storyId: string,
  body: GenerateRequest,
): Promise<GenerateSyncResponse> {
  return requestJson<GenerateSyncResponse>(
    `/stories/${encodeURIComponent(storyId)}/generate`,
    {
      method: 'POST',
      body: JSON.stringify(body),
    },
  )
}

export type StreamDonePayload = {
  chapterId: string
  /** 落库用的最终正文长度；0 表示模型未返回可见正文 */
  generatedChars: number
  tokenFrames?: number
  nonEmptyTokenFrames?: number
}

export type StreamGenerateHandlers = {
  onToken: (chunk: string) => void
  onDone: (payload: StreamDonePayload) => void
  onStreamError: (message: string) => void
}

function parseSsePayload(raw: string): Record<string, unknown> | null {
  if (!raw) return null
  try {
    let o = JSON.parse(raw) as unknown
    // Spring 曾用 data(s, APPLICATION_JSON) 导致 String 被二次 JSON 编码，首层 parse 得到 string
    if (typeof o === 'string') {
      try {
        o = JSON.parse(o) as unknown
      } catch {
        return null
      }
    }
    return typeof o === 'object' && o !== null ? (o as Record<string, unknown>) : null
  } catch {
    return null
  }
}

function parseSseDataLine(line: string): Record<string, unknown> | null {
  const t = line.trimEnd()
  if (!t || t.startsWith(':')) return null
  if (!t.startsWith('data:')) return null
  const json = t.slice(5).trimStart()
  return parseSsePayload(json)
}

/**
 * POST SSE：fetch + ReadableStream 解析 {@code data: {...}} 行。
 */
export async function streamGenerate(
  storyId: string,
  body: GenerateRequest,
  handlers: StreamGenerateHandlers,
  init?: { signal?: AbortSignal },
): Promise<void> {
  const url = buildApiUrl(
    `/stories/${encodeURIComponent(storyId)}/generate/stream`,
  )
  let res: Response
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify(body),
      signal: init?.signal,
    })
  } catch (e) {
    throw new ApiClientError(0, 0, '流式请求失败', formatApiClientError(e))
  }

  if (!res.ok) {
    const text = await res.text()
    let msg = text || `${res.status} ${res.statusText}`
    try {
      const errBody = text ? (JSON.parse(text) as unknown) : null
      if (isApiErrorBody(errBody)) {
        msg = `${errBody.message}：${errBody.error}`
      }
    } catch {
      if (text.length > 300) msg = `${text.slice(0, 300)}…`
    }
    throw new ApiClientError(res.status, res.status, '生成失败', msg)
  }

  const reader = res.body?.getReader()
  if (!reader) {
    throw new ApiClientError(res.status, res.status, '生成失败', '响应无 body')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  const extractCompleteLines = (buf: string): { rest: string; lines: string[] } => {
    const lines: string[] = []
    let start = 0
    for (let i = 0; i < buf.length; i++) {
      if (buf.charCodeAt(i) === 0x0a) {
        let line = buf.slice(start, i)
        if (line.endsWith('\r')) {
          line = line.slice(0, -1)
        }
        lines.push(line)
        start = i + 1
      }
    }
    return { rest: buf.slice(start), lines }
  }

  const flushLine = (line: string) => {
    const obj = parseSseDataLine(line)
    if (!obj) return
    const doneByFlag = obj.done === true
    const doneByType = obj.type === 'done'
    if ((doneByFlag || doneByType) && typeof obj.chapterId === 'string') {
      const gen =
        typeof obj.generatedChars === 'number' && Number.isFinite(obj.generatedChars)
          ? obj.generatedChars
          : 0
      const tf =
        typeof obj.tokenFrames === 'number' && Number.isFinite(obj.tokenFrames)
          ? obj.tokenFrames
          : undefined
      const netf =
        typeof obj.nonEmptyTokenFrames === 'number' &&
        Number.isFinite(obj.nonEmptyTokenFrames)
          ? obj.nonEmptyTokenFrames
          : undefined
      handlers.onDone({
        chapterId: obj.chapterId,
        generatedChars: gen,
        tokenFrames: tf,
        nonEmptyTokenFrames: netf,
      })
      return
    }
    if (typeof obj.error === 'string') {
      handlers.onStreamError(obj.error)
      return
    }
    if (obj.type === 'token' && typeof obj.content === 'string') {
      handlers.onToken(obj.content)
    }
  }

  try {
    for (;;) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      const { rest, lines } = extractCompleteLines(buffer)
      buffer = rest
      for (const line of lines) {
        flushLine(line)
      }
      if (done) break
    }
    if (buffer.length > 0) {
      const { lines } = extractCompleteLines(buffer + '\n')
      for (const line of lines) {
        flushLine(line)
      }
    }
  } finally {
    reader.releaseLock()
  }
}
