/** 与后端 ApiResponse / ApiErrorResponse、docs/api/API 接口定义.md 第 4 节对齐 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface ApiErrorResponse {
  code: number
  message: string
  error: string
}
