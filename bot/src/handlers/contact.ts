import type { Context } from "grammy";
import { mensagens } from "../messages.js";
import { registrarUsuario, salvarTelefone } from "../services/api.js";
import { responderErro } from "./errors.js";
import { nomeCompleto, pedirTelefone } from "./onboarding.js";

export async function handleContact(ctx: Context): Promise<void> {
  const contato = ctx.message?.contact;
  const from = ctx.from;
  if (!contato || !from) return;

  if (contato.user_id !== from.id) {
    await pedirTelefone(ctx, mensagens.contatoDeOutraPessoa);
    return;
  }

  try {
    await registrarUsuario(from.id, nomeCompleto(from));
    await salvarTelefone(from.id, contato.phone_number);
    await ctx.reply(mensagens.telefoneSalvo, { reply_markup: { remove_keyboard: true } });
  } catch (err) {
    await responderErro(ctx, err);
  }
}
