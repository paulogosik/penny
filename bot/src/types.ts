// Contrato de dados trocado com o backend (Java/Spring).
// A chamada HTTP em si é fase 2 — ver services/api.ts.

export interface ExpenseTextRequest {
  text: string;
}

export interface ExpensePhotoRequest {
  imageBase64: string;
  mimeType: string;
}

export interface ExpenseResponse {
  category: string;
  amount: number;
  date: string; // ISO 8601, ex.: "2026-07-10"
  description: string;
}
