import { FormEvent, useCallback, useEffect, useState } from 'react'
import { formatApiClientError } from '../../../api/client'
import { listCharacterLibrary } from '../../../api/characterLibrary'
import {
  createStoryCharacterSnapshot,
  deleteStoryCharacterSnapshot,
  listStoryCharacterSnapshots,
  patchStoryCharacterSnapshot,
  reorderStoryCharacterSnapshots,
} from '../../../api/storyCharacterSnapshots'
import { useToast } from '../../../components/useToast'
import type { CharacterLibraryItemDto } from '../../../types/characterLibrary'
import type { StoryCharacterSnapshotDto } from '../../../types/storySnapshots'

function snapshotTitle(s: StoryCharacterSnapshotDto): string {
  const n = s.payload?.name
  return typeof n === 'string' && n.trim() ? n.trim() : '未命名'
}

export function StoryCharacterSnapshotsPanel({ storyId }: { storyId: string }) {
  const { showError, showInfo } = useToast()
  const [rows, setRows] = useState<StoryCharacterSnapshotDto[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [library, setLibrary] = useState<CharacterLibraryItemDto[]>([])
  const [pickLibraryId, setPickLibraryId] = useState('')
  const [editing, setEditing] = useState<StoryCharacterSnapshotDto | null>(null)
  const [editJson, setEditJson] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const list = await listStoryCharacterSnapshots(storyId)
      setRows(list)
    } catch (e) {
      setRows(null)
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [storyId, showError])

  const loadLibrary = useCallback(async () => {
    try {
      const list = await listCharacterLibrary({ limit: 300 })
      setLibrary(list)
      setPickLibraryId((cur) => (list.length && !cur ? list[0].id : cur))
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }, [showError])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    void loadLibrary()
  }, [loadLibrary])

  async function onAddFromLibrary() {
    if (!pickLibraryId) {
      showError('请选择库中人物')
      return
    }
    try {
      await createStoryCharacterSnapshot(storyId, { libraryCharacterId: pickLibraryId })
      showInfo('已从库克隆快照')
      void refresh()
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }

  async function onAddEmpty() {
    try {
      await createStoryCharacterSnapshot(storyId, { payload: {} })
      showInfo('已新增空白快照')
      void refresh()
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }

  async function onDelete(id: string) {
    if (!window.confirm('删除该人物快照？仅影响后续生成。')) return
    try {
      await deleteStoryCharacterSnapshot(storyId, id)
      showInfo('已删除')
      void refresh()
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }

  async function move(index: number, dir: -1 | 1) {
    if (!rows || rows.length < 2) return
    const j = index + dir
    if (j < 0 || j >= rows.length) return
    const ordered = [...rows]
    const t = ordered[index]
    ordered[index] = ordered[j]
    ordered[j] = t
    try {
      await reorderStoryCharacterSnapshots(storyId, {
        snapshotIds: ordered.map((r) => r.id),
      })
      showInfo('已调整顺序')
      void refresh()
    } catch (e) {
      showError(formatApiClientError(e))
    }
  }

  function openEdit(s: StoryCharacterSnapshotDto) {
    setEditing(s)
    try {
      setEditJson(JSON.stringify(s.payload ?? {}, null, 2))
    } catch {
      setEditJson('{}')
    }
  }

  async function onSaveEdit(e: FormEvent) {
    e.preventDefault()
    if (!editing) return
    let payload: Record<string, unknown>
    try {
      payload = JSON.parse(editJson) as Record<string, unknown>
      if (typeof payload !== 'object' || payload === null || Array.isArray(payload)) {
        showError('payload 须为 JSON 对象')
        return
      }
    } catch {
      showError('JSON 不合法')
      return
    }
    try {
      await patchStoryCharacterSnapshot(storyId, editing.id, { payload })
      showInfo('已保存')
      setEditing(null)
      void refresh()
    } catch (err) {
      showError(formatApiClientError(err))
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold">人物快照</h2>
          <p className="text-xs text-base-content/60">
            修改仅影响后续生成。可从库克隆、手写空卡或编辑 JSON。
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            className="select select-bordered select-sm max-w-xs"
            value={pickLibraryId}
            onChange={(e) => setPickLibraryId(e.target.value)}
          >
            {library.map((c) => (
              <option key={c.id} value={c.id}>
                {(c.displayName || '未命名').slice(0, 24)}
                {c.sourceRelativePath ? ` — ${c.sourceRelativePath.slice(-40)}` : ''}
              </option>
            ))}
          </select>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => void onAddFromLibrary()}>
            从库添加
          </button>
          <button type="button" className="btn btn-outline btn-sm" onClick={() => void onAddEmpty()}>
            空白快照
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-6">
          <span className="loading loading-spinner loading-md" />
        </div>
      ) : !rows || rows.length === 0 ? (
        <p className="text-sm text-base-content/60">暂无人物快照。</p>
      ) : (
        <ul className="space-y-2">
          {rows.map((s, i) => (
            <li
              key={s.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-base-300 bg-base-100 px-3 py-2"
            >
              <div className="min-w-0 flex-1">
                <div className="font-medium">{snapshotTitle(s)}</div>
                <div className="truncate font-mono text-xs text-base-content/50">{s.id}</div>
              </div>
              <div className="flex flex-wrap gap-1">
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  disabled={i === 0}
                  onClick={() => void move(i, -1)}
                >
                  上移
                </button>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  disabled={i === rows.length - 1}
                  onClick={() => void move(i, 1)}
                >
                  下移
                </button>
                <button type="button" className="btn btn-ghost btn-xs" onClick={() => openEdit(s)}>
                  编辑 JSON
                </button>
                <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => void onDelete(s.id)}>
                  删除
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing ? (
        <dialog className="modal modal-open">
          <div className="modal-box max-w-3xl">
            <h3 className="text-lg font-bold">编辑快照 payload</h3>
            <form className="mt-4 flex flex-col gap-3" onSubmit={(e) => void onSaveEdit(e)}>
              <textarea
                className="textarea textarea-bordered font-mono text-sm min-h-[16rem] w-full"
                value={editJson}
                onChange={(e) => setEditJson(e.target.value)}
                spellCheck={false}
              />
              <div className="modal-action">
                <button type="button" className="btn" onClick={() => setEditing(null)}>
                  取消
                </button>
                <button type="submit" className="btn btn-primary">
                  保存
                </button>
              </div>
            </form>
          </div>
          <button
            type="button"
            className="modal-backdrop bg-black/50"
            aria-label="关闭"
            onClick={() => setEditing(null)}
          />
        </dialog>
      ) : null}
    </section>
  )
}
