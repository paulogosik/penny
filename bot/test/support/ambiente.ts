import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { AddressInfo } from "node:net";
import type { TestContext } from "node:test";
import { inspect } from "node:util";
import type { Bot } from "grammy";
import type { Update, User, UserFromGetMe } from "grammy/types";
import { createBot } from "../../src/app.js";

export const TOKEN = "123456:TOKEN-FALSO-SO-PARA-TESTE";

const BOT_INFO: UserFromGetMe = {
  id: 42,
  is_bot: true,
  first_name: "Penny",
  username: "penny_teste_bot",
  can_join_groups: true,
  can_read_all_group_messages: false,
  supports_inline_queries: false,
  can_connect_to_business: false,
  has_main_web_app: false,
  has_topics_enabled: false,
  allows_users_to_create_topics: false,
};

export const ANA: User = { id: 1001, is_bot: false, first_name: "Ana", last_name: "Souza" };
export const BRUNO: User = { id: 2002, is_bot: false, first_name: "Bruno" };
export const GRUPO = { id: -100777, type: "supergroup", title: "Família" };

export interface ChamadaBackend {
  metodo: string;
  caminho: string;
  usuario: string | undefined;
  corpo: unknown;
}

export interface RespostaBackend {
  status: number;
  corpo?: unknown;
  html?: string;
}

type Responder = (chamada: ChamadaBackend) => RespostaBackend;

export class BackendFalso {
  readonly chamadas: ChamadaBackend[] = [];
  private readonly rotas = new Map<string, Responder>();
  private readonly servidor: Server = createServer((req, res) => void this.atender(req, res));

  async iniciar(): Promise<string> {
    await new Promise<void>((resolve) => this.servidor.listen(0, "127.0.0.1", resolve));
    const { port } = this.servidor.address() as AddressInfo;
    return `http://127.0.0.1:${port}`;
  }

  parar(): Promise<void> {
    return new Promise((resolve) => this.servidor.close(() => resolve()));
  }

  limpar(): void {
    this.chamadas.length = 0;
    this.rotas.clear();
  }

  responder(rota: string, resposta: RespostaBackend | Responder): void {
    this.rotas.set(rota, typeof resposta === "function" ? resposta : () => resposta);
  }

  private async atender(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const partes: Buffer[] = [];
    for await (const parte of req) {
      partes.push(parte as Buffer);
    }
    const bruto = Buffer.concat(partes).toString("utf8");
    const usuario = req.headers["x-telegram-user-id"];
    const chamada: ChamadaBackend = {
      metodo: req.method ?? "",
      caminho: req.url ?? "",
      usuario: Array.isArray(usuario) ? usuario.join(",") : usuario,
      corpo: bruto ? JSON.parse(bruto) : undefined,
    };
    this.chamadas.push(chamada);

    const responder = this.rotas.get(`${chamada.metodo} ${chamada.caminho}`);
    const resposta = responder?.(chamada) ?? {
      status: 404,
      corpo: { code: "NOT_FOUND", message: "Rota não configurada no teste." },
    };
    if (resposta.html !== undefined) {
      res.writeHead(resposta.status, { "Content-Type": "text/html" }).end(resposta.html);
      return;
    }
    res.writeHead(resposta.status, { "Content-Type": "application/json" }).end(JSON.stringify(resposta.corpo ?? {}));
  }
}

export interface ChamadaTelegram {
  metodo: string;
  payload: Record<string, unknown>;
}

export class TelegramFalso {
  readonly chamadas: ChamadaTelegram[] = [];
  readonly bot: Bot;
  caminhoDoArquivo: string | undefined = "photos/recibo.jpg";

  constructor() {
    this.bot = createBot(TOKEN, { botInfo: BOT_INFO });
    this.bot.api.config.use(async (_prev, method, payload) => {
      this.chamadas.push({ metodo: method, payload: payload as Record<string, unknown> });
      return { ok: true, result: this.resultado(method, payload) as never };
    });
  }

  enviar(update: Update): Promise<void> {
    return this.bot.handleUpdate(update);
  }

  mensagens(): ChamadaTelegram[] {
    return this.chamadas.filter((chamada) => chamada.metodo === "sendMessage");
  }

  textos(): string[] {
    return this.mensagens().map((chamada) => String(chamada.payload["text"]));
  }

  private resultado(method: string, payload: unknown): unknown {
    if (method === "getFile") {
      const fileId = (payload as { file_id: string }).file_id;
      return { file_id: fileId, file_unique_id: fileId, ...(this.caminhoDoArquivo && { file_path: this.caminhoDoArquivo }) };
    }
    if (method === "sendMessage") {
      return { message_id: this.chamadas.length, date: 0, chat: { id: 0, type: "private" } };
    }
    return true;
  }
}

export function pedeContato(chamada: ChamadaTelegram | undefined): boolean {
  const teclado = chamada?.payload["reply_markup"] as { keyboard?: { request_contact?: boolean }[][] } | undefined;
  return teclado?.keyboard?.[0]?.[0]?.request_contact === true;
}

let proximoId = 1;

function mensagem(de: User, conteudo: Record<string, unknown>, chat?: object): Update {
  const id = proximoId++;
  const conversa = chat ?? { id: de.id, type: "private", first_name: de.first_name };
  return { update_id: id, message: { message_id: id, date: 1_760_000_000, chat: conversa, from: de, ...conteudo } } as unknown as Update;
}

export function texto(de: User, conteudo: string, chat?: object): Update {
  const comando = conteudo.split(" ")[0] ?? "";
  const entidades = conteudo.startsWith("/") ? { entities: [{ type: "bot_command", offset: 0, length: comando.length }] } : {};
  return mensagem(de, { text: conteudo, ...entidades }, chat);
}

export function contato(de: User, dados: { phone_number: string; first_name: string; user_id?: number }, chat?: object): Update {
  return mensagem(de, { contact: dados }, chat);
}

export function foto(de: User, tamanhos: { file_id: string; file_size?: number }[], chat?: object): Update {
  const photo = tamanhos.map((tamanho, i) => ({ file_unique_id: tamanho.file_id, width: 90 * (i + 1), height: 90 * (i + 1), ...tamanho }));
  return mensagem(de, { photo }, chat);
}

export function interceptarDownloads(t: TestContext, resposta: () => Response): string[] {
  const urls: string[] = [];
  const fetchOriginal = globalThis.fetch;
  t.mock.method(globalThis, "fetch", (entrada: Parameters<typeof fetch>[0], init?: RequestInit) => {
    const url = entrada instanceof Request ? entrada.url : String(entrada);
    if (!url.startsWith("https://api.telegram.org/file/")) {
      return fetchOriginal(entrada, init);
    }
    urls.push(url);
    return Promise.resolve(resposta());
  });
  return urls;
}

export function capturarErros(t: TestContext): () => string {
  const espiao = t.mock.method(console, "error", () => {});
  return () => espiao.mock.calls.map((chamada) => chamada.arguments.map((arg) => inspect(arg)).join(" ")).join("\n");
}

export async function usarBackendForaDoAr(t: TestContext): Promise<void> {
  const servidor = createServer();
  await new Promise<void>((resolve) => servidor.listen(0, "127.0.0.1", resolve));
  const { port } = servidor.address() as AddressInfo;
  await new Promise<void>((resolve) => servidor.close(() => resolve()));

  const original = process.env.BACKEND_URL;
  process.env.BACKEND_URL = `http://127.0.0.1:${port}`;
  t.after(() => {
    process.env.BACKEND_URL = original;
  });
}
