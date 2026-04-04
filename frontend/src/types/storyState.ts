export interface StoryStateResponse {
  storyId: string
  version: number
  currentSummary: string | null
  characterStates: Record<string, string>
  importantFacts: string[]
  worldFlags: string[]
  lastChapterEnding: string | null
  updatedAt: string
}

export interface UpdateStoryStateRequest {
  version: number
  currentSummary?: string | null
  characterStates?: Record<string, string> | null
  importantFacts?: string[] | null
  worldFlags?: string[] | null
  lastChapterEnding?: string | null
}
