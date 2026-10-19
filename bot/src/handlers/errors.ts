import type { Context } from "grammy";
import { mensagens } from "../messages.js";
import { ApiError } from "../services/api.js";
import { iniciarCadastro, pedirTelefone } from "./onboarding.js";

export async function responderErro(ctx: Context, err: unknown): Promise<void> {
  if (!(err instanceof ApiError)) {
    console.error("Erro inesperado:", err);
    await ctx.reply(mensagens.erroGenerico);
    return;
  }

  switch (err.code) {
    case "PHONE_NUMBER_REQUIRED":
      await pedirTelefone(ctx);
      return;
    case "USER_NOT_REGISTERED":
      await iniciarCadastro(ctx);
      return;
    case "AI_UNAVAILABLE":
      await ctx.reply(mensagens.iaIndisponivel);
      return;
    case "BACKEND_UNAVAILABLE":
      await ctx.reply(mensagens.servidorIndisponivel);
      return;
  }

  if (err.status === 409 || err.status === 422) {
    await ctx.reply(`🤔 ${err.message}`);
    return;
  }

  console.error(`Erro da API (${err.status} ${err.code}):`, err.message);
  await ctx.reply(mensagens.erroGenerico);
}
