import type { Context } from "grammy";

export async function handleStart(ctx: Context): Promise<void> {
  await ctx.reply(
    "Olá! Eu sou o Penny 🤖.\n\n" +
      'Me envie um gasto por texto (ex.: "gastei 45 no mercado") ' +
      "ou uma foto do recibo que eu anoto pra você.",
  );
}
