import type { Context } from "grammy";
import { responderErro } from "./errors.js";
import { iniciarCadastro } from "./onboarding.js";

export async function handleStart(ctx: Context): Promise<void> {
  try {
    await iniciarCadastro(ctx);
  } catch (err) {
    await responderErro(ctx, err);
  }
}
