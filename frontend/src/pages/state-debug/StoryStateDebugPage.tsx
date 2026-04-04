import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiClientError, formatApiClientError } from '../../api/client'
import { getStoryState, putStoryState } from '../../api/state'
import { useToast } from '../../components/useToast'
import type { StoryStateResponse, UpdateStoryStateRequest } from '../../types/storyState'

function safeJsonParseObject(
  text: string,
): { ok: true; value: Record<string, string> } | { ok: false; error: string } {
  const t = text.trim()
  if (!t) return { ok: true, value: {} }
  try {
    const v = JSON.parse(t) as unknown
    if (typeof v !== 'object' || v === null || Array.isArray(v)) {
      return { ok: false, error: '须为 JSON 对象，例如 {"角色A":"状态"}' }
    }
    const out: Record<string, string> = {}
    for (const [k, val] of Object.entries(v)) {
      if (typeof val !== 'string') {
        return { ok: false, error: `键「${k}」的值须为字符串` }
      }
      out[k] = val
    }
    return { ok: true, value: out }
  } catch {
    return { ok: false, error: 'JSON 解析失败' }
  }
}

function safeJsonParseStringArray(
  text: string,
): { ok: true; value: string[] } | { ok: false; error: string } {
  const t = text.trim()
  if (!t) return { ok: true, value: [] }
  try {
    const v = JSON.parse(t) as unknown
    if (!Array.isArray(v)) {
      return { ok: false, error: '须为 JSON 数组，例如 ["事实1","事实2"]' }
    }
    for (let i = 0; i < v.length; i++) {
      if (typeof v[i] !== 'string') {
        return { ok: false, error: `第 ${i + 1} 项须为字符串` }
      }
    }
    return { ok: true, value: v as string[] }
  } catch {
    return { ok: false, error: 'JSON 解析失败' }
  }
}

export function StoryStateDebugPage() {
  const { storyId = '' } = useParams<{ storyId: string }>()
  const { showError, showInfo } = useToast()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [version, setVersion] = useState(0)
  const [summary, setSummary] = useState('')
  const [lastEnding, setLastEnding] = useState('')
  const [charsJson, setCharsJson] = useState('{}')
  const [factsJson, setFactsJson] = useState('[]')
  const [flagsJson, setFlagsJson] = useState('[]')
  const [updatedAt, setUpdatedAt] = useState('')

  const applyState = useCallback((s: StoryStateResponse) => {
    setVersion(s.version)
    setSummary(s.currentSummary ?? '')
    setLastEnding(s.lastChapterEnding ?? '')
    setCharsJson(JSON.stringify(s.characterStates ?? {}, null, 2))
    setFactsJson(JSON.stringify(s.importantFacts ?? [], null, 2))
    setFlagsJson(JSON.stringify(s.worldFlags ?? [], null, 2))
    setUpdatedAt(s.updatedAt)
  }, [])

  const load = useCallback(async () => {
    if (!storyId) return
    setLoading(true)
    try {
      const s = await getStoryState(storyId)
      applyState(s)
    } catch (e) {
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [storyId, applyState, showError])

  useEffect(() => {
    void load()
  }, [load])

  async function save() {
    if (!storyId) return
    const co = safeJsonParseObject(charsJson)
    if (!co.ok) {
      showError(`人物状态：${co.error}`)
      return
    }
    const fa = safeJsonParseStringArray(factsJson)
    if (!fa.ok) {
      showError(`重要事实：${fa.error}`)
      return
    }
    const fl = safeJsonParseStringArray(flagsJson)
    if (!fl.ok) {
      showError(`世界标记：${fl.error}`)
      return
    }

    const body: UpdateStoryStateRequest = {
      version,
      currentSummary: summary,
      lastChapterEnding: lastEnding,
      characterStates: co.value,
      importantFacts: fa.value,
      worldFlags: fl.value,
    }

    setSaving(true)
    try {
      const next = await putStoryState(storyId, body)
      applyState(next)
      showInfo('已保存状态')
    } catch (e) {
      if (e instanceof ApiClientError && e.httpStatus === 409) {
        showError(
          `版本冲突（409）：服务端状态已变，请点「重新加载」后再编辑。详情：${e.detail}`,
        )
      } else {
        showError(formatApiClientError(e))
      }
    } finally {
      setSaving(false)
    }
  }

  if (!storyId) {
    return (
      <div className="alert alert-warning">
        缺少故事 ID。<Link to="/">返回列表</Link>
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
        <h1 className="mt-2 text-2xl font-bold">StoryState 调试</h1>
        <p className="mt-1 text-xs text-base-content/60">
          PUT 会整表覆盖人物状态 / 事实 / 标记（与当前后端语义一致）；version 必须与服务器一致。
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          className="btn btn-outline btn-sm"
          disabled={loading}
          onClick={() => void load()}
        >
          {loading ? (
            <span className="loading loading-spinner loading-xs" />
          ) : null}
          重新加载
        </button>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          disabled={loading || saving}
          onClick={() => void save()}
        >
          {saving ? (
            <span className="loading loading-spinner loading-xs" />
          ) : null}
          保存 PUT
        </button>
      </div>

      {loading && !updatedAt ? (
        <div className="flex justify-center py-16">
          <span className="loading loading-spinner loading-lg text-primary" />
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-4 text-sm">
            <span>
              <span className="text-base-content/60">version</span>{' '}
              <span className="font-mono font-semibold tabular-nums">{version}</span>
            </span>
            {updatedAt && (
              <span className="text-base-content/60">
                updatedAt:{' '}
                <span className="font-mono text-xs">{updatedAt}</span>
              </span>
            )}
          </div>

          <label className="form-control">
            <span className="label-text text-sm">currentSummary</span>
            <textarea
              className="textarea textarea-bordered min-h-24 w-full text-sm"
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
            />
          </label>

          <label className="form-control">
            <span className="label-text text-sm">lastChapterEnding</span>
            <textarea
              className="textarea textarea-bordered min-h-20 w-full text-sm"
              value={lastEnding}
              onChange={(e) => setLastEnding(e.target.value)}
            />
          </label>

          <label className="form-control">
            <span className="label-text text-sm font-mono">characterStates（JSON 对象）</span>
            <textarea
              className="textarea textarea-bordered min-h-32 w-full font-mono text-xs"
              value={charsJson}
              onChange={(e) => setCharsJson(e.target.value)}
            />
          </label>

          <label className="form-control">
            <span className="label-text text-sm font-mono">importantFacts（JSON 字符串数组）</span>
            <textarea
              className="textarea textarea-bordered min-h-28 w-full font-mono text-xs"
              value={factsJson}
              onChange={(e) => setFactsJson(e.target.value)}
            />
          </label>

          <label className="form-control">
            <span className="label-text text-sm font-mono">worldFlags（JSON 字符串数组）</span>
            <textarea
              className="textarea textarea-bordered min-h-24 w-full font-mono text-xs"
              value={flagsJson}
              onChange={(e) => setFlagsJson(e.target.value)}
            />
          </label>
        </div>
      )}
    </div>
  )
}
