import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { fetchHealth } from '../../api/health'
import type { HealthResponse } from '../../types/health'

function statusBadge(label: string, value: string) {
  const ok = value === 'up'
  return (
    <div className="flex items-center justify-between gap-4 border-b border-base-300 py-3 last:border-0">
      <span className="text-sm font-medium text-base-content/80">{label}</span>
      <span
        className={`badge badge-sm ${ok ? 'badge-success' : 'badge-error'} font-mono`}
      >
        {value}
      </span>
    </div>
  )
}

export function HealthPage() {
  const [data, setData] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const h = await fetchHealth()
      setData(h)
    } catch (e) {
      setData(null)
      setError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  return (
    <div className="space-y-6">
      <div>
        <Link to="/" className="link link-hover text-sm">
          ← 故事列表
        </Link>
        <h1 className="mt-2 text-2xl font-bold">服务状态</h1>
        <p className="mt-1 text-sm text-base-content/60">
          聚合检查本机 PostgreSQL、对话 LLM（/v1/models）、嵌入服务（独立 base-url 的 /v1/models）。
        </p>
      </div>

      <div className="flex gap-2">
        <button
          type="button"
          className="btn btn-outline btn-sm"
          disabled={loading}
          onClick={() => void load()}
        >
          {loading ? (
            <span className="loading loading-spinner loading-xs" />
          ) : null}
          重新检测
        </button>
      </div>

      {loading && !data && !error ? (
        <div className="flex justify-center py-16">
          <span className="loading loading-spinner loading-lg text-primary" />
        </div>
      ) : error ? (
        <div className="alert alert-error text-sm">{error}</div>
      ) : data ? (
        <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm">
          {statusBadge('总览 status', data.status)}
          {statusBadge('database', data.database)}
          {statusBadge('llm', data.llm)}
          {statusBadge('embedding', data.embedding)}
        </div>
      ) : null}

      <div className="rounded-box border border-base-200 bg-base-200/40 p-4 text-sm text-base-content/70">
        <p className="font-medium text-base-content">环境异常时</p>
        <p className="mt-2">
          请对照仓库内部署说明排查依赖与本机端口：
          <code className="mx-1 rounded bg-base-300 px-1.5 py-0.5 text-xs">
            docs/deployment/安装部署指南.md
          </code>
          （若文档路径不同，请以仓库 README 为准。）
        </p>
      </div>
    </div>
  )
}
