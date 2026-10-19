import type { Context } from "grammy";
import { mensagens, tecladoTelefone } from "../messages.js";
import { registrarUsuario } from "../services/api.js";

export async function iniciarCadastro(ctx: Context): Promise<void> {
  const from = ctx.from;
  if (!from) return;

  const usuario = await registrarUsuario(from.id, nomeCompleto(from));
  if (usuario.phoneNumber) {
    await ctx.reply(mensagens.bemVindoDeVolta(from.first_name));
    return;
  }
  await ctx.reply(mensagens.boasVindas(from.first_name), { reply_markup: tecladoTelefone() });
}

export async function pedirTelefone(ctx: Context, texto: string = mensagens.pedidoTelefone): Promise<void> {
  await ctx.reply(texto, { reply_markup: tecladoTelefone() });
}

export function nomeCompleto(from: { first_name: string; last_name?: string }): string {
  return [from.first_name, from.last_name].filter(Boolean).join(" ");
}
