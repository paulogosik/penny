import "dotenv/config";
import { createBot } from "./app.js";

const token = process.env.BOT_TOKEN;
if (!token) {
  throw new Error("O BOT_TOKEN não foi definido no arquivo .env");
}
if (!process.env.BACKEND_URL) {
  throw new Error("O BACKEND_URL não foi definido no arquivo .env");
}

const bot = createBot(token);

process.once("SIGINT", () => bot.stop());
process.once("SIGTERM", () => bot.stop());

console.log("Penny está online! 🚀");
bot.start();
