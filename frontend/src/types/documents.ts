/** 与后端 DocumentUploadResponse / DocumentListItemDto 的 JSON 对齐 */

export interface DocumentUploadResponse {
  docId: string
  fileName: string
  chunkCount: number
  status: string
}

export interface DocumentListItemDto {
  docId: string
  fileName: string
  chunkCount: number
  createdAt: string
  metadata: Record<string, unknown> | null
}
