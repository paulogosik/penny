import type { Context } from "grammy";
import { formatarGasto, mensagens } from "../messages.js";
import { registrarGastoTexto } from "../services/api.js";
import { responderErro } from "./errors.js";

const TAMANHO_MAXIMO_TEXTO = 1000;

export async function handleTextExpense(ctx: Context): Promise<void> {
  const texto = ctx.message?.text?.trim();
  const from = ctx.from;
  if (!texto || !from) return;

  if (texto.startsWith("/")) {
    await ctx.reply(mensagens.semComandos);
    return;
  }
  if (texto.length > TAMANHO_MAXIMO_TEXTO) {
    await ctx.reply(mensagens.mensagemLonga);
    return;
  }

  await ctx.replyWithChatAction("typing");
  try {
    const gasto = await registrarGastoTexto(from.id, texto);
    await ctx.reply(formatarGasto(gasto));
  } catch (err) {
    await responderErro(ctx, err);
  }
}
