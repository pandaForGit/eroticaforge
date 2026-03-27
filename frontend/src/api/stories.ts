/**
 * 故事：POST/GET/DELETE /stories、GET /stories/{id}
 */

import type {
  CreateStoryRequest,
  CreateStoryResponse,
  StoryDetailDto,
  StoryListItemDto,
} from '../types/stories'
import { requestJson } from './client'

export function listStories(): Promise<StoryListItemDto[]> {
  return requestJson<StoryListItemDto[]>('/stories', { method: 'GET' })
}

export function getStory(storyId: string): Promise<StoryDetailDto> {
  return requestJson<StoryDetailDto>(`/stories/${encodeURIComponent(storyId)}`, {
    method: 'GET',
  })
}

export function createStory(body: CreateStoryRequest): Promise<CreateStoryResponse> {
  return requestJson<CreateStoryResponse>('/stories', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function deleteStory(storyId: string): Promise<null> {
  return requestJson<null>(`/stories/${encodeURIComponent(storyId)}`, {
    method: 'DELETE',
  })
}
