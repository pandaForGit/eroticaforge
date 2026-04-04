import { useCallback, useEffect, useState } from 'react'
import { ApiClientError, formatApiClientError } from '../../../api/client'
import { listDocuments, uploadDocument } from '../../../api/documents'
import { useToast } from '../../../components/useToast'
import { formatDateTime } from '../../../lib/formatDate'
import type { DocumentListItemDto } from '../../../types/documents'

const MAX_HINT_MB = 10

export function StoryDocumentsPanel({ storyId }: { storyId: string }) {
  const { showError, showInfo } = useToast()
  const [rows, setRows] = useState<DocumentListItemDto[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [metadataDraft, setMetadataDraft] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const list = await listDocuments(storyId)
      setRows(list)
    } catch (e) {
      setRows(null)
      showError(formatApiClientError(e))
    } finally {
      setLoading(false)
    }
  }, [storyId, showError])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function onUpload(form: HTMLFormElement) {
    const input = form.elements.namedItem('file') as HTMLInputElement | null
    const file = input?.files?.[0]
    if (!file) {
      showError('请选择要上传的文件')
      return
    }
    const lower = file.name.toLowerCase()
    if (!lower.endsWith('.txt')) {
      showError('当前版本仅支持 UTF-8 文本（.txt）')
      return
    }
    let metadataToSend: string | undefined
    const trimmed = metadataDraft.trim()
    if (trimmed) {
      try {
        JSON.parse(trimmed)
        metadataToSend = trimmed
      } catch {
        showError('metadata 须为合法 JSON 对象，例如 {"type":"character_card"}')
        return
      }
    }

    setUploading(true)
    try {
      const res = await uploadDocument(storyId, file, metadataToSend)
      showInfo(`已上传「${res.fileName}」，切块数 ${res.chunkCount}`)
      form.reset()
      setMetadataDraft('')
      await refresh()
    } catch (e) {
      if (e instanceof ApiClientError && e.httpStatus === 413) {
        showError(
          `${formatApiClientError(e)}（后端限制约 ${MAX_HINT_MB}MB，请缩小文件后重试）`,
        )
      } else {
        showError(formatApiClientError(e))
      }
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-box border border-base-300 bg-base-100 p-4 shadow-sm">
        <h3 className="text-sm font-semibold text-base-content/80">上传文档</h3>
        <p className="mt-1 text-xs text-base-content/60">
          仅支持 <code className="rounded bg-base-200 px-1">.txt</code>（UTF-8）；单文件建议不超过{' '}
          {MAX_HINT_MB}MB（与后端{' '}
          <code className="rounded bg-base-200 px-1">spring.servlet.multipart.max-file-size</code>{' '}
          一致）。可选 <code className="rounded bg-base-200 px-1">metadata</code> 为 JSON 字符串。
        </p>

        <form
          className="mt-4 flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            void onUpload(e.currentTarget)
          }}
        >
          <input
            type="file"
            name="file"
            accept=".txt,text/plain"
            className="file-input file-input-bordered file-input-sm w-full max-w-md"
          />

          <label className="form-control w-full max-w-xl">
            <span className="label-text text-xs text-base-content/70">
              metadata（可选，JSON）
            </span>
            <textarea
              className="textarea textarea-bordered textarea-sm font-mono text-xs"
              rows={3}
              placeholder='{"type":"character_card","characterName":"示例"}'
              value={metadataDraft}
              onChange={(e) => setMetadataDraft(e.target.value)}
            />
          </label>

          <div>
            <button
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={uploading}
            >
              {uploading ? (
                <>
                  <span className="loading loading-spinner loading-xs" />
                  上传中…
                </>
              ) : (
                '上传并索引'
              )}
            </button>
          </div>
        </form>
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-base-content/80">已上传</h3>
          <button
            type="button"
            className="btn btn-ghost btn-xs"
            disabled={loading}
            onClick={() => void refresh()}
          >
            刷新列表
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-10">
            <span className="loading loading-spinner loading-md text-primary" />
          </div>
        ) : rows === null ? (
          <div className="alert alert-error text-sm">文档列表加载失败。</div>
        ) : rows.length === 0 ? (
          <div className="rounded-box border border-dashed border-base-300 bg-base-200/30 p-6 text-center text-sm text-base-content/60">
            暂无文档，请上传 .txt。
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-base-300 bg-base-100 shadow-sm">
            <table className="table table-sm table-zebra">
              <thead>
                <tr>
                  <th>文件名</th>
                  <th className="w-24 text-center">切块数</th>
                  <th className="w-52">上传时间</th>
                  <th>docId</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.docId}>
                    <td className="font-medium">{row.fileName}</td>
                    <td className="text-center tabular-nums">{row.chunkCount}</td>
                    <td className="text-xs text-base-content/80">
                      {formatDateTime(row.createdAt)}
                    </td>
                    <td className="font-mono text-xs text-base-content/70">
                      {row.docId}
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
