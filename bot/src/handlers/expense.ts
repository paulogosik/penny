import type { Context } from "grammy";
import { registrarGastoTexto, BackendIndisponivelError } from "../services/api.js";
import type { ExpenseResponse } from "../types.js";

export async function handleTextExpense(ctx: Context): Promise<void> {
  const texto = ctx.message?.text;
  if (!texto) return;

  await ctx.replyWithChatAction("typing");

  try {
    const gasto = await registrarGastoTexto(texto);
    await ctx.reply(formatarGasto(gasto));
  } catch (err) {
    if (err instanceof BackendIndisponivelError) {
      await ctx.reply(
        `Recebi seu gasto ✍️: "${texto}"\n\n` +
          "Ainda não consigo categorizar sozinho — a integração com o backend vem na próxima etapa.",
      );
      return; 
    }
    throw err;
  }
}

function formatarGasto(g: ExpenseResponse): string {
  return (
    "✅ Gasto anotado!\n" +
    `📂 Categoria: ${g.category}\n` +
    `💰 Valor: R$ ${g.amount.toFixed(2)}\n` +
    `📅 Data: ${g.date}\n` +
    `📝 ${g.description}`
  );
}
