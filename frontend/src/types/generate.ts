export interface GenerateRequest {
  prompt: string
  maxTokens?: number | null
  useMultiModel?: boolean | null
}

export interface GenerateSyncResponse {
  text: string
  chapterId: string
  /** RAG 失败降级时后端返回的说明 */
  ragWarning?: string | null
}
