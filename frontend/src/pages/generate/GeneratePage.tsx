import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { generateSync, streamGenerate } from '../../api/generate'
import { getStory } from '../../api/stories'
import { useToast } from '../../components/useToast'
import type { StoryDetailDto } from '../../types/stories'

export function GeneratePage() {
  const { storyId = '' } = useParams<{ storyId: string }>()
  const { showError, showInfo } = useToast()
  const [story, setStory] = useState<StoryDetailDto | null>(null)
  const [loadingStory, setLoadingStory] = useState(true)
  const [prompt, setPrompt] = useState('')
  const [maxTokens, setMaxTokens] = useState<number | ''>('')
  const [useMultiModel, setUseMultiModel] = useState(false)
  const [streaming, setStreaming] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [output, setOutput] = useState('')
  const [lastChapterId, setLastChapterId] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  const outputEndRef = useRef<HTMLDivElement>(null)
  const outputBoxRef = useRef<HTMLDivElement>(null)

  const scrollOutputToBottom = useCallback(() => {
    const el = outputBoxRef.current
    if (el) {
      el.scrollTop = el.scrollHeight
    }
    outputEndRef.current?.scrollIntoView({ block: 'end' })
  }, [])

  useEffect(() => {
    if (!storyId) return
    let cancelled = false
    setLoadingStory(true)
    void getStory(storyId)
      .then((s) => {
        if (!cancelled) setStory(s)
      })
      .catch((e) => {
        if (!cancelled) {
          setStory(null)
          showError(formatApiClientError(e))
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingStory(false)
      })
    return () => {
      cancelled = true
    }
  }, [storyId, showError])

  useEffect(() => {
    if (streaming && output) {
      scrollOutputToBottom()
    }
  }, [output, streaming, scrollOutputToBottom])

  function stopStream() {
    abortRef.current?.abort()
    abortRef.current = null
    setStreaming(false)
  }

  async function startStream() {
    if (!storyId) return
    const p = prompt.trim()
    if (!p) {
      showError('请输入 prompt')
      return
    }
    if (useMultiModel) {
      showError('多模型链尚未实现，请关闭「多模型链」后再试')
      return
    }

    stopStream()
    const ac = new AbortController()
    abortRef.current = ac
    setStreaming(true)
    setOutput('')
    setLastChapterId(null)

    const body = {
      prompt: p,
      maxTokens: maxTokens === '' ? null : maxTokens,
      useMultiModel: false,
    }

    let sawDone = false
    let sawStreamErr = false
    try {
      await streamGenerate(
        storyId,
        body,
        {
          onToken: (chunk) => setOutput((prev) => prev + chunk),
          onDone: ({ chapterId, generatedChars, tokenFrames, nonEmptyTokenFrames }) => {
            sawDone = true
            setLastChapterId(chapterId)
            if (generatedChars === 0) {
              setOutput((prev) => {
                if (prev && prev.length > 0) return prev
                const tf =
                  tokenFrames != null && nonEmptyTokenFrames != null
                    ? `（后端 token 帧 ${tokenFrames}，其中非空 ${nonEmptyTokenFrames}）`
                    : ''
                return `（本轮未收到可见正文，已落库空章节。${tf}请查后端 WARN「模型结束但正文为空」与 llama-server 流式配置。）`
              })
              showError(
                `生成结束但正文长度为 0，章节 ID：${chapterId}。请对照后端日志与 OpenAI 兼容服务的 stream 输出。`,
              )
            } else {
              showInfo(`生成完成，章节 ID：${chapterId}，正文约 ${generatedChars} 字`)
            }
          },
          onStreamError: (msg) => {
            sawStreamErr = true
            showError(msg)
          },
        },
        { signal: ac.signal },
      )
      if (!ac.signal.aborted && !sawDone && !sawStreamErr) {
        showInfo('流已结束（未收到带 chapterId 的完成帧，可核对输出或后端日志）')
      }
    } catch (e) {
      if (e instanceof Error && e.name === 'AbortError') {
        showInfo('已中断生成')
      } else {
        showError(formatApiClientError(e))
      }
    } finally {
      setStreaming(false)
      abortRef.current = null
    }
  }

  async function runSync() {
    if (!storyId) return
    const p = prompt.trim()
    if (!p) {
      showError('请输入 prompt')
      return
    }
    if (useMultiModel) {
      showError('多模型链尚未实现，请关闭「多模型链」后再试')
      return
    }
    setSyncing(true)
    try {
      const body = {
        prompt: p,
        maxTokens: maxTokens === '' ? null : maxTokens,
        useMultiModel: false,
      }
      const res = await generateSync(storyId, body)
      setOutput(res.text)
      setLastChapterId(res.chapterId)
      if (res.ragWarning) {
        showError(res.ragWarning)
      }
      showInfo(`非流式生成完成，章节 ID：${res.chapterId}`)
      scrollOutputToBottom()
    } catch (e) {
      showError(formatApiClientError(e))
    } finally {
      setSyncing(false)
    }
  }

  useEffect(() => () => stopStream(), [])

  if (!storyId) {
    return (
      <div className="alert alert-warning">
        缺少故事 ID。<Link to="/">返回列表</Link>
      </div>
    )
  }

  if (loadingStory) {
    return (
      <div className="flex justify-center py-20">
        <span className="loading loading-spinner loading-lg text-primary" />
      </div>
    )
  }

  if (!story) {
    return (
      <div className="space-y-4">
        <Link to="/" className="link link-hover text-sm">
          ← 返回列表
        </Link>
        <div className="alert alert-error">无法加载故事。</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <Link
          to={`/stories/${encodeURIComponent(storyId)}`}
          className="link link-hover text-sm"
        >
          ← 故事详情
        </Link>
        <h1 className="mt-2 text-2xl font-bold">流式生成</h1>
        <p className="mt-1 font-mono text-xs text-base-content/60">
          {story.storyId}
        </p>
      </div>

      <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm space-y-4">
        <label className="form-control">
          <span className="label-text text-sm font-medium">prompt</span>
          <textarea
            className="textarea textarea-bordered min-h-32 w-full font-mono text-sm"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="续写指令或场景描述…"
            disabled={streaming}
          />
        </label>

        <div className="flex flex-wrap items-end gap-4">
          <label className="form-control w-40">
            <span className="label-text text-xs">maxTokens（可选）</span>
            <input
              type="number"
              min={1}
              className="input input-bordered input-sm"
              value={maxTokens}
              onChange={(e) => {
                const v = e.target.value
                setMaxTokens(v === '' ? '' : Number(v))
              }}
              placeholder="留空用默认；正整数覆盖本轮 max_tokens"
              disabled={streaming}
            />
          </label>

          <label className="label cursor-pointer gap-2">
            <input
              type="checkbox"
              className="toggle toggle-sm"
              checked={useMultiModel}
              onChange={(e) => setUseMultiModel(e.target.checked)}
              disabled={streaming}
            />
            <span className="label-text text-sm">多模型链</span>
          </label>
        </div>

        {useMultiModel && (
          <div className="alert alert-warning text-sm py-2">
            当前后端对多模型链返回 501，请勿在联调环境开启；仅供接口对齐占位。
          </div>
        )}

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-primary btn-sm"
            disabled={streaming || syncing}
            onClick={() => void startStream()}
          >
            {streaming ? (
              <>
                <span className="loading loading-spinner loading-xs" />
                生成中…
              </>
            ) : (
              '开始（SSE）'
            )}
          </button>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            disabled={streaming || syncing}
            onClick={() => stopStream()}
          >
            中断
          </button>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={streaming || syncing}
            onClick={() => void runSync()}
          >
            {syncing ? (
              <>
                <span className="loading loading-spinner loading-xs" />
                非流式…
              </>
            ) : (
              '非流式调试'
            )}
          </button>
        </div>
      </div>

      <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm">
        <div className="mb-2 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-base-content/80">输出</h2>
          {lastChapterId && (
            <Link
              to={`/stories/${encodeURIComponent(storyId)}/read?chapter=${encodeURIComponent(lastChapterId)}`}
              className="link link-primary text-xs"
            >
              去阅读本章 →
            </Link>
          )}
        </div>
        <div
          ref={outputBoxRef}
          className="max-h-[min(60vh,28rem)] overflow-y-auto rounded-lg bg-base-200/50 p-3 font-mono text-sm whitespace-pre-wrap break-words"
        >
          {output || (
            <span className="text-base-content/40">等待生成…</span>
          )}
          <div ref={outputEndRef} />
        </div>
      </div>
    </div>
  )
}
