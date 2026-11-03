import assert from "node:assert/strict";
import { after, before, beforeEach, describe, test } from "node:test";
import { formatarGasto, mensagens } from "../src/messages.js";
import {
  ANA,
  BackendFalso,
  TOKEN,
  TelegramFalso,
  capturarErros,
  foto,
  interceptarDownloads,
  texto,
} from "./support/ambiente.js";
import { gasto } from "./support/dados.js";

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

describe("gasto por texto", () => {
  test("mostra 'digitando', manda o texto sem espaços sobrando e responde com o gasto formatado", async () => {
    const salvo = gasto({ amount: 45, description: "Mercado" });
    backend.responder("POST /api/expenses/text", { status: 201, corpo: salvo });

    await telegram.enviar(texto(ANA, "   gastei 45 no mercado   "));

    assert.deepEqual(backend.chamadas, [
      { metodo: "POST", caminho: "/api/expenses/text", usuario: "1001", corpo: { text: "gastei 45 no mercado" } },
    ]);
    assert.deepEqual(
      telegram.chamadas.map((chamada) => chamada.metodo),
      ["sendChatAction", "sendMessage"],
    );
    assert.equal(telegram.chamadas[0]?.payload["action"], "typing");
    assert.deepEqual(telegram.textos(), [formatarGasto(salvo)]);
  });

  test("texto com exatamente 1000 caracteres ainda é enviado", async () => {
    backend.responder("POST /api/expenses/text", { status: 201, corpo: gasto() });

    await telegram.enviar(texto(ANA, "a".repeat(1000)));

    assert.equal(backend.chamadas.length, 1);
  });

  test("texto acima de 1000 caracteres é recusado sem chamar o servidor", async () => {
    await telegram.enviar(texto(ANA, "a".repeat(1001)));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.mensagemLonga]);
  });

  test("comando desconhecido explica que não precisa de comandos", async () => {
    await telegram.enviar(texto(ANA, "/ajuda"));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.semComandos]);
  });
});

describe("gasto por foto", () => {
  test("usa a maior resolução, baixa do Telegram e envia a imagem em base64", async (t) => {
    const imagem = Buffer.from("conteudo-da-foto-do-recibo");
    const downloads = interceptarDownloads(t, () => new Response(imagem));
    const salvo = gasto({ source: "telegram_photo", amount: 245.7, description: "Supermercado" });
    backend.responder("POST /api/expenses/photo", { status: 201, corpo: salvo });

    await telegram.enviar(
      foto(ANA, [
        { file_id: "pequena", file_size: 1_000 },
        { file_id: "media", file_size: 20_000 },
        { file_id: "grande", file_size: 150_000 },
      ]),
    );

    const getFile = telegram.chamadas.find((chamada) => chamada.metodo === "getFile");
    assert.equal(getFile?.payload["file_id"], "grande");
    assert.deepEqual(downloads, [`https://api.telegram.org/file/bot${TOKEN}/photos/recibo.jpg`]);
    assert.deepEqual(backend.chamadas, [
      {
        metodo: "POST",
        caminho: "/api/expenses/photo",
        usuario: "1001",
        corpo: { imageBase64: imagem.toString("base64"), mimeType: "image/jpeg" },
      },
    ]);
    assert.deepEqual(telegram.textos(), [formatarGasto(salvo)]);
  });

  test("foto sem tamanho informado pelo Telegram ainda é processada", async (t) => {
    interceptarDownloads(t, () => new Response(Buffer.from("foto")));
    backend.responder("POST /api/expenses/photo", { status: 201, corpo: gasto({ source: "telegram_photo" }) });

    await telegram.enviar(foto(ANA, [{ file_id: "sem-tamanho" }]));

    assert.equal(backend.chamadas.length, 1);
  });

  test("foto acima de 10 MB é recusada sem baixar nem chamar o servidor", async (t) => {
    const downloads = interceptarDownloads(t, () => new Response("nunca"));

    await telegram.enviar(foto(ANA, [{ file_id: "enorme", file_size: 10 * 1024 * 1024 + 1 }]));

    assert.equal(telegram.chamadas.some((chamada) => chamada.metodo === "getFile"), false);
    assert.deepEqual(downloads, []);
    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.fotoGrande]);
  });

  test("Telegram sem caminho do arquivo responde com erro genérico", async (t) => {
    capturarErros(t);
    telegram.caminhoDoArquivo = undefined;

    await telegram.enviar(foto(ANA, [{ file_id: "sumiu", file_size: 1_000 }]));

    assert.equal(backend.chamadas.length, 0);
    assert.deepEqual(telegram.textos(), [mensagens.erroGenerico]);
  });
});
