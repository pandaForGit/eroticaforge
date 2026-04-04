import { FormEvent, useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listCharacterLibrary } from '../../api/characterLibrary'
import { formatApiClientError } from '../../api/client'
import { createStory } from '../../api/stories'
import { useToast } from '../../components/useToast'
import type { CharacterLibraryItemDto } from '../../types/characterLibrary'

function parseTags(input: string): string[] | null {
  const parts = input
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
  return parts.length === 0 ? null : parts
}

export function StoryCreatePage() {
  const navigate = useNavigate()
  const { showError, showInfo } = useToast()
  const [title, setTitle] = useState('')
  const [tagsInput, setTagsInput] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [library, setLibrary] = useState<CharacterLibraryItemDto[]>([])
  const [libFilter, setLibFilter] = useState('')
  /** 按勾选顺序记录库卡 ID */
  const [selectedIds, setSelectedIds] = useState<string[]>([])

  const loadLibrary = useCallback(async () => {
    try {
      const list = await listCharacterLibrary({ limit: 400 })
      setLibrary(list)
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }, [showError])

  useEffect(() => {
    void loadLibrary()
  }, [loadLibrary])

  function toggleLibraryId(id: string) {
    setSelectedIds((prev) => {
      const i = prev.indexOf(id)
      if (i >= 0) {
        return prev.filter((x) => x !== id)
      }
      return [...prev, id]
    })
  }

  const filteredLibrary = library.filter((c) => {
    const q = libFilter.trim().toLowerCase()
    if (!q) return true
    return (
      (c.displayName || '').toLowerCase().includes(q) ||
      (c.sourceRelativePath || '').toLowerCase().includes(q)
    )
  })

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      const res = await createStory({
        title: title.trim(),
        tags: parseTags(tagsInput),
        libraryCharacterIds: selectedIds.length > 0 ? selectedIds : null,
      })
      showInfo('已创建故事')
      navigate(`/stories/${res.storyId}`, { replace: true })
    } catch (err) {
      showError(formatApiClientError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <Link to="/" className="link link-hover text-sm">
          ← 返回列表
        </Link>
        <h1 className="mt-2 text-2xl font-bold">新建故事</h1>
        <p className="text-sm text-base-content/70">
          标题可留空；标签用中英文逗号分隔。可选从人物卡库勾选角色（创建为故事内快照，之后可在详情页编辑）。
        </p>
      </div>

      <div className="card bg-base-100 shadow-md">
        <div className="card-body gap-4">
          <form className="flex flex-col gap-4" onSubmit={(e) => void onSubmit(e)}>
            <label className="form-control w-full">
              <span className="label-text font-medium">标题</span>
              <input
                type="text"
                className="input input-bordered w-full"
                placeholder="例如：某条故事线"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={512}
              />
            </label>

            <label className="form-control w-full">
              <span className="label-text font-medium">标签</span>
              <input
                type="text"
                className="input input-bordered w-full"
                placeholder="NTR, 调教, 黑丝"
                value={tagsInput}
                onChange={(e) => setTagsInput(e.target.value)}
              />
            </label>

            <div className="form-control w-full">
              <span className="label-text font-medium">人物卡库（多选，顺序影响 Prompt 排序）</span>
              <input
                type="search"
                className="input input-bordered input-sm mb-2 w-full"
                placeholder="筛选展示名或路径…"
                value={libFilter}
                onChange={(e) => setLibFilter(e.target.value)}
              />
              <div className="max-h-56 overflow-y-auto rounded-lg border border-base-300 bg-base-200/40 p-2">
                {filteredLibrary.length === 0 ? (
                  <p className="text-sm text-base-content/50">无匹配项或库为空（可先导入 JSONL 并重启）。</p>
                ) : (
                  <ul className="space-y-1">
                    {filteredLibrary.map((c) => (
                      <li key={c.id}>
                        <label className="flex cursor-pointer items-start gap-2 rounded px-1 py-0.5 hover:bg-base-200">
                          <input
                            type="checkbox"
                            className="checkbox checkbox-sm mt-0.5"
                            checked={selectedIds.includes(c.id)}
                            onChange={() => toggleLibraryId(c.id)}
                          />
                          <span className="min-w-0 text-sm">
                            <span className="font-medium">{c.displayName || '未命名'}</span>
                            <span className="block truncate text-xs text-base-content/50">
                              {c.sourceRelativePath}
                            </span>
                          </span>
                        </label>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <span className="label-text-alt mt-1">已选 {selectedIds.length} 个</span>
            </div>

            <div className="card-actions justify-end pt-2">
              <Link to="/" className="btn btn-ghost">
                取消
              </Link>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? (
                  <>
                    <span className="loading loading-spinner loading-sm" />
                    创建中
                  </>
                ) : (
                  '创建'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
