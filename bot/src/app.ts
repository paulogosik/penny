import { Bot, type BotConfig, type Context } from "grammy";
import { handleContact } from "./handlers/contact.js";
import { handlePhoto } from "./handlers/photo.js";
import { handleStart } from "./handlers/start.js";
import { handleText } from "./handlers/text.js";

export function createBot(token: string, config?: BotConfig<Context>): Bot {
  const bot = new Bot(token, config);
  const privado = bot.chatType("private");

  privado.command("start", handleStart);
  privado.on("message:contact", handleContact);
  privado.on("message:photo", handlePhoto);
  privado.on("message:text", handleText);

  bot.catch((err) => {
    console.error("Erro no bot:", err);
  });

  return bot;
}
