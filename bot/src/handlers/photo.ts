import type { Context } from "grammy";
import { formatarGasto, mensagens } from "../messages.js";
import { registrarGastoFoto } from "../services/api.js";
import { responderErro } from "./errors.js";

const TAMANHO_MAXIMO_FOTO = 10 * 1024 * 1024;

export async function handlePhoto(ctx: Context): Promise<void> {
  const fotos = ctx.message?.photo;
  const from = ctx.from;
  if (!fotos || fotos.length === 0 || !from) return;

  const maior = fotos[fotos.length - 1];
  if (!maior) return;
  if (maior.file_size !== undefined && maior.file_size > TAMANHO_MAXIMO_FOTO) {
    await ctx.reply(mensagens.fotoGrande);
    return;
  }

  await ctx.replyWithChatAction("typing");
  try {
    const base64 = await baixarComoBase64(ctx, maior.file_id);
    const gasto = await registrarGastoFoto(from.id, base64, "image/jpeg");
    await ctx.reply(formatarGasto(gasto));
  } catch (err) {
    await responderErro(ctx, err);
  }
}

async function baixarComoBase64(ctx: Context, fileId: string): Promise<string> {
  const file = await ctx.api.getFile(fileId);
  if (!file.file_path) {
    throw new Error("O Telegram não retornou o caminho do arquivo.");
  }

  const url = `https://api.telegram.org/file/bot${ctx.api.token}/${file.file_path}`;
  const resp = await fetch(url);
  if (!resp.ok) {
    throw new Error(`Falha ao baixar a foto do Telegram (${resp.status}).`);
  }

  const buffer = Buffer.from(await resp.arrayBuffer());
  return buffer.toString("base64");
}
