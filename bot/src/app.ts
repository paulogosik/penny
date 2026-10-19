import { Bot, type BotConfig, type Context } from "grammy";
import { handleContact } from "./handlers/contact.js";
import { handleTextExpense } from "./handlers/expense.js";
import { handlePhoto } from "./handlers/photo.js";
import { handleStart } from "./handlers/start.js";

export function createBot(token: string, config?: BotConfig<Context>): Bot {
  const bot = new Bot(token, config);

  bot.command("start", handleStart);
  bot.on("message:contact", handleContact);
  bot.on("message:photo", handlePhoto);
  bot.on("message:text", handleTextExpense);

  bot.catch((err) => {
    console.error("Erro no bot:", err);
  });

  return bot;
}
