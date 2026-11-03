import type { ExpenseResponse, UserResponse } from "../../src/types.js";

export function usuario(parcial: Partial<UserResponse> = {}): UserResponse {
  return {
    id: "8f1c2c58-0000-4000-8000-000000000001",
    telegramUserId: 1001,
    name: "Ana Souza",
    timezone: "America/Sao_Paulo",
    phoneNumber: "+5511987654321",
    createdAt: "2026-10-13T15:00:00Z",
    ...parcial,
  };
}

export function gasto(parcial: Partial<ExpenseResponse> = {}): ExpenseResponse {
  return {
    id: "8f1c2c58-0000-4000-8000-000000000002",
    categoryId: "8f1c2c58-0000-4000-8000-000000000003",
    categoryName: "Mercado",
    amount: 45,
    description: "Mercado",
    occurredOn: "2026-10-13",
    source: "telegram_text",
    creditCardId: null,
    creditCardName: null,
    billingMonth: null,
    installment: null,
    recurringExpenseId: null,
    createdAt: "2026-10-13T15:00:00Z",
    ...parcial,
  };
}
