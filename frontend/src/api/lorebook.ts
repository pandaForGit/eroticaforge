import type { LorebookCreateRequest, LorebookItemDto } from '../types/lorebook'
import { requestJson } from './client'

export function listLorebook(): Promise<LorebookItemDto[]> {
  return requestJson<LorebookItemDto[]>('/lorebook', { method: 'GET' })
}

export function createLorebookEntry(
  body: LorebookCreateRequest,
): Promise<LorebookItemDto> {
  return requestJson<LorebookItemDto>('/lorebook', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
