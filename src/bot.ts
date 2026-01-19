import { Bot } from "grammy";
import "dotenv/config"; // Carrega as variáveis do .env

// Verifica se o token existe
const token = process.env.BOT_TOKEN;
if (!token) {
  throw new Error("O BOT_TOKEN não foi definido no arquivo .env");
}

// Cria a instância do bot
const bot = new Bot(token);

// Comando /start (Quando você clica em "Começar" no Telegram)
bot.command("start", (ctx) => {
  ctx.reply("Olá! Eu sou o Penny 🤖. \nEnvie uma mensagem com um gasto e eu vou tentar anotar.");
});

// Ouve qualquer mensagem de texto
bot.on("message:text", (ctx) => {
  const textoRecebido = ctx.message.text;
  
  // Por enquanto, só vamos repetir o que o usuário disse (Echo)
  // Futuramente, aqui entrará a IA para interpretar
  ctx.reply(`Você disse: "${textoRecebido}". \n(Ainda estou aprendendo a ler gastos!)`);
});

// Tratamento de erros
bot.catch((err) => {
  console.error("Erro no bot:", err);
});

// Inicia o bot
console.log("Penny está online! 🚀");
bot.start();