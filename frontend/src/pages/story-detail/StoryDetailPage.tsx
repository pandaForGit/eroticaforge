import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { deleteStory, getStory } from '../../api/stories'
import { useToast } from '../../components/useToast'
import { formatDateTime } from '../../lib/formatDate'
import type { StoryDetailDto } from '../../types/stories'
import { StoryDocumentsPanel } from './documents/StoryDocumentsPanel'
import { StoryCharacterSnapshotsPanel } from './snapshots/StoryCharacterSnapshotsPanel'

export function StoryDetailPage() {
  const { storyId = '' } = useParams<{ storyId: string }>()
  const navigate = useNavigate()
  const { showError, showInfo } = useToast()
  const [story, setStory] = useState<StoryDetailDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(false)
  const deleteDialogRef = useRef<HTMLDialogElement>(null)

  const load = useCallback(async () => {
    if (!storyId) return
    setLoading(true)
    try {
      const s = await getStory(storyId)
      setStory(s)
    } catch (e) {
      setStory(null)
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [storyId, showError])

  useEffect(() => {
    void load()
  }, [load])

  async function confirmDelete() {
    if (!storyId) return
    setDeleting(true)
    try {
      await deleteStory(storyId)
      showInfo('已删除故事')
      deleteDialogRef.current?.close()
      navigate('/', { replace: true })
    } catch (e) {
      showError(formatApiClientError(e))
    } finally {
      setDeleting(false)
    }
  }

  if (!storyId) {
    return (
      <div className="alert alert-warning">
        缺少故事 ID。<Link to="/">返回列表</Link>
      </div>
    )
  }

  if (loading) {
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
        <div className="alert alert-error">无法加载该故事。</div>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link to="/" className="link link-hover text-sm">
            ← 返回列表
          </Link>
          <h1 className="mt-2 text-2xl font-bold">
            {story.title || '（无标题）'}
          </h1>
          <p className="mt-1 font-mono text-xs text-base-content/60">
            {story.storyId}
          </p>
          <div className="mt-3 flex flex-wrap gap-1">
            {(story.tags ?? []).length === 0 ? (
              <span className="text-sm text-base-content/50">无标签</span>
            ) : (
              story.tags!.map((t) => (
                <span key={t} className="badge badge-primary badge-outline">
                  {t}
                </span>
              ))
            )}
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to={`/stories/${encodeURIComponent(story.storyId)}/generate`}
            className="btn btn-primary btn-sm"
          >
            生成
          </Link>
          <Link
            to={`/stories/${encodeURIComponent(story.storyId)}/read`}
            className="btn btn-secondary btn-sm"
          >
            阅读
          </Link>
          <Link
            to={`/stories/${encodeURIComponent(story.storyId)}/state`}
            className="btn btn-ghost btn-sm"
          >
            状态调试
          </Link>
          <button
            type="button"
            className="btn btn-outline btn-error btn-sm"
            onClick={() => deleteDialogRef.current?.showModal()}
          >
            删除故事
          </button>
        </div>
      </div>

      <div className="stats stats-vertical bg-base-100 shadow sm:stats-horizontal">
        <div className="stat place-items-center border-base-300 sm:border-e">
          <div className="stat-title">章节数</div>
          <div className="stat-value text-primary tabular-nums">
            {story.totalChapters}
          </div>
        </div>
        <div className="stat place-items-center border-base-300 sm:border-e">
          <div className="stat-title">下一章序号</div>
          <div className="stat-value text-secondary tabular-nums text-2xl">
            {story.nextChapterSeq}
          </div>
        </div>
        <div className="stat place-items-center">
          <div className="stat-title">主模型</div>
          <div className="stat-value break-all text-lg font-normal">
            {story.mainModel}
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-base-content/70">创建时间</h2>
          <p className="mt-1">{formatDateTime(story.createdAt)}</p>
        </div>
        <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-base-content/70">更新时间</h2>
          <p className="mt-1">{formatDateTime(story.updatedAt)}</p>
        </div>
      </div>

      <StoryCharacterSnapshotsPanel storyId={story.storyId} />

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">文档</h2>
        <StoryDocumentsPanel storyId={story.storyId} />
      </section>

      <dialog ref={deleteDialogRef} className="modal">
        <div className="modal-box">
          <h3 className="text-lg font-bold">删除故事</h3>
          <p className="py-4 text-sm leading-relaxed">
            将永久删除该故事。数据库若配置为级联，会一并清理关联章节与文档记录。此操作不可恢复。
          </p>
          <div className="modal-action">
            <button
              type="button"
              className="btn"
              onClick={() => deleteDialogRef.current?.close()}
            >
              取消
            </button>
            <button
              type="button"
              className="btn btn-error"
              disabled={deleting}
              onClick={() => void confirmDelete()}
            >
              {deleting ? (
                <>
                  <span className="loading loading-spinner loading-sm" />
                  删除中
                </>
              ) : (
                '确认删除'
              )}
            </button>
          </div>
        </div>
        <form method="dialog" className="modal-backdrop">
          <button type="submit">关闭</button>
        </form>
      </dialog>
    </div>
  )
}
