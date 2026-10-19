import type { ApiErrorBody, ExpenseResponse, UserResponse } from "../types.js";

const REQUEST_TIMEOUT_MS = 90_000;

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function registrarUsuario(telegramUserId: number, name: string): Promise<UserResponse> {
  return request(telegramUserId, "PUT", "/api/me", { name });
}

export function salvarTelefone(telegramUserId: number, phoneNumber: string): Promise<UserResponse> {
  return request(telegramUserId, "PUT", "/api/me/phone", { phoneNumber });
}

export function registrarGastoTexto(telegramUserId: number, text: string): Promise<ExpenseResponse> {
  return request(telegramUserId, "POST", "/api/expenses/text", { text });
}

export function registrarGastoFoto(
  telegramUserId: number,
  imageBase64: string,
  mimeType: string,
): Promise<ExpenseResponse> {
  return request(telegramUserId, "POST", "/api/expenses/photo", { imageBase64, mimeType });
}

async function request<T>(telegramUserId: number, method: string, path: string, body: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${backendUrl()}${path}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        "X-Telegram-User-Id": String(telegramUserId),
      },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch (err) {
    console.error(`Falha ao chamar ${method} ${path}:`, err);
    throw new ApiError(0, "BACKEND_UNAVAILABLE", "Não consegui falar com o servidor.");
  }

  const text = await response.text();
  if (!response.ok) {
    const error = parseError(text);
    throw new ApiError(
      response.status,
      error?.code ?? "UNEXPECTED_ERROR",
      error?.message ?? `O servidor respondeu ${response.status}.`,
    );
  }
  return JSON.parse(text) as T;
}

function parseError(text: string): ApiErrorBody | null {
  try {
    const body: unknown = JSON.parse(text);
    if (typeof body === "object" && body !== null && "code" in body && "message" in body) {
      return body as ApiErrorBody;
    }
  } catch {
    return null;
  }
  return null;
}

function backendUrl(): string {
  const url = process.env.BACKEND_URL;
  if (!url) {
    throw new Error("BACKEND_URL não definido.");
  }
  return url.replace(/\/+$/, "");
}
