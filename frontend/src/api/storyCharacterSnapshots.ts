import type {
  CreateCharacterSnapshotRequest,
  PatchCharacterSnapshotRequest,
  ReorderCharacterSnapshotsRequest,
  StoryCharacterSnapshotDto,
} from '../types/storySnapshots'
import { requestJson } from './client'

export function listStoryCharacterSnapshots(
  storyId: string,
): Promise<StoryCharacterSnapshotDto[]> {
  return requestJson<StoryCharacterSnapshotDto[]>(
    `/stories/${encodeURIComponent(storyId)}/character-snapshots`,
    { method: 'GET' },
  )
}

export function createStoryCharacterSnapshot(
  storyId: string,
  body: CreateCharacterSnapshotRequest,
): Promise<StoryCharacterSnapshotDto> {
  return requestJson<StoryCharacterSnapshotDto>(
    `/stories/${encodeURIComponent(storyId)}/character-snapshots`,
    { method: 'POST', body: JSON.stringify(body) },
  )
}

export function patchStoryCharacterSnapshot(
  storyId: string,
  snapshotId: string,
  body: PatchCharacterSnapshotRequest,
): Promise<StoryCharacterSnapshotDto> {
  return requestJson<StoryCharacterSnapshotDto>(
    `/stories/${encodeURIComponent(storyId)}/character-snapshots/${encodeURIComponent(snapshotId)}`,
    { method: 'PATCH', body: JSON.stringify(body) },
  )
}

export function deleteStoryCharacterSnapshot(
  storyId: string,
  snapshotId: string,
): Promise<null> {
  return requestJson<null>(
    `/stories/${encodeURIComponent(storyId)}/character-snapshots/${encodeURIComponent(snapshotId)}`,
    { method: 'DELETE' },
  )
}

export function reorderStoryCharacterSnapshots(
  storyId: string,
  body: ReorderCharacterSnapshotsRequest,
): Promise<null> {
  return requestJson<null>(
    `/stories/${encodeURIComponent(storyId)}/character-snapshots/order`,
    { method: 'PUT', body: JSON.stringify(body) },
  )
}
