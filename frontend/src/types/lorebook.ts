export interface LorebookItemDto {
  id: number
  keyword: string
  body: string
  createdAt: string
}

export interface LorebookCreateRequest {
  keyword: string
  body: string
}
