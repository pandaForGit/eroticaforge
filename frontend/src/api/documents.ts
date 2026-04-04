/**
 * 文档：POST/GET /stories/{storyId}/documents（multipart + 可选 metadata JSON 字符串）
 */

import type { DocumentListItemDto, DocumentUploadResponse } from '../types/documents'
import { requestJson } from './client'

function documentsPath(storyId: string): string {
  return `/stories/${encodeURIComponent(storyId)}/documents`
}

export function listDocuments(storyId: string): Promise<DocumentListItemDto[]> {
  return requestJson<DocumentListItemDto[]>(documentsPath(storyId), { method: 'GET' })
}

export function uploadDocument(
  storyId: string,
  file: File,
  metadataJson?: string,
): Promise<DocumentUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  if (metadataJson !== undefined && metadataJson.trim() !== '') {
    form.append('metadata', metadataJson.trim())
  }
  return requestJson<DocumentUploadResponse>(documentsPath(storyId), {
    method: 'POST',
    body: form,
  })
}
