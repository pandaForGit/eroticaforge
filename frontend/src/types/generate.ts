export interface GenerateRequest {
  prompt: string
  maxTokens?: number | null
  useMultiModel?: boolean | null
}

export interface GenerateSyncResponse {
  text: string
  chapterId: string
}
