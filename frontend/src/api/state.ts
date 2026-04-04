import type { StoryStateResponse, UpdateStoryStateRequest } from '../types/storyState'
import { requestJson } from './client'

export function getStoryState(storyId: string): Promise<StoryStateResponse> {
  return requestJson<StoryStateResponse>(
    `/stories/${encodeURIComponent(storyId)}/state`,
    { method: 'GET' },
  )
}

export function putStoryState(
  storyId: string,
  body: UpdateStoryStateRequest,
): Promise<StoryStateResponse> {
  return requestJson<StoryStateResponse>(
    `/stories/${encodeURIComponent(storyId)}/state`,
    {
      method: 'PUT',
      body: JSON.stringify(body),
    },
  )
}
