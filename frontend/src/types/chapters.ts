/** 与 ChapterSummaryDto / ChapterDetailDto 对齐 */

export interface ChapterSummaryDto {
  chapterId: string
  seq: number
  title: string
  createdAt: string
}

export interface ChapterDetailDto {
  chapterId: string
  seq: number
  title: string
  content: string
  metadata: Record<string, unknown>
  createdAt: string
}
