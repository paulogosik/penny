export interface UserResponse {
  id: string;
  telegramUserId: number;
  name: string | null;
  timezone: string;
  phoneNumber: string | null;
  createdAt: string;
}

export interface InstallmentInfo {
  planId: string;
  number: number;
  count: number;
  totalAmount: number;
}

export type ExpenseSource = "telegram_text" | "telegram_photo" | "panel" | "recurring";

export interface ExpenseResponse {
  id: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  description: string;
  occurredOn: string;
  source: ExpenseSource;
  creditCardId: string | null;
  creditCardName: string | null;
  billingMonth: string | null;
  installment: InstallmentInfo | null;
  recurringExpenseId: string | null;
  createdAt: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fields?: Record<string, string>;
}
