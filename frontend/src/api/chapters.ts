import type { ChapterDetailDto, ChapterSummaryDto } from '../types/chapters'
import { requestJson } from './client'

export function listChapters(storyId: string): Promise<ChapterSummaryDto[]> {
  return requestJson<ChapterSummaryDto[]>(
    `/stories/${encodeURIComponent(storyId)}/chapters`,
    { method: 'GET' },
  )
}

export function getChapter(
  storyId: string,
  chapterId: string,
): Promise<ChapterDetailDto> {
  return requestJson<ChapterDetailDto>(
    `/stories/${encodeURIComponent(storyId)}/chapters/${encodeURIComponent(chapterId)}`,
    { method: 'GET' },
  )
}
