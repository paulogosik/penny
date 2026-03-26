import { Bot } from "grammy";
import "dotenv/config";

const token = process.env.BOT_TOKEN;
if (!token) {
  throw new Error("O BOT_TOKEN não foi definido no arquivo .env");
}

const bot = new Bot(token);

bot.command("start", (ctx) => {
  ctx.reply("Olá! Eu sou o Penny 🤖. \nEnvie uma mensagem com um gasto e eu vou tentar anotar.");
});

bot.on("message:text", (ctx) => {
  const textoRecebido = ctx.message.text;
  ctx.reply(`Você disse: "${textoRecebido}". \n(Ainda estou aprendendo a ler gastos!)`);
});

bot.catch((err) => {
  console.error("Erro no bot:", err);
});

console.log("Penny está online! 🚀");
bot.start();
