# PennyBot — contexto para o Claude Code

> Leia também o **[`MEMORY.md`](./MEMORY.md)**: diário do projeto (o que cada PR fez, problemas encontrados, pendências e decisões em aberto). Este arquivo guarda o que é **permanente**; o `MEMORY.md` guarda o **histórico e o momento atual**.

## O que é o projeto

**PennyBot** é um assistente de finanças pessoais organizado como **monorepo**, com três partes:

- **`bot/`** — Bot do Telegram (Node.js) onde o usuário registra gastos (texto ou foto).
- **`backend/`** — API REST em Java (Spring Boot) responsável pela IA, persistência e regras de negócio.
- **`panel/`** — Painel admin web (ainda não implementado) para gerenciar renda, gastos fixos, categorias e dashboards.

## Visão do produto

1. O usuário envia ao bot uma **foto de recibo ou uma descrição de gasto** pelo Telegram.
2. O bot repassa ao **backend**, que usa IA para classificar o gasto e o persiste.
3. O **painel admin** permite:
   - Cadastrar e editar categorias de gasto e de renda.
   - Cadastrar renda mensal e gastos fixos recorrentes.
   - Cadastrar cartões de crédito e acompanhar faturas e parcelamentos.
   - Definir orçamento por categoria e metas de economia.
   - Visualizar dashboards e gráficos de gastos.
   - Editar ou excluir lançamentos cadastrados.

O **diferencial principal** é a IA para categorização automática de gastos — as categorias são gerenciadas pelo próprio usuário no painel.

## Estado atual (outubro 2026)

### `backend/` — pronto para o bot e para o painel
- Stack: **Java 21 (target) + Spring Boot 3.5.16 + Maven**, Spring Web, Spring Data JPA, Flyway, Bean Validation, Actuator.
- Banco: **PostgreSQL no Supabase** (Session pooler), schema versionado com **Flyway** (`V1` schema inicial, `V2` controle de geração dos recorrentes, `V3` telefone do usuário) e **RLS ligado em todas as tabelas**.
- **Telefone obrigatório** para registrar gastos (`PUT /api/me/phone`; sem número, as rotas de gasto do bot respondem `PHONE_NUMBER_REQUIRED`).
- API completa para o bot (cadastro no `/start`, gasto por texto e por foto) e para o painel (CRUD de categorias, gastos, rendas, cartões, parcelamentos, recorrências, metas, fatura do cartão e dashboard). Ver tabela de endpoints abaixo.
- IA: Gemini **`gemini-flash-lite-latest`**, com categorias e cartões do próprio usuário, data no fuso do usuário, detecção de parcelas e cartão, e nova tentativa automática em erros temporários (429/5xx).
- Gastos fixos e renda mensal são **gerados automaticamente** (agendador de hora em hora + na subida da aplicação).
- **50 testes** automatizados (`./mvnw test`), incluindo integração contra PostgreSQL real via Testcontainers.

### `bot/` — integrado ao backend
- Stack: **Node.js + TypeScript** (ESM), **grammy** v1.41.1, **dotenv**.
- `npm run dev` (ts-node) para desenvolvimento; `npm run build` + `npm start` (ou Docker) para produção.
- **Conversa livre, sem árvore de comandos:** o único comando é o `/start` (enviado pelo botão Iniciar do Telegram); depois disso a pessoa só manda texto ou foto.
- Fluxo: `/start` cadastra a conta e pede o telefone com o botão nativo de compartilhar contato → número salvo → gastos por texto e foto, com resposta formatada (categoria, valor em R$, data, cartão/fatura, parcelas).
- Cada código de erro do backend vira uma mensagem amigável; `PHONE_NUMBER_REQUIRED` e `USER_NOT_REGISTERED` retomam o cadastro automaticamente.

### `panel/` — não iniciado
- Planejado; a pasta ainda não existe fisicamente. Stack não definido.

### Infra
- **Docker:** um `Dockerfile` multi-stage por serviço (`bot/`, `backend/`) e `compose.yaml` na raiz orquestrando tudo (`docker compose up --build`).
- **CI:** GitHub Actions (`.github/workflows/ci.yml`) em todo PR e push na `main`, com os jobs `backend` (`./mvnw verify`), `bot` (`npm ci` + build) e `docker` (build das duas imagens). A `main` exige os três verdes para merge.
- **CD:** ainda não existe — depende de escolher a hospedagem (ver `MEMORY.md`).

---

## Arquitetura geral

```
Telegram → bot (Node.js) → backend (Java/Spring Boot) → PostgreSQL (Supabase)
                                     ↕
                                 Gemini AI
```

O bot é uma **camada fina**: recebe mensagens do Telegram, faz validações técnicas mínimas (mensagem não vazia, tamanho de foto aceitável) e chama o backend via HTTP. Toda lógica de negócio vive no backend. O painel (futuro) também consome só o backend.

---

## `bot/`

### Estrutura (`bot/src/`)

```
bot/src/
├── bot.ts               # entrypoint: lê o .env, cria o bot e inicia o long polling
├── app.ts               # createBot(): registra os handlers (testável sem rede)
├── messages.ts          # textos, formatação do gasto e teclado de compartilhar número
├── types.ts             # contrato de req/res com o backend
├── handlers/
│   ├── start.ts         # /start
│   ├── onboarding.ts    # cadastro + pedido de telefone (reusado por vários handlers)
│   ├── contact.ts       # recebe o contato compartilhado e salva o telefone
│   ├── expense.ts       # gasto por texto
│   ├── photo.ts         # gasto por foto de recibo (baixa e converte para base64)
│   └── errors.ts        # código de erro do backend → mensagem para o usuário
└── services/
    └── api.ts           # cliente HTTP do backend (ApiError com status e code)
```

### Responsabilidades do bot
- `handlers/` — extraem dados da mensagem do Telegram e chamam `services/api.ts`.
- `services/api.ts` — único ponto de contato com o backend; encapsula URLs, o cabeçalho `X-Telegram-User-Id`, timeout (90s) e a serialização.
- `types.ts` — define os formatos de request/response trocados com o backend.
- Validações só técnicas (texto até 1000 caracteres, foto até 10 MB, contato pertencer a quem enviou). Sem lógica de IA, sem acesso a banco de dados — tudo isso fica no backend.

---

## `backend/`

### Stack
- **Linguagem:** Java (bytecode alvo **21 / LTS**; máquina de dev tem JDK 25)
- **Framework:** **Spring Boot 3.5.16** (Web, Data JPA, Validation, Actuator)
- **Banco:** PostgreSQL 17 no **Supabase**, migrations com **Flyway**
- **Build:** Maven, com **Maven Wrapper** (`./mvnw`) commitado
- **IA:** Google Gemini via chamada HTTP direta (`RestClient` do Spring)
- **Testes:** JUnit 5, MockMvc, Testcontainers (PostgreSQL)

### Estrutura (`backend/src/main/java/com/penny/`)

```
├── PennyApplication.java
├── config/       # Clock, agendamento, registro do resolver de usuário
├── web/          # @CurrentUser + resolver do cabeçalho X-Telegram-User-Id
├── controller/   # endpoints REST (finos: validam e delegam)
├── service/      # regras de negócio, Gemini, cálculo de fatura, geração de recorrentes
├── repository/   # Spring Data JPA (+ Specifications para filtros)
├── model/        # entidades JPA (uma por tabela) + enums de origem com conversores
├── dto/          # records de request/response
├── exception/    # ApiException e subclasses + handler global
└── scheduler/    # agendador dos lançamentos recorrentes
```

### Banco (10 tabelas)
`users` · `expense_categories` · `income_categories` · `expenses` · `incomes` · `recurring_expenses` · `recurring_incomes` · `credit_cards` · `installment_plans` · `goals`

### Endpoints

Todas as rotas (exceto `PUT /api/me`, que cadastra) exigem o cabeçalho **`X-Telegram-User-Id`**.

| Método | Rota | Uso |
|--------|------|-----|
| `PUT` / `GET` | `/api/me` | Cadastro idempotente (cria categorias padrão) / perfil |
| `PUT` | `/api/me/phone` | Salva o telefone (normalizado para E.164, único por conta) |
| `POST` | `/api/expenses/text` | Bot: texto → IA → gasto salvo (parcelado vira plano) |
| `POST` | `/api/expenses/photo` | Bot: foto de recibo (base64) → IA → gasto salvo |
| `GET` `POST` `PUT` `DELETE` | `/api/expenses` | Painel: listagem paginada com filtros e CRUD manual |
| `GET` `POST` `DELETE` | `/api/installment-plans` | Compras parceladas |
| `GET` `POST` `PUT` `DELETE` | `/api/expense-categories`, `/api/income-categories` | Categorias |
| `GET` `POST` `PUT` `DELETE` | `/api/credit-cards` | Cartões |
| `GET` | `/api/credit-cards/{id}/statements/{yyyy-MM}` | Fatura do mês |
| `GET` `POST` `PUT` `DELETE` | `/api/incomes` | Rendas |
| `GET` `POST` `PUT` `DELETE` | `/api/recurring-expenses`, `/api/recurring-incomes` | Gastos fixos e renda fixa |
| `GET` `POST` `PUT` `DELETE` | `/api/goals` | Metas |
| `GET` | `/api/dashboard?month=yyyy-MM` | Resumo do mês |
| `GET` | `/actuator/health` | Healthcheck |

Erros sempre no formato `{ code, message, fields? }`.

---

## Decisões de arquitetura

- **Provider de IA: Google Gemini**, modelo **`gemini-flash-lite-latest`** (decisão de outubro/2026).
  - Chamada via HTTP direto com o `RestClient` (sem SDK), chave no cabeçalho `x-goog-api-key` (não na URL).
  - Saída estruturada com `responseSchema`; `category` e `creditCard` são **enums com os valores do próprio usuário**.
  - Motivo da troca: em out/2026 toda a linha Flash principal (`gemini-flash-latest`, `3-flash-preview`, `3.5`/`3.6`/`3.7`/`3.8-flash`) respondia 503 por sobrecarga; a linha Flash-Lite estava estável. Alias continua evitando 404 quando o Google aposenta versões.
  - Nova tentativa automática (até 3, backoff 1s/2s) em 429/500/502/503/504; erros 4xx não são repetidos.
  - A IA fica atrás da interface `ExpenseClassifier` (implementação `GeminiExpenseClassifier`), o que permite um classificador falso nos testes.
  - A chamada à IA acontece **fora da transação** do banco (`TransactionTemplate` só para persistir), para não segurar conexão do pool (5 conexões) por até 60s.
- **Backend em Java + Spring Boot** — padrão Controller → Service → Repository.
- **Bot como camada fina** — validações técnicas apenas; lógica no backend.
- **Monorepo** — bot, backend e painel no mesmo repositório, em pastas irmãs.
- **Bytecode alvo Java 21** — o app roda normalmente no JVM 25 da máquina de dev; no Docker roda em JRE 21.
- **Docker:** um `Dockerfile` multi-stage **por serviço** + `compose.yaml` na raiz como orquestrador único. Container único rodando tudo foi descartado (anti-padrão: imagem com Node + Java, sem reinício/escala independentes).
- **Banco: PostgreSQL no Supabase, acessado só pelo backend.** O Supabase é usado como Postgres gerenciado; bot e painel nunca falam com o banco direto.
  - **RLS ligado em todas as tabelas, sem políticas:** bloqueia a API pública do Supabase (chave anon); o backend conecta como dono das tabelas e não é afetado.
  - Conexão pelo **Session pooler** (IPv4); a conexão direta do plano gratuito é só IPv6.
  - **Schema só por Flyway** (`backend/src/main/resources/db/migration`), nunca pelo painel do Supabase. `ddl-auto=validate`: o Hibernate valida as entidades contra o schema ao subir.
- **Modelo de dados:**
  - **Vários usuários desde o início**, todos nas mesmas tabelas com `user_id` (padrão multi-tenant por coluna).
  - **Gastos e rendas em tabelas separadas** (`expenses`/`incomes`), cada um com categorias e recorrências próprias — decisão do usuário, por organização.
  - **Categorias padrão** criadas no cadastro: 9 de gasto + **"Outros"** (`is_fallback`, não pode ser removida, só renomeada) e 1 de renda (**"Salário"**).
  - **"Remover" categoria/cartão:** se tem lançamentos, é **arquivado** (some da IA e do painel, histórico intacto); se nunca foi usado, é apagado.
  - **Parcelamento:** `installment_plans` + uma linha em `expenses` por parcela. A última parcela absorve a diferença de centavos. Parcela não é editada/removida sozinha — só o plano inteiro.
  - **Mês da fatura (`billing_month`)** = primeiro dia do **mês de vencimento**. Compra até o dia de fechamento entra na fatura que fecha naquele mês; vencimento cai no mesmo mês se `due_day > closing_day`, senão no seguinte.
  - **Cada gasto guarda `occurred_on` (data da compra) e `billing_month` (fatura).** O dashboard soma por `occurred_on`; a fatura soma por `billing_month`.
  - **Recorrentes viram lançamentos reais** quando o dia chega (dia 31 em mês de 30 dias → último dia). `last_generated_month` impede que um lançamento apagado pelo usuário seja recriado.
  - **Pagar a fatura não gera gasto** (as compras já foram registradas).
  - Valores em `numeric(12,2)` ↔ `BigDecimal`.
- **Identificação do usuário:** cabeçalho `X-Telegram-User-Id`, resolvido para `@CurrentUser User` num único `HandlerMethodArgumentResolver`. **Provisório** até existir login no painel — a troca fica concentrada nesse resolver.
- **Telefone (decisão de out/2026):**
  - **Obrigatório** e pedido no primeiro contato, explicando que serve para entrar no painel sem senha.
  - Capturado pelo botão nativo do Telegram (`request_contact`); o bot só aceita se `contact.user_id` for de quem enviou (impede compartilhar o contato de outra pessoa).
  - Guardado normalizado em **E.164** (`+5511987654321`), **único** por conta; a chave interna continua sendo o UUID (o número pode mudar — é só compartilhar de novo).
  - O ID do Telegram continua sendo como o bot identifica a pessoa em cada mensagem (o Telegram não envia o telefone nas mensagens).
- **CI no GitHub Actions** (decisão de out/2026): grátis porque o repositório é público; jobs paralelos e sem segredos (classificador falso + Testcontainers); actions fixadas por versão principal (`checkout@v7`, `setup-java@v6`, `setup-node@v7`, `upload-artifact@v7`); `permissions: contents: read`; runs antigos do mesmo PR são cancelados. Proteção da `main` exige `backend`, `bot` e `docker` verdes, sem exigir review (o dono não pode aprovar o próprio PR) e permitindo bypass do admin.
- **Login do painel (planejado, não implementado):** a pessoa digita o telefone no site → o backend manda um **código de 6 dígitos pelo Telegram** (via Bot API, `sendMessage` no chat da pessoa) → ela digita o código e entra. Código com validade de 5 min, uso único, até 5 tentativas, limite de pedidos e guardado com hash. Telefone sozinho nunca libera acesso. Sem número cadastrado, o site manda a pessoa abrir o bot.
- **Fuso horário por usuário** (`users.timezone`, padrão `America/Sao_Paulo`): "hoje" é calculado no fuso do usuário com um `Clock` injetável (o container roda em UTC).

## Decisões de estrutura
- `LICENSE` e `README.md` (visão geral) na raiz; cada subprojeto tem seu `README.md`.
- `.gitignore` da raiz é próprio do monorepo; o backend tem também o seu (`backend/.gitignore`).
- Variáveis de ambiente: `bot/.env` (`BOT_TOKEN`, `BACKEND_URL`) e `backend/.env` (`GEMINI_API_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), com templates `.env.example`.
- **`DB_URL` no formato JDBC, sem credenciais:** `jdbc:postgresql://<host-do-session-pooler>:5432/postgres?sslmode=require`; usuário e senha só em `DB_USERNAME`/`DB_PASSWORD`.
- O Spring lê `backend/.env` via `spring.config.import=optional:file:.env[.properties]` (rodar a partir de `backend/`). No Docker, o Compose injeta as variáveis com **`env_file` em `format: raw`** (sem interpolar `$`).
- Testes usam o profile `test` (`src/test/resources/application-test.properties`), com agendador desligado.

---

## Bibliotecas e dependências
- **Spring Boot 3.5.16:** `starter-web`, `starter-data-jpa`, `starter-validation`, `starter-actuator`, `starter-test`.
- **Flyway** (`flyway-core` + `flyway-database-postgresql`): schema versionado e aplicado na subida.
- **Driver PostgreSQL** (`org.postgresql:postgresql`).
- **Actuator:** expõe `/actuator/health` para o healthcheck do Docker (o bot só sobe com o backend saudável).
- **Testcontainers** (`spring-boot-testcontainers`, `junit-jupiter`, `postgresql`): testes de integração contra Postgres 17 real (requer Docker).
- **Maven Wrapper (`./mvnw`)** commitado.
- Sem SDK do Gemini e sem lib de `.env`: `RestClient` + Jackson + `spring.config.import`.

## Padrões de código
- **Sem comentários no código**, em nenhum arquivo (inclusive testes e configs). Pendências ficam no `MEMORY.md`, não em `TODO` no código.
- **Commits e PRs só no nome do usuário:** nunca adicionar `Co-Authored-By` nem "Generated with Claude Code".
- **Bot:**
  - Imports relativos em ESM/`nodenext` usam extensão `.js`.
  - Todo texto mostrado ao usuário fica em `messages.ts`; handlers não montam mensagens soltas.
  - Erros da API sempre passam por `responderErro()` (decide pela `code`, nunca pelo texto).
  - Valores com `Intl.NumberFormat("pt-BR", BRL)`, datas como `dd/mm/aaaa`, mês da fatura por extenso.
- **Backend:**
  - DTOs são **Java records**, com `static from(entidade)` nos de resposta.
  - Controllers finos: `@Valid` no corpo, `@CurrentUser User` para o usuário, delegam ao service.
  - Services com `@Transactional` (`readOnly` nas leituras); o mapeamento para DTO acontece **dentro** da transação (`open-in-view=false`).
  - Toda busca por id é escopada ao usuário (`findByIdAndUser`); recurso de outro usuário responde 404.
  - Erros de negócio via `ApiException` e subclasses (`NotFoundException` 404, `ConflictException` 409, `BusinessRuleException` 422, `GeminiException` 502), sempre com `code` estável em inglês e `message` em português.
  - Enums gravados como texto minúsculo no banco via `AttributeConverter` e serializados com `@JsonValue`.
  - Colunas `smallint` mapeadas como `Integer` com `@JdbcTypeCode(SqlTypes.SMALLINT)`.
  - Datas "de hoje" sempre via `Clock` injetado + fuso do usuário, nunca `LocalDate.now()` puro.
- **Testes:** classe base `IntegrationTest` (MockMvc + Testcontainers + `FakeExpenseClassifier` + `MutableClock`); cada teste cria seu próprio usuário com ID único em vez de limpar o banco.
- **CI:** testes nunca podem depender de segredos ou serviços externos reais (Gemini, Supabase, Telegram) — senão quebram no GitHub Actions. Um PR só vai para merge com o CI verde.

## Problemas e soluções
- **JDK 25 na máquina x suporte do Spring Boot:** compilamos mirando **Java 21 (LTS)**.
- **`gemini-1.5-flash` descontinuado (404) e linha 2.5 bloqueada para chaves novas:** migramos para aliases. Em out/2026 a linha Flash principal ficou sobrecarregada (503) → `gemini-flash-lite-latest` + nova tentativa automática.
- **Spring Boot não lê `.env` nativamente:** `spring.config.import=optional:file:.env[.properties]`.
- **Fish faz glob de `[...]` e `^` em one-liners:** scripts de teste em arquivo no scratchpad.
- **Docker Desktop reinicia sozinho ao atualizar:** um `docker` falhando logo após o build pode ser isso; esperar o daemon voltar.
- **Container em UTC gravava "hoje" com a data de amanhã após as 21h:** fuso por usuário + `Clock` injetável.
- **Supabase: conexão direta é só IPv6:** usar o Session pooler (IPv4).
- **Senha com `$` chegava cortada no container:** o Compose interpola `$` no `env_file` → `format: raw`.
- **`DB_URL` colado no formato `postgresql://usuario:senha@host`:** o Spring exige JDBC sem credenciais; ao validar o `.env`, separar pelo **último** `@` (senhas podem conter `@`) e nunca imprimir valores.
- **`ALTER TABLE flyway_schema_history` dentro de migration trava** (o Flyway mantém a tabela em uso) → RLS da tabela do Flyway via callback `afterMigrate.sql`.
- **`docker compose logs` mostra logs de execuções antigas** se o container só reiniciou: usar `--force-recreate` e `--since`.
- **Backend em loop de falha de senha** pode levar o Supabase a bloquear o IP: parar o container (`docker compose stop backend`) assim que o erro aparecer.

---

## Próximos passos
Detalhes e status em [`MEMORY.md`](./MEMORY.md).
- **Painel (segundo momento):** definir stack; login com código pelo Telegram; consumir a API existente.
- **CD:** escolher hospedagem (bot precisa de processo sempre ligado), publicar imagens no GHCR e fazer deploy a cada merge na `main`.
- **Backend:** rotas do login por código + sessão do painel (trocar o resolver de usuário), chave de API entre bot e backend, recibos no Supabase Storage (`receipt_path`), deploy.
- **Bot:** hoje cartões só são criados pela API (o painel ainda não existe), então compras parceladas pelo bot dependem disso.

---

## Instruções para o Claude

- Provider de IA definido: **Google Gemini** (modelo atual: `gemini-flash-lite-latest`). Não sugerir outros providers.
- **Monorepo:** bot, backend e painel vivem no mesmo repositório (pastas irmãs). Não sugerir separar em repositórios distintos.
- O bot usa ESM (`"type": "module"`); manter compatibilidade ao adicionar dependências.
- O painel ainda não tem stack definido — não assumir framework sem perguntar.
- O backend usa **Spring Boot 3.5.16 com Maven**, bytecode alvo **Java 21**; DTOs como Java records. Não sugerir outros frameworks Java.
- **Não escrever comentários** em nenhum código; não remover comentários existentes sem pedido explícito.
- Mudanças de schema **sempre** por nova migration Flyway (`V<n>__descricao.sql`), nunca editando uma já aplicada.
- Nunca imprimir valores de `.env` (senhas, tokens, chaves) em comandos ou respostas.
- Commits e PRs **sem** `Co-Authored-By` e sem menção ao Claude; trabalhar em branch e abrir PR para o usuário mergear.
- **A cada PR criado:** atualizar este `CLAUDE.md` (se algo permanente mudou) e o **`MEMORY.md`** (sempre), e espelhar as mudanças no `AGENTS.md`.

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
