import { Bot } from "grammy";
import "dotenv/config";
import { handleStart } from "./handlers/start.js";
import { handleTextExpense } from "./handlers/expense.js";
import { handlePhoto } from "./handlers/photo.js";

const token = process.env.BOT_TOKEN;
if (!token) {
  throw new Error("O BOT_TOKEN não foi definido no arquivo .env");
}

const bot = new Bot(token);

bot.command("start", handleStart);
bot.on("message:photo", handlePhoto);
bot.on("message:text", handleTextExpense);

bot.catch((err) => {
  console.error("Erro no bot:", err);
});

console.log("Penny está online! 🚀");
bot.start();
