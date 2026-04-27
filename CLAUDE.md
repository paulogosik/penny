# PennyBot — contexto para o Claude Code

## O que é o projeto

**PennyBot** é um assistente de finanças pessoais composto por três partes em repositórios separados:

- **`bot/`** — Bot do Telegram (Node.js) onde o usuário registra gastos (texto ou foto).
- **`backend/`** — API REST em Java (Spring Boot) responsável pela IA, persistência e regras de negócio.
- **`panel/`** — Painel admin web (ainda não implementado) para gerenciar renda, gastos fixos, categorias e dashboards.

## Visão do produto

1. O usuário envia ao bot uma **foto de recibo ou uma descrição de gasto** pelo Telegram.
2. O bot repassa ao **backend**, que usa IA para classificar o gasto e o persiste.
3. O **painel admin** permite:
   - Cadastrar e editar categorias de gasto.
   - Cadastrar renda mensal e gastos fixos recorrentes.
   - Visualizar dashboards e gráficos de gastos.
   - Editar ou excluir lançamentos cadastrados.

O **diferencial principal** é a IA para categorização automática de gastos — as categorias são gerenciadas pelo próprio usuário no painel.

## Estado atual (abril 2026)

### `bot/` — funcional mas mínimo
- Stack: **Node.js + TypeScript** (ESM), **grammy** v1.41.1, **dotenv**.
- `npm run dev` → `node --loader ts-node/esm src/bot.ts`
- Único arquivo de lógica: `src/bot.ts`.
- Implementado: `/start` e eco de mensagem de texto.
- **Não implementado:** handler de fotos, chamada ao backend, estrutura de pastas.

### `backend/` — não iniciado
- Repositório separado, ainda a ser criado.

### `panel/` — vazio
- Pasta reservada com `.gitkeep`. Stack ainda não definido.

---

## Arquitetura geral

```
Telegram → bot (Node.js) → backend (Java/Spring Boot) → banco de dados
                                     ↕
                                 Gemini AI
```

O bot é uma **camada fina**: recebe mensagens do Telegram, faz validações técnicas mínimas (mensagem não vazia, tamanho de foto aceitável) e chama o backend via HTTP. Toda lógica de negócio vive no backend.

---

## Planejamento: `bot/`

### Estrutura de pastas planejada (`bot/src/`)

```
bot/src/
├── bot.ts               # orquestrador puro — só registra handlers
├── types.ts             # tipos compartilhados (req/res shapes da API)
├── handlers/
│   ├── start.ts         # handler do comando /start
│   ├── expense.ts       # handler de mensagem de texto
│   └── photo.ts         # handler de foto de recibo
└── services/
    └── api.ts           # cliente HTTP que chama o backend Java
```

### Responsabilidades do bot
- `handlers/` — extraem dados da mensagem do Telegram e chamam `services/api.ts`.
- `services/api.ts` — único ponto de contato com o backend; encapsula URLs e serialização.
- `types.ts` — define os formatos de request/response trocados com o backend.
- Sem lógica de IA, sem acesso a banco de dados — tudo isso fica no backend.

---

## Planejamento: `backend/` (repositório separado)

### Stack definida
- **Linguagem:** Java
- **Framework:** Spring Boot 3.x
- **Módulos Spring:** Spring Web (REST API), Spring Data JPA (ORM)
- **Build:** Maven
- **Banco de dados:** a definir (PostgreSQL é o candidato principal)
- **IA:** Google Gemini `gemini-1.5-flash` via chamada HTTP direta à API REST

### Estrutura de pastas planejada

```
src/main/java/com/penny/
├── PennyApplication.java          # entry point (@SpringBootApplication)
├── controller/
│   └── ExpenseController.java     # endpoints REST
├── service/
│   ├── ExpenseService.java        # orquestra o fluxo de um gasto
│   └── GeminiService.java         # integração com Gemini AI
├── repository/
│   └── ExpenseRepository.java     # Spring Data JPA (acesso ao banco)
├── model/
│   └── Expense.java               # entidade JPA (tabela de gastos)
└── dto/
    ├── ExpenseRequest.java        # payload recebido do bot
    └── ExpenseResponse.java       # resposta enviada ao bot
```

### Endpoints planejados

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/expenses/text` | Recebe texto, classifica e salva |
| `POST` | `/api/expenses/photo` | Recebe imagem base64, classifica e salva |

### Fluxo de uma requisição
1. Bot chama `POST /api/expenses/text` com `{ "text": "gastei 45 reais no mercado" }`.
2. `ExpenseController` recebe e delega ao `ExpenseService`.
3. `ExpenseService` chama `GeminiService` para classificar o gasto.
4. `ExpenseService` salva via `ExpenseRepository`.
5. Retorna `ExpenseResponse` com categoria, valor, data e descrição.

---

## Decisões de arquitetura

- **Provider de IA: Google Gemini** — modelo `gemini-1.5-flash` para texto e fotos de recibo.
  - Chamada via HTTP direto à API REST (sem SDK Java dedicado, para simplicidade).
  - Saída estruturada com `responseMimeType: "application/json"`.
  - Motivo: suporte nativo a imagens, tier gratuito generoso (1.500 req/dia), custo baixo para uso pessoal.
- **Backend em Java + Spring Boot** — escolhido por valor de mercado e documentação extensa.
  - Spring Boot 3.x com Maven; padrão Controller → Service → Repository.
  - Repositório separado do bot.
- **Bot como camada fina** — validações técnicas apenas (não de negócio); lógica no backend.
- **Repositórios separados** — bot e backend vivem em repos distintos.

## Decisões de estrutura
- `LICENSE`, `.gitignore` e `README.md` ficam na raiz de cada repositório.
- Variáveis de ambiente do bot ficam em `bot/.env` (baseado em `bot/.env.example`).
- Variáveis do backend ficam em `application.properties` / variáveis de ambiente.

---

## Próximos passos

### Bot
- Criar estrutura de pastas (`handlers/`, `services/`).
- Implementar `services/api.ts` com cliente HTTP para o backend.
- Implementar handlers `expense.ts` e `photo.ts`.

### Backend
- Criar repositório separado e inicializar projeto com Spring Initializr.
- Implementar primeiro endpoint (`POST /api/expenses/text`) com retorno fixo para validar o ciclo.
- Integrar Gemini AI no `GeminiService`.
- Definir banco de dados e implementar `ExpenseRepository`.
- Integrar persistência no `ExpenseService`.

### Geral
- Definir o banco de dados (PostgreSQL é o candidato).
- Definir como o bot descobre a URL do backend (variável de ambiente `BACKEND_URL`).

---

## Instruções para o Claude

- Provider de IA definido: **Google Gemini** (`gemini-1.5-flash`). Não sugerir outros providers.
- O bot usa ESM (`"type": "module"`); manter compatibilidade ao adicionar dependências.
- O painel ainda não tem stack definido — não assumir framework sem perguntar.
- O backend usa **Spring Boot 3.x com Maven**. Não sugerir outros frameworks Java.
- Não adicionar comentários ao código existente a menos que explicitamente pedido.

---

## Ritual de encerramento (`/checkpoint`)

Ao receber o comando `/checkpoint`, atualizar este arquivo com o resumo da sessão:
- **Estado atual** — o que mudou no que foi implementado.
- **Decisões de arquitetura** — novas decisões ou mudanças.
- **Bibliotecas e dependências** — adições com motivo da escolha.
- **Padrões de código** — convenções definidas.
- **Problemas e soluções** — problemas encontrados e como foram resolvidos.
- **Próximos passos** — o que ficou planejado.

Incluir apenas seções com conteúdo novo. Usar português e bullet points concisos.
