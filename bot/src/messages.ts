import { Keyboard } from "grammy";
import type { ExpenseResponse } from "./types.js";

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const mesPorExtenso = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric", timeZone: "UTC" });

const EXEMPLOS =
  '• "gastei 45 no mercado"\n' +
  '• "uber ontem 23,50"\n' +
  '• "tênis de 600 em 6x no nubank"\n' +
  "• ou uma foto do recibo 📸";

export const mensagens = {
  boasVindas: (nome: string) =>
    `Oi, ${nome}! 👋 Eu sou o Penny e vou te ajudar a organizar seus gastos.\n\n` +
    "Para começar, preciso do seu número de telefone. Com ele, você entra no seu painel no site " +
    "sem precisar de senha: é só digitar o número lá e confirmar por aqui.\n\n" +
    "Toque no botão abaixo para compartilhar. 👇",
  bemVindoDeVolta: (nome: string) =>
    `Que bom te ver de novo, ${nome}! 👋\n\nÉ só me mandar seus gastos do jeito que você falaria:\n${EXEMPLOS}`,
  pedidoTelefone:
    "Antes de anotar seus gastos, preciso do seu número de telefone. " +
    "Ele serve para você entrar no painel do site sem senha.\n\n" +
    "Toque no botão abaixo para compartilhar. 👇",
  telefoneSalvo: `Tudo certo, número salvo! ✅\n\nAgora é só me mandar seus gastos do jeito que você falaria:\n${EXEMPLOS}`,
  contatoDeOutraPessoa:
    "Esse contato não é o seu. 🙂 Para conectar sua conta, use o botão abaixo, que envia o seu próprio número.",
  semComandos: `Não precisa de comandos! É só me mandar o gasto do jeito que você falaria:\n${EXEMPLOS}`,
  mensagemLonga: 'Essa mensagem ficou longa demais pra mim. Me manda só o gasto, tipo "gastei 45 no mercado".',
  fotoGrande: "Essa foto é grande demais. Tenta mandar uma versão menor do recibo. 📸",
  iaIndisponivel: "A IA que classifica seus gastos está sobrecarregada agora. 😕 Tenta de novo em alguns instantes.",
  servidorIndisponivel: "Não consegui falar com o servidor agora. 😕 Tenta de novo em alguns instantes.",
  erroGenerico: "Algo deu errado do meu lado. 😕 Tenta de novo em alguns instantes.",
};

export function tecladoTelefone(): Keyboard {
  return new Keyboard().requestContact("📱 Compartilhar meu número").resized().oneTime();
}

export function formatarGasto(gasto: ExpenseResponse): string {
  const linhas = ["✅ Gasto anotado!", `📂 Categoria: ${gasto.categoryName}`];

  const parcela = gasto.installment;
  if (parcela) {
    linhas.push(
      `💰 Valor: ${formatarValor(parcela.totalAmount)} em ${parcela.count}x de ${formatarValor(gasto.amount)}`,
    );
  } else {
    linhas.push(`💰 Valor: ${formatarValor(gasto.amount)}`);
  }

  linhas.push(`📅 Data: ${formatarData(gasto.occurredOn)}`);

  if (gasto.creditCardName && gasto.billingMonth) {
    const fatura = mesPorExtenso.format(new Date(`${gasto.billingMonth}T00:00:00Z`));
    const quando = parcela ? `1ª parcela na fatura de ${fatura}` : `fatura de ${fatura}`;
    linhas.push(`💳 Cartão: ${gasto.creditCardName} (${quando})`);
  }

  linhas.push(`📝 ${gasto.description}`);
  return linhas.join("\n");
}

export function formatarValor(valor: number): string {
  return moeda.format(valor);
}

export function formatarData(isoDate: string): string {
  const [ano, mes, dia] = isoDate.split("-");
  return `${dia}/${mes}/${ano}`;
}
