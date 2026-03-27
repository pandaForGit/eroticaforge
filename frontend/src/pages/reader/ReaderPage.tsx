import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { getChapter, listChapters } from '../../api/chapters'
import { getStory } from '../../api/stories'
import { useToast } from '../../components/useToast'
import { formatDateTime } from '../../lib/formatDate'
import type { ChapterDetailDto, ChapterSummaryDto } from '../../types/chapters'
import type { StoryDetailDto } from '../../types/stories'

export function ReaderPage() {
  const { storyId = '' } = useParams<{ storyId: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const chapterParam = searchParams.get('chapter') ?? ''
  const { showError } = useToast()

  const [story, setStory] = useState<StoryDetailDto | null>(null)
  const [chapters, setChapters] = useState<ChapterSummaryDto[] | null>(null)
  const [detail, setDetail] = useState<ChapterDetailDto | null>(null)
  const [loadingMeta, setLoadingMeta] = useState(true)
  const [loadingChapter, setLoadingChapter] = useState(false)

  const selectChapter = useCallback(
    (chapterId: string) => {
      const next = new URLSearchParams(searchParams)
      if (chapterId) {
        next.set('chapter', chapterId)
      } else {
        next.delete('chapter')
      }
      setSearchParams(next, { replace: true })
    },
    [searchParams, setSearchParams],
  )

  const refreshList = useCallback(async () => {
    if (!storyId) return
    try {
      const list = await listChapters(storyId)
      setChapters(list)
    } catch (e) {
      setChapters(null)
      showError(formatApiClientError(e))
    }
  }, [storyId, showError])

  useEffect(() => {
    if (!storyId) return
    let cancelled = false
    setLoadingMeta(true)
    void Promise.all([getStory(storyId), listChapters(storyId)])
      .then(([s, list]) => {
        if (!cancelled) {
          setStory(s)
          setChapters(list)
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setStory(null)
          setChapters(null)
          showError(formatApiClientError(e))
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingMeta(false)
      })
    return () => {
      cancelled = true
    }
  }, [storyId, showError])

  useEffect(() => {
    if (!storyId || !chapterParam) {
      setDetail(null)
      return
    }
    let cancelled = false
    setLoadingChapter(true)
    void getChapter(storyId, chapterParam)
      .then((d) => {
        if (!cancelled) setDetail(d)
      })
      .catch((e) => {
        if (!cancelled) {
          setDetail(null)
          showError(formatApiClientError(e))
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingChapter(false)
      })
    return () => {
      cancelled = true
    }
  }, [storyId, chapterParam, showError])

  if (!storyId) {
    return (
      <div className="alert alert-warning">
        缺少故事 ID。<Link to="/">返回列表</Link>
      </div>
    )
  }

  if (loadingMeta) {
    return (
      <div className="flex justify-center py-20">
        <span className="loading loading-spinner loading-lg text-primary" />
      </div>
    )
  }

  if (!story || chapters === null) {
    return (
      <div className="space-y-4">
        <Link to="/" className="link link-hover text-sm">
          ← 返回列表
        </Link>
        <div className="alert alert-error">无法加载故事或章节列表。</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link
            to={`/stories/${encodeURIComponent(storyId)}`}
            className="link link-hover text-sm"
          >
            ← 故事详情
          </Link>
          <h1 className="mt-2 text-2xl font-bold">章节阅读</h1>
          <p className="mt-1 text-sm text-base-content/70">{story.title}</p>
        </div>
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => void refreshList()}
        >
          刷新章节列表
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,14rem)_1fr]">
        <aside className="rounded-box border border-base-300 bg-base-100 p-3 shadow-sm lg:max-h-[min(70vh,32rem)] lg:overflow-y-auto">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-base-content/60">
            章节
          </h2>
          {chapters.length === 0 ? (
            <p className="mt-3 text-sm text-base-content/50">暂无章节，请先去生成。</p>
          ) : (
            <ul className="menu menu-sm mt-2 rounded-lg bg-base-200/40 p-0">
              {chapters.map((c) => (
                <li key={c.chapterId}>
                  <button
                    type="button"
                    className={
                      c.chapterId === chapterParam ? 'active font-medium' : ''
                    }
                    onClick={() => selectChapter(c.chapterId)}
                  >
                    <span className="tabular-nums text-base-content/60">
                      #{c.seq}
                    </span>
                    <span className="truncate">
                      {c.title?.trim() ? c.title : '（无标题）'}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </aside>

        <article className="min-h-[12rem] rounded-box border border-base-300 bg-base-100 p-6 shadow-sm">
          {!chapterParam ? (
            <p className="text-sm text-base-content/50">请从左侧选择一章。</p>
          ) : loadingChapter ? (
            <div className="flex justify-center py-16">
              <span className="loading loading-spinner loading-lg text-primary" />
            </div>
          ) : !detail ? (
            <div className="alert alert-warning text-sm">该章节无法加载。</div>
          ) : (
            <>
              <header className="border-b border-base-300 pb-4">
                <h2 className="text-xl font-semibold">
                  {detail.title?.trim() ? detail.title : `第 ${detail.seq} 章`}
                </h2>
                <p className="mt-1 font-mono text-xs text-base-content/50">
                  {detail.chapterId} · {formatDateTime(detail.createdAt)}
                </p>
              </header>
              <div className="prose prose-sm max-w-none py-6 dark:prose-invert">
                <div className="whitespace-pre-wrap break-words font-sans text-base leading-relaxed">
                  {detail.content}
                </div>
              </div>
            </>
          )}
        </article>
      </div>
    </div>
  )
}
