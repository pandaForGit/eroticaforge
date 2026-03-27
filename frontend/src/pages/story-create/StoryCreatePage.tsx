import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { formatApiClientError } from '../../api/client'
import { createStory } from '../../api/stories'
import { useToast } from '../../components/useToast'

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

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      const res = await createStory({
        title: title.trim(),
        tags: parseTags(tagsInput),
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
          标题可留空；标签用中英文逗号分隔。
        </p>
      </div>

      <div className="card bg-base-100 shadow-md">
        <div className="card-body gap-4">
          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
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

            <div className="card-actions justify-end pt-2">
              <Link to="/" className="btn btn-ghost">
                取消
              </Link>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting}
              >
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
