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
  date: string;
  description: string;
}
