/** GET /character-library */

export interface CharacterLibraryItemDto {
  id: string
  displayName: string
  sourceRelativePath: string
  schemaVersion: string
  contentSha256: string
  roleIndex: number
}
