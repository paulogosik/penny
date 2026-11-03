import assert from "node:assert/strict";
import { after, before, beforeEach, describe, test } from "node:test";
import { mensagens } from "../src/messages.js";
import {
  ANA,
  BackendFalso,
  TelegramFalso,
  capturarErros,
  contato,
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

describe("cadastro pelo /start", () => {
  test("pessoa nova é cadastrada com o nome do Telegram e recebe o pedido de telefone", async () => {
    backend.responder("PUT /api/me", { status: 201, corpo: usuario({ phoneNumber: null }) });

    await telegram.enviar(texto(ANA, "/start"));

    assert.deepEqual(backend.chamadas, [
      { metodo: "PUT", caminho: "/api/me", usuario: "1001", corpo: { name: "Ana Souza" } },
    ]);
    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.boasVindas("Ana"));
    assert.equal(pedeContato(resposta), true);
  });

  test("quem já tem telefone recebe boas-vindas de volta, sem teclado", async () => {
    backend.responder("PUT /api/me", { status: 200, corpo: usuario() });

    await telegram.enviar(texto(ANA, "/start"));

    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.bemVindoDeVolta("Ana"));
    assert.equal(resposta?.payload["reply_markup"], undefined);
  });

  test("/start com o servidor fora do ar avisa que não conseguiu falar com ele", async (t) => {
    capturarErros(t);
    await usarBackendForaDoAr(t);

    await telegram.enviar(texto(ANA, "/start"));

    assert.deepEqual(telegram.textos(), [mensagens.servidorIndisponivel]);
  });
});

describe("compartilhar o telefone", () => {
  test("contato da própria pessoa salva o número e tira o teclado", async () => {
    backend.responder("PUT /api/me", { status: 200, corpo: usuario({ phoneNumber: null }) });
    backend.responder("PUT /api/me/phone", { status: 200, corpo: usuario() });

    await telegram.enviar(contato(ANA, { phone_number: "5511987654321", first_name: "Ana", user_id: ANA.id }));

    assert.deepEqual(
      backend.chamadas.map(({ metodo, caminho, usuario, corpo }) => [metodo, caminho, usuario, corpo]),
      [
        ["PUT", "/api/me", "1001", { name: "Ana Souza" }],
        ["PUT", "/api/me/phone", "1001", { phoneNumber: "5511987654321" }],
      ],
    );
    const [resposta] = telegram.mensagens();
    assert.equal(resposta?.payload["text"], mensagens.telefoneSalvo);
    assert.deepEqual(resposta?.payload["reply_markup"], { remove_keyboard: true });
  });

  test("número já ligado a outra conta mostra o motivo que o servidor deu", async () => {
    backend.responder("PUT /api/me", { status: 200, corpo: usuario({ phoneNumber: null }) });
    backend.responder("PUT /api/me/phone", {
      status: 409,
      corpo: { code: "PHONE_NUMBER_TAKEN", message: "Esse número já está ligado a outra conta." },
    });

    await telegram.enviar(contato(ANA, { phone_number: "5511987654321", first_name: "Ana", user_id: ANA.id }));

    assert.deepEqual(telegram.textos(), ["🤔 Esse número já está ligado a outra conta."]);
  });

  test("número que o servidor considera inválido mostra o motivo", async () => {
    backend.responder("PUT /api/me", { status: 200, corpo: usuario({ phoneNumber: null }) });
    backend.responder("PUT /api/me/phone", {
      status: 422,
      corpo: { code: "INVALID_PHONE_NUMBER", message: "Número de telefone inválido." },
    });

    await telegram.enviar(contato(ANA, { phone_number: "123", first_name: "Ana", user_id: ANA.id }));

    assert.deepEqual(telegram.textos(), ["🤔 Número de telefone inválido."]);
  });
});
