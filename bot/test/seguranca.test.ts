import assert from "node:assert/strict";
import { after, before, beforeEach, describe, test } from "node:test";
import { mensagens } from "../src/messages.js";
import {
  ANA,
  BRUNO,
  BackendFalso,
  GRUPO,
  TOKEN,
  TelegramFalso,
  capturarErros,
  contato,
  foto,
  interceptarDownloads,
  pedeContato,
  texto,
} from "./support/ambiente.js";
import { gasto, respostaGasto, usuario } from "./support/dados.js";

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

describe("segurança do bot", () => {
  test("contato de outra pessoa não é salvo e o bot pede o número de quem enviou", async () => {
    await telegram.enviar(contato(ANA, { phone_number: "5511912345678", first_name: "Bruno", user_id: BRUNO.id }));

    assert.equal(backend.chamadas.length, 0);
    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.contatoDeOutraPessoa);
    assert.equal(pedeContato(resposta), true);
  });

  test("contato digitado à mão (sem conta do Telegram ligada) não é salvo", async () => {
    await telegram.enviar(contato(ANA, { phone_number: "5511912345678", first_name: "Ana" }));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.contatoDeOutraPessoa]);
  });

  test("mensagens em grupos são ignoradas para não expor gastos para outras pessoas", async (t) => {
    interceptarDownloads(t, () => new Response("foto"));

    await telegram.enviar(texto(ANA, "/start", GRUPO));
    await telegram.enviar(texto(ANA, "gastei 45 no mercado", GRUPO));
    await telegram.enviar(foto(ANA, [{ file_id: "recibo", file_size: 1_000 }], GRUPO));
    await telegram.enviar(contato(ANA, { phone_number: "5511987654321", first_name: "Ana", user_id: ANA.id }, GRUPO));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.chamadas, []);
  });

  test("o servidor sempre recebe o ID de quem enviou, mesmo em mensagem encaminhada de outra pessoa", async () => {
    backend.responder("POST /api/messages/text", { status: 201, corpo: respostaGasto(gasto()) });
    const encaminhada = texto(ANA, "gastei 45 no mercado");
    Object.assign(encaminhada.message ?? {}, { forward_origin: { type: "user", sender_user: BRUNO, date: 1_759_000_000 } });

    await telegram.enviar(encaminhada);

    assert.deepEqual(backend.chamadas.map((chamada) => chamada.usuario), [String(ANA.id)]);
  });

  test("respostas não usam formatação, então texto vindo de fora aparece literal", async () => {
    backend.responder("PUT /api/me", { status: 200, corpo: usuario() });
    const salvo = gasto({ categoryName: "<i>Mercado</i>", description: "<b>negrito</b> *estrela* [link](https://exemplo.com)" });
    backend.responder("POST /api/messages/text", { status: 201, corpo: respostaGasto(salvo) });

    await telegram.enviar(texto({ ...ANA, first_name: "<b>Ana</b>" }, "/start"));
    await telegram.enviar(texto(ANA, "gastei 45 no mercado"));

    const respostas = telegram.mensagens();
    assert.equal(respostas.length, 2);
    for (const resposta of respostas) {
      assert.equal(resposta.payload["parse_mode"], undefined);
      assert.equal(resposta.payload["entities"], undefined);
    }
    assert.match(String(respostas[0]?.payload["text"]), /<b>Ana<\/b>/);
    assert.match(String(respostas[1]?.payload["text"]), /<b>negrito<\/b> \*estrela\* \[link\]\(https:\/\/exemplo\.com\)/);
  });

  test("detalhes de erro interno do servidor não chegam à pessoa", async (t) => {
    capturarErros(t);
    backend.responder("POST /api/messages/text", {
      status: 500,
      corpo: { code: "INTERNAL_ERROR", message: "NullPointerException em com.penny.service.ExpenseService" },
    });

    await telegram.enviar(texto(ANA, "gastei 45 no mercado"));

    assert.deepEqual(telegram.textos(), [mensagens.erroGenerico]);
  });

  test("o token do bot não vaza para a pessoa nem para os logs quando o download da foto falha", async (t) => {
    const logs = capturarErros(t);
    interceptarDownloads(t, () => new Response("não encontrado", { status: 404 }));

    await telegram.enviar(foto(ANA, [{ file_id: "recibo", file_size: 1_000 }]));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.erroGenerico]);
    assert.notEqual(logs(), "");
    assert.equal(logs().includes(TOKEN), false);
    assert.equal(telegram.textos().join("\n").includes(TOKEN), false);
  });
});
