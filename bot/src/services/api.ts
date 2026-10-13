import type { ExpenseResponse } from "../types.js";

// Único ponto de contato do bot com o backend Java/Spring.
// O bot só monta o payload; toda a lógica de negócio e IA vive no backend.

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
  // TODO — Fase 2: enviar ao backend e devolver a resposta.
  // POST ${BACKEND_URL}/api/expenses/text  body: { text } (ExpenseTextRequest)
  //
  // const res = await fetch(`${process.env.BACKEND_URL}/api/expenses/text`, {
  //   method: "POST",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify({ text }),
  // });
  // if (!res.ok) throw new BackendIndisponivelError(`Backend respondeu ${res.status}.`);
  // return (await res.json()) as ExpenseResponse;
  throw new BackendIndisponivelError();
}

export async function registrarGastoFoto(
  imageBase64: string,
  mimeType: string,
): Promise<ExpenseResponse> {
  // TODO — Fase 2: enviar ao backend e devolver a resposta.
  // POST ${BACKEND_URL}/api/expenses/photo  body: { imageBase64, mimeType } (ExpensePhotoRequest)
  //
  // const res = await fetch(`${process.env.BACKEND_URL}/api/expenses/photo`, {
  //   method: "POST",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify({ imageBase64, mimeType }),
  // });
  // if (!res.ok) throw new BackendIndisponivelError(`Backend respondeu ${res.status}.`);
  // return (await res.json()) as ExpenseResponse;
  throw new BackendIndisponivelError();
}
