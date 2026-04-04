/** /stories/{id}/character-snapshots */

export interface StoryCharacterSnapshotDto {
  id: string
  storyId: string
  sortOrder: number
  clonedFromLibraryId: string | null
  payload: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface CreateCharacterSnapshotRequest {
  libraryCharacterId?: string | null
  payload?: Record<string, unknown> | null
}

export interface PatchCharacterSnapshotRequest {
  sortOrder?: number | null
  payload?: Record<string, unknown> | null
}

export interface ReorderCharacterSnapshotsRequest {
  snapshotIds: string[]
}
