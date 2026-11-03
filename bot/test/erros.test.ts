import assert from "node:assert/strict";
import { after, before, beforeEach, describe, test } from "node:test";
import { mensagens } from "../src/messages.js";
import {
  ANA,
  BackendFalso,
  type RespostaBackend,
  TelegramFalso,
  capturarErros,
  pedeContato,
  texto,
  usarBackendForaDoAr,
} from "./support/ambiente.js";
import { usuario } from "./support/dados.js";

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

function gastoFalhaCom(resposta: RespostaBackend): Promise<void> {
  backend.responder("POST /api/messages/text", resposta);
  return telegram.enviar(texto(ANA, "gastei 45 no mercado"));
}

describe("erros do servidor viram mensagens amigáveis", () => {
  test("sem telefone cadastrado, pede o número de novo com o botão", async () => {
    await gastoFalhaCom({ status: 422, corpo: { code: "PHONE_NUMBER_REQUIRED", message: "Compartilhe seu número." } });

    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.pedidoTelefone);
    assert.equal(pedeContato(resposta), true);
  });

  test("usuário não cadastrado é cadastrado na hora e recebe as boas-vindas", async () => {
    backend.responder("PUT /api/me", { status: 201, corpo: usuario({ phoneNumber: null }) });

    await gastoFalhaCom({ status: 404, corpo: { code: "USER_NOT_REGISTERED", message: "Envie /start." } });

    assert.deepEqual(
      backend.chamadas.map((chamada) => `${chamada.metodo} ${chamada.caminho}`),
      ["POST /api/messages/text", "PUT /api/me"],
    );
    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.boasVindas("Ana"));
    assert.equal(pedeContato(resposta), true);
  });

  test("IA fora do ar avisa para tentar de novo", async () => {
    await gastoFalhaCom({ status: 502, corpo: { code: "AI_UNAVAILABLE", message: "A IA está indisponível (HTTP 503)." } });

    assert.deepEqual(telegram.textos(), [mensagens.iaIndisponivel]);
  });

  test("regra de negócio (422) mostra a explicação do servidor", async () => {
    await gastoFalhaCom({
      status: 422,
      corpo: { code: "INSTALLMENTS_REQUIRE_CREDIT_CARD", message: "Compras parceladas precisam de um cartão cadastrado." },
    });

    assert.deepEqual(telegram.textos(), ["🤔 Compras parceladas precisam de um cartão cadastrado."]);
  });

  test("conflito (409) mostra a explicação do servidor", async () => {
    await gastoFalhaCom({ status: 409, corpo: { code: "DATA_CONFLICT", message: "A operação conflita com dados existentes." } });

    assert.deepEqual(telegram.textos(), ["🤔 A operação conflita com dados existentes."]);
  });

  test("erro de validação (400) não mostra detalhes técnicos", async (t) => {
    capturarErros(t);

    await gastoFalhaCom({ status: 400, corpo: { code: "VALIDATION_ERROR", message: "Dados inválidos.", fields: { text: "obrigatório" } } });

    assert.deepEqual(telegram.textos(), [mensagens.erroGenerico]);
  });

  test("resposta que não é JSON (ex.: página de erro de um proxy) vira erro genérico", async (t) => {
    capturarErros(t);

    await gastoFalhaCom({ status: 502, html: "<html><body>Bad Gateway</body></html>" });

    assert.deepEqual(telegram.textos(), [mensagens.erroGenerico]);
  });

  test("servidor fora do ar avisa que não conseguiu falar com ele", async (t) => {
    capturarErros(t);
    await usarBackendForaDoAr(t);

    await telegram.enviar(texto(ANA, "gastei 45 no mercado"));

    assert.deepEqual(telegram.textos(), [mensagens.servidorIndisponivel]);
  });
});
