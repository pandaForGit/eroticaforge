import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { listStories } from '../../api/stories'
import { useToast } from '../../components/useToast'
import { formatDateTime } from '../../lib/formatDate'
import type { StoryListItemDto } from '../../types/stories'

export function StoryListPage() {
  const { showError } = useToast()
  const [items, setItems] = useState<StoryListItemDto[] | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const list = await listStories()
      setItems(list)
    } catch (e) {
      setItems(null)
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [showError])

  useEffect(() => {
    void load()
  }, [load])

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">故事</h1>
          <p className="text-sm text-base-content/70">
            标题、标签、章节数与更新时间；点击进入详情或删除。
          </p>
        </div>
        <Link to="/stories/new" className="btn btn-primary">
          新建故事
        </Link>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <span className="loading loading-spinner loading-lg text-primary" />
        </div>
      ) : items === null ? (
        <div className="alert alert-error shadow-lg">
          <span>列表加载失败（已提示错误）。可刷新页面重试。</span>
          <button type="button" className="btn btn-sm" onClick={() => void load()}>
            重试
          </button>
        </div>
      ) : items.length === 0 ? (
        <div className="alert alert-info shadow-lg">
          <span>还没有故事。</span>
          <Link to="/stories/new" className="btn btn-sm">
            创建第一个
          </Link>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-base-300 bg-base-100 shadow-sm">
          <table className="table table-zebra">
            <thead>
              <tr>
                <th>标题</th>
                <th>标签</th>
                <th className="w-24 text-center">章节</th>
                <th className="w-52">更新时间</th>
                <th className="w-28 text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map((row) => (
                <tr key={row.storyId} className="hover">
                  <td className="font-medium">{row.title || '（无标题）'}</td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      {(row.tags ?? []).length === 0 ? (
                        <span className="text-base-content/50">—</span>
                      ) : (
                        row.tags!.map((t) => (
                          <span key={t} className="badge badge-outline badge-sm">
                            {t}
                          </span>
                        ))
                      )}
                    </div>
                  </td>
                  <td className="text-center tabular-nums">{row.totalChapters}</td>
                  <td className="text-sm text-base-content/80">
                    {formatDateTime(row.updatedAt)}
                  </td>
                  <td className="text-right">
                    <Link
                      to={`/stories/${row.storyId}`}
                      className="btn btn-ghost btn-xs"
                    >
                      详情
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
