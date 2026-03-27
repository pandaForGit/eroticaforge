/** 与后端 StoryListItemDto / StoryDetailDto 的 JSON 字段对齐（Instant → ISO 字符串） */

export interface CreateStoryRequest {
  title: string
  tags: string[] | null
}

export interface CreateStoryResponse {
  storyId: string
  title: string
  createdAt: string
}

export interface StoryListItemDto {
  storyId: string
  title: string
  tags: string[] | null
  totalChapters: number
  updatedAt: string
}

export interface StoryDetailDto {
  storyId: string
  title: string
  tags: string[] | null
  totalChapters: number
  nextChapterSeq: number
  mainModel: string
  createdAt: string
  updatedAt: string
}
