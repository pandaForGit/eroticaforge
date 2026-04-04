import type { CharacterLibraryItemDto } from '../types/characterLibrary'
import { requestJson } from './client'

export function listCharacterLibrary(params?: {
  query?: string
  limit?: number
}): Promise<CharacterLibraryItemDto[]> {
  const sp = new URLSearchParams()
  if (params?.query) sp.set('query', params.query)
  if (params?.limit != null) sp.set('limit', String(params.limit))
  const q = sp.toString()
  return requestJson<CharacterLibraryItemDto[]>(
    `/character-library${q ? `?${q}` : ''}`,
    { method: 'GET' },
  )
}
