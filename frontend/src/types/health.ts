/** GET /api/health 为裸 JSON，非 ApiResponse 包裹 */

export interface HealthResponse {
  status: string
  database: string
  llm: string
  embedding: string
}
