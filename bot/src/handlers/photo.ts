import type { Context } from "grammy";
import { registrarGastoFoto, BackendIndisponivelError } from "../services/api.js";
import type { ExpenseResponse } from "../types.js";

export async function handlePhoto(ctx: Context): Promise<void> {
  const fotos = ctx.message?.photo;
  if (!fotos || fotos.length === 0) return;

  await ctx.replyWithChatAction("typing");

  // O Telegram envia várias resoluções da mesma foto; a última é a maior.
  const maior = fotos[fotos.length - 1];
  if (!maior) return;

  const base64 = await baixarComoBase64(ctx, maior.file_id);

  try {
    const gasto = await registrarGastoFoto(base64, "image/jpeg");
    await ctx.reply(formatarGasto(gasto));
  } catch (err) {
    if (err instanceof BackendIndisponivelError) {
      await ctx.reply(
        "Recebi sua foto 📸 e já consigo prepará-la pro backend.\n\n" +
          "A leitura do recibo por IA vem na próxima etapa.",
      );
      return;
    }
    throw err;
  }
}

async function baixarComoBase64(ctx: Context, fileId: string): Promise<string> {
  const token = process.env.BOT_TOKEN;
  if (!token) throw new Error("BOT_TOKEN não definido.");

  const file = await ctx.api.getFile(fileId);
  if (!file.file_path) {
    throw new Error("O Telegram não retornou o caminho do arquivo.");
  }

  const url = `https://api.telegram.org/file/bot${token}/${file.file_path}`;
  const resp = await fetch(url);
  if (!resp.ok) {
    throw new Error(`Falha ao baixar a foto do Telegram (${resp.status}).`);
  }

  const buffer = Buffer.from(await resp.arrayBuffer());
  return buffer.toString("base64");
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
