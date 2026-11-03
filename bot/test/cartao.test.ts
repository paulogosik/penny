import assert from "node:assert/strict";
import { after, before, beforeEach, describe, test } from "node:test";
import { formatarCartaoAtualizado, formatarCartaoCadastrado, formatarGasto } from "../src/messages.js";
import { ANA, BackendFalso, TelegramFalso, texto } from "./support/ambiente.js";
import { cartao, gasto, respostaCartao, respostaGasto } from "./support/dados.js";

const backend = new BackendFalso();
let telegram: TelegramFalso;

before(async () => {
  process.env.BACKEND_URL = await backend.iniciar();
});
after(() => backend.parar());
beforeEach(() => {
  backend.limpar();
  telegram = new TelegramFalso();
});

describe("cartão de crédito pelo bot", () => {
  test("frase de cadastro vai para a rota de mensagens e o bot confirma o cartão novo", async () => {
    const novo = cartao({ name: "Nubank", closingDay: 3, dueDay: 10 });
    backend.responder("POST /api/messages/text", { status: 201, corpo: respostaCartao("credit_card_created", novo) });

    await telegram.enviar(texto(ANA, "meu cartão nubank fecha dia 3 e vence dia 10"));

    assert.deepEqual(backend.chamadas, [
      {
        metodo: "POST",
        caminho: "/api/messages/text",
        usuario: "1001",
        corpo: { text: "meu cartão nubank fecha dia 3 e vence dia 10" },
      },
    ]);
    assert.deepEqual(telegram.textos(), [formatarCartaoCadastrado(novo)]);
  });

  test("cartão que já existia é confirmado como atualizado", async () => {
    const atualizado = cartao({ name: "Nubank", closingDay: 20, dueDay: 27 });
    backend.responder("POST /api/messages/text", { status: 200, corpo: respostaCartao("credit_card_updated", atualizado) });

    await telegram.enviar(texto(ANA, "meu nubank agora fecha dia 20 e vence dia 27"));

    assert.deepEqual(telegram.textos(), [formatarCartaoAtualizado(atualizado)]);
  });

  test("gasto continua sendo respondido com o resumo do gasto", async () => {
    const salvo = gasto({ creditCardId: "c1", creditCardName: "Nubank", billingMonth: "2026-11-01" });
    backend.responder("POST /api/messages/text", { status: 201, corpo: respostaGasto(salvo) });

    await telegram.enviar(texto(ANA, "fone 120 no nubank"));

    assert.deepEqual(telegram.textos(), [formatarGasto(salvo)]);
  });

  for (const [codigo, explicacao] of [
    ["CREDIT_CARD_DETAILS_MISSING", 'Faltou o dia de fechamento e o de vencimento. Tente: "meu cartão Inter fecha dia 5 e vence dia 12".'],
    ["CREDIT_CARD_NOT_REGISTERED", 'Não encontrei o cartão Itaú. Para cadastrar, me mande: "meu cartão Itaú fecha dia 5 e vence dia 12".'],
    ["INVALID_CREDIT_CARD_DAY", "Os dias de fechamento e de vencimento precisam estar entre 1 e 31."],
    ["MESSAGE_NOT_UNDERSTOOD", 'Não entendi. Me mande um gasto, como "gastei 45 no mercado", ou um cartão.'],
  ] as const) {
    test(`erro ${codigo} mostra a explicação do servidor`, async () => {
      backend.responder("POST /api/messages/text", { status: 422, corpo: { code: codigo, message: explicacao } });

      await telegram.enviar(texto(ANA, "mensagem qualquer"));

      assert.deepEqual(telegram.textos(), [`🤔 ${explicacao}`]);
    });
  }
});
