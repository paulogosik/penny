import type { ExpenseResponse } from "../types.js";

export class BackendIndisponivelError extends Error {
  constructor(
    message = "Integração com o backend ainda não implementada (fase 2).",
  ) {
    super(message);
    this.name = "BackendIndisponivelError";
  }
}

export async function registrarGastoTexto(
  text: string,
): Promise<ExpenseResponse> {
  throw new BackendIndisponivelError();
}

export async function registrarGastoFoto(
  imageBase64: string,
  mimeType: string,
): Promise<ExpenseResponse> {
  throw new BackendIndisponivelError();
}
