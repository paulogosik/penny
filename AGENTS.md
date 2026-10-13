# PennyBot — contexto para o Codex

## O que é o projeto

**PennyBot** é um assistente de finanças pessoais organizado como **monorepo**, com três partes:

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

## Estado atual (julho 2026)

### `bot/` — estrutura completa (lado Telegram)
- Stack: **Node.js + TypeScript** (ESM), **grammy** v1.41.1, **dotenv**.
- `npm run dev` → `node --loader ts-node/esm src/bot.ts`
- Estrutura implementada: `bot.ts` (orquestrador), `types.ts`, `handlers/{start,expense,photo}.ts`, `services/api.ts`.
- Implementado: `/start`, handler de texto e handler de foto (baixa a foto do Telegram e converte para base64).
- Chamada ao backend está como **TODO (fase 2)** em `services/api.ts`; por enquanto lança `BackendIndisponivelError` e os handlers respondem que a categorização vem em seguida.

### `backend/` — classificação por IA funcionando (passo 3 concluído)
- Stack: **Java 21 (target) + Spring Boot 3.5.16 + Maven**; só **Spring Web** por enquanto.
- Estrutura: `PennyApplication`, `controller/ExpenseController`, `service/{ExpenseService,GeminiService}`, `exception/{GeminiException,ApiExceptionHandler}`, `dto/{ExpenseRequest,ExpenseResponse,ErrorResponse}` (Java records), `application.properties` (porta 8080).
- `POST /api/expenses/text` agora **classifica de verdade** via Gemini: `ExpenseController` → `ExpenseService` → `GeminiService` (chamada HTTP ao Gemini com saída JSON estruturada). Validado ponta a ponta com `curl` (categoria, valor e data extraídos corretamente, inclusive datas relativas como "ontem").
- Chave do Gemini lida de `backend/.env` via `spring.config.import` (sem lib externa). Modelo: **`gemini-flash-latest`**.
- **Persistência** ainda **não implementada** (marcada com `TODO` no `ExpenseService`); `POST /api/expenses/photo` ainda não existe.

### `panel/` — não iniciado
- Planejado; a pasta ainda não existe fisicamente. Stack não definido.

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

## Planejamento: `backend/`

### Stack definida
- **Linguagem:** Java (bytecode alvo **21 / LTS**; máquina de dev tem JDK 25)
- **Framework:** **Spring Boot 3.5.16** (linha 3.x)
- **Módulos Spring:** Spring Web (REST API) já incluído; Spring Data JPA (ORM) entra na fase de persistência
- **Build:** Maven, com **Maven Wrapper** (`./mvnw`) commitado
- **Banco de dados:** a definir (PostgreSQL é o candidato principal)
- **IA:** Google Gemini `gemini-flash-latest` via chamada HTTP direta à API REST (`RestClient` do Spring)

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

- **Provider de IA: Google Gemini** — modelo `gemini-flash-latest` para texto e fotos de recibo.
  - Chamada via HTTP direto à API REST com o `RestClient` do Spring (sem SDK Java dedicado, para simplicidade).
  - Saída estruturada com `responseMimeType: "application/json"` **+ `responseSchema`** (força o shape `{category, amount, date, description}`).
  - Motivo: suporte nativo a imagens, tier gratuito generoso, custo baixo para uso pessoal.
  - **Modelo é um alias (`gemini-flash-latest`), não uma versão fixa** — decisão de julho/2026 após o `gemini-1.5-flash` ser descontinuado. O alias sempre aponta pro flash estável atual e **não dá 404 quando o Google aposenta um modelo**. Trade-off aceito: o comportamento pode mudar quando o Google atualiza o alias. Alternativas fixas que funcionam na chave de dev: `gemini-3-flash-preview`, `gemini-3.5-flash`.
- **Backend em Java + Spring Boot** — escolhido por valor de mercado e documentação extensa.
  - Spring Boot 3.5.16 com Maven; padrão Controller → Service → Repository.
- **Bot como camada fina** — validações técnicas apenas (não de negócio); lógica no backend.
- **Monorepo** (mudança em julho/2026) — bot, backend e painel vivem no **mesmo repositório**, em pastas irmãs (`bot/`, `backend/`, `panel/`). Substitui a decisão anterior de repositórios separados.
- **Bytecode alvo Java 21** — o suporte oficial do Spring Boot ao Java 25 (instalado na máquina) ainda é recente; compilar mirando o LTS 21 evita surpresas. O app roda normalmente no JVM 25.
- **Ordem de construção do backend** — primeiro o esqueleto que roda (endpoint com retorno fixo), depois a IA (Gemini) e a **persistência/banco por último** — o schema é consequência do que a IA extrai, não o ponto de partida.

## Decisões de estrutura
- `LICENSE` e `README.md` (visão geral, uma seção por subprojeto) ficam na raiz do monorepo; cada subprojeto tem seu próprio `README.md`.
- `.gitignore` do backend fica em `backend/.gitignore` (ignora `target/` etc.).
- Variáveis de ambiente do bot ficam em `bot/.env` (baseado em `bot/.env.example`); inclui `BACKEND_URL`.
- Variáveis do backend ficam em `application.properties` / variáveis de ambiente.
- **`backend/.env`** (com `GEMINI_API_KEY`) é lido nativamente pelo Spring via `spring.config.import=optional:file:.env[.properties]` — sem lib `dotenv`. Ignorado no `.gitignore`; template versionado em `backend/.env.example`. Variável de ambiente do sistema tem prioridade sobre o `.env`. ⚠️ Rodar o backend a partir da pasta `backend/` (caminho `file:.env` é relativo ao diretório de execução).

---

## Bibliotecas e dependências (adicionadas em julho/2026)
- **Backend — Spring Boot 3.5.16** (`spring-boot-starter-web`, `spring-boot-starter-test`): versão real mais recente da linha 3.x confirmada no Maven Central; só o starter web por enquanto (JPA entra na fase de persistência).
- **Maven Wrapper (`./mvnw`)** commitado no backend: permite rodar sem Maven instalado e fixa a versão do Maven para todos.
- **Passo 3 não adicionou dependência nova:** a chamada HTTP ao Gemini usa o `RestClient` do Spring e o parse usa o Jackson (`ObjectMapper`/`JsonNode`) — ambos já vêm do `spring-boot-starter-web`. Idem para leitura do `.env` (`spring.config.import`, recurso nativo). Fiel à filosofia de simplicidade / sem SDK dedicado.

## Padrões de código
- **Bot:** imports relativos em ESM/`nodenext` usam extensão `.js` (ex.: `./services/api.js`), mesmo apontando para arquivos `.ts`.
- **Bot:** o ponto de integração com o backend (fase 2) fica isolado em `services/api.ts`, marcado com `TODO` e lançando `BackendIndisponivelError` enquanto não existe.
- **Backend:** DTOs são **Java records** (`ExpenseRequest`, `ExpenseResponse`).
- **Backend:** pontos ainda não implementados ficam marcados com `// TODO — passo N` no lugar exato (ex.: persistência no `ExpenseService`, categorias vindas do banco no `GeminiService`).
- **Backend:** erros de integração com o Gemini usam exceção própria (`GeminiException`) tratada num `@RestControllerAdvice` (`ApiExceptionHandler`) que devolve **HTTP 502** com `ErrorResponse` (`{ error }`) — o bot recebe um JSON de erro limpo em vez de stacktrace.
- **Backend:** o `GeminiService` retorna direto um `ExpenseResponse` (sem camada de mapeamento extra enquanto não há persistência); a lista de categorias fica hardcoded no service com `TODO` marcando que virá do banco.
- Comentários `TODO` de fase/integração são permitidos (foram pedidos explicitamente); a regra de "não comentar código existente" segue valendo para o resto.

## Problemas e soluções
- **JDK 25 na máquina x suporte do Spring Boot:** o JDK instalado é o 25, mas o suporte oficial ainda é recente → compilamos mirando o bytecode **Java 21 (LTS)**; o app roda normalmente no JVM 25.
- **`amount` como `double` no `ExpenseResponse`:** ok para o stub, mas para valores monetários o ideal é migrar para `BigDecimal` quando entrar a persistência.
- **README linkava `panel/` inexistente:** a pasta não existe fisicamente; o link foi removido e o painel virou uma menção de "planejado".
- **`gemini-1.5-flash` descontinuado (404):** o modelo saiu do ar. Investigando a chave via `ListModels` + testes de `generateContent`: os modelos 2.0 dão `429 RESOURCE_EXHAUSTED` e os 2.5 estão **bloqueados para chaves novas** (404 "no longer available to new users"). Solução: adotar o alias **`gemini-flash-latest`** (testado, funciona ponta a ponta). ⚠️ Atenção a `429` nos modelos 2.0 caso o free tier se esgote.
- **Spring Boot não lê `.env` nativamente como o bot:** resolvido com `spring.config.import=optional:file:.env[.properties]` (sem lib) em vez de setar a variável no shell a cada boot.
- **Fish faz glob de `[...]` e `^` em one-liners:** ao rodar scripts de teste, comandos Python inline quebravam no shell; solução foi escrever os scripts em arquivo no scratchpad e rodar com `python3 arquivo.py`.

---

## Próximos passos

### Bot
- ✅ Estrutura de pastas, handlers (`start`/`expense`/`photo`) e `services/api.ts` criados.
- Fase 2: implementar a chamada HTTP real ao backend em `services/api.ts` (hoje é `TODO`).
- Considerar extrair o `formatarGasto` duplicado (`expense.ts` e `photo.ts`) para um único lugar.

### Backend
- ✅ Scaffold com `POST /api/expenses/text` (retorno fixo) rodando e validado.
- ✅ **Passo 3:** `GeminiService` (classificação com saída JSON estruturada) e `ExpenseService` implementados e validados; controller delega ao service.
- Implementar `POST /api/expenses/photo` (imagem base64) — próximo passo natural.
- Depois: definir banco, entidade `Expense`, `ExpenseRepository` e integrar persistência (remover o `TODO` do `ExpenseService`).
- Futuro: categorias do `GeminiService` deixarem de ser hardcoded e virem do banco (geridas no painel).

### Geral
- Definir o banco de dados (PostgreSQL é o candidato).
- Avaliar `BigDecimal` para valores monetários na fase de persistência.

---

## Instruções para o Codex

- Provider de IA definido: **Google Gemini** (modelo atual: `gemini-flash-latest`). Não sugerir outros providers.
- **Monorepo:** bot, backend e painel vivem no mesmo repositório (pastas irmãs). Não sugerir separar em repositórios distintos.
- O bot usa ESM (`"type": "module"`); manter compatibilidade ao adicionar dependências.
- O painel ainda não tem stack definido — não assumir framework sem perguntar.
- O backend usa **Spring Boot 3.5.16 com Maven**, bytecode alvo **Java 21**; DTOs como Java records. Não sugerir outros frameworks Java.
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
