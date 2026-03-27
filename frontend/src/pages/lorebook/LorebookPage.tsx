import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { createLorebookEntry, listLorebook } from '../../api/lorebook'
import { useToast } from '../../components/useToast'
import { formatDateTime } from '../../lib/formatDate'
import type { LorebookItemDto } from '../../types/lorebook'

export function LorebookPage() {
  const { showError, showInfo } = useToast()
  const [rows, setRows] = useState<LorebookItemDto[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const list = await listLorebook()
      setRows(list)
    } catch (e) {
      setRows(null)
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [showError])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const k = keyword.trim()
    const b = body.trim()
    if (!k) {
      showError('请填写关键词')
      return
    }
    if (!b) {
      showError('请填写注入正文')
      return
    }
    setSubmitting(true)
    try {
      await createLorebookEntry({ keyword: k, body: b })
      showInfo('已新增 Lorebook 条目')
      setKeyword('')
      setBody('')
      await refresh()
    } catch (err) {
      showError(formatApiClientError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-8">
      <div>
        <Link to="/" className="link link-hover text-sm">
          ← 故事列表
        </Link>
        <h1 className="mt-2 text-2xl font-bold">Lorebook</h1>
        <p className="mt-1 text-sm text-base-content/60">
          当前后端为内存实现，进程重启后清空；关键词命中逻辑由生成侧实现。
        </p>
      </div>

      <form
        onSubmit={onSubmit}
        className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm space-y-4"
      >
        <h2 className="text-sm font-semibold text-base-content/80">新增条目</h2>
        <label className="form-control">
          <span className="label-text text-xs">keyword</span>
          <input
            type="text"
            className="input input-bordered input-sm w-full max-w-md"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="触发词"
          />
        </label>
        <label className="form-control">
          <span className="label-text text-xs">body</span>
          <textarea
            className="textarea textarea-bordered min-h-28 w-full text-sm"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="命中时注入的设定/描写"
          />
        </label>
        <button
          type="submit"
          className="btn btn-primary btn-sm"
          disabled={submitting}
        >
          {submitting ? (
            <span className="loading loading-spinner loading-xs" />
          ) : null}
          提交
        </button>
      </form>

      <div>
        <div className="mb-2 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-base-content/80">全部条目</h2>
          <button
            type="button"
            className="btn btn-ghost btn-xs"
            disabled={loading}
            onClick={() => void refresh()}
          >
            刷新
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <span className="loading loading-spinner loading-md text-primary" />
          </div>
        ) : rows === null ? (
          <div className="alert alert-error text-sm">列表加载失败。</div>
        ) : rows.length === 0 ? (
          <div className="rounded-box border border-dashed border-base-300 bg-base-200/30 p-8 text-center text-sm text-base-content/50">
            暂无条目。
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-base-300 bg-base-100 shadow-sm">
            <table className="table table-sm">
              <thead>
                <tr>
                  <th className="w-24">id</th>
                  <th className="w-40">keyword</th>
                  <th>body</th>
                  <th className="w-44">createdAt</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td className="font-mono text-xs tabular-nums">{r.id}</td>
                    <td className="font-medium">{r.keyword}</td>
                    <td className="max-w-md whitespace-pre-wrap break-words text-sm">
                      {r.body}
                    </td>
                    <td className="text-xs text-base-content/70">
                      {formatDateTime(r.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
