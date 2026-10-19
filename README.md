# PennyBot

Assistente de finanças pessoais: você registra gastos pelo **Telegram** (texto ou foto de recibo), uma **IA** classifica automaticamente e (planejado) um **painel web** mostra dashboards e gestão.

```
Telegram → bot (Node.js) → backend (Java/Spring Boot) → PostgreSQL (Supabase)
                                   ↕
                               Gemini AI
```

## Estrutura do repositório

| Pasta | Descrição |
|-------|-----------|
| [`bot/`](./bot/) | Bot do Telegram (Node.js + TypeScript + [Grammy](https://grammy.dev/)). |
| [`backend/`](./backend/) | API REST em Java (Spring Boot) — classificação por IA e persistência. |
| [`compose.yaml`](./compose.yaml) | Orquestração dos serviços com Docker Compose. |

Um **painel admin** (renda, gastos fixos, categorias, dashboards) está planejado, mas ainda não foi iniciado. A API que ele vai consumir já está pronta no backend.

---

## Rodando com Docker (recomendado)

Cada serviço tem o próprio `Dockerfile` (build multi-stage) e o `compose.yaml` na raiz sobe tudo junto, já com a rede interna entre bot e backend.

```bash
cp bot/.env.example bot/.env           # defina BOT_TOKEN
cp backend/.env.example backend/.env   # defina GEMINI_API_KEY e as variáveis DB_* do Supabase
docker compose up --build
```

- O backend fica exposto em `http://localhost:8080` e tem healthcheck em `/actuator/health`.
- O bot só sobe depois que o backend está saudável e fala com ele por `http://backend:8080` (rede interna do Compose).

Comandos úteis:

```bash
docker compose logs -f bot       # acompanhar os logs do bot
docker compose down              # derrubar tudo
```

---

## Bot (`bot/`)

Camada fina: recebe mensagens do Telegram, faz validações técnicas mínimas e repassa ao backend. Sem lógica de negócio e **sem árvore de comandos**: depois do `/start`, é só conversar.

**Funcionalidades**
- **Primeiro contato:** o `/start` cria a conta e pede o número de telefone pelo botão nativo do Telegram, explicando que ele serve para entrar no painel sem senha.
- Registro de gasto por **texto livre** ("uber ontem 23,50", "tênis de 600 em 6x no nubank").
- Registro de gasto por **foto de recibo**.
- Resposta com categoria, valor, data, cartão, fatura e parcelas; erros viram mensagens amigáveis.

**Como rodar sem Docker**
```bash
cd bot
cp .env.example .env   # defina BOT_TOKEN e BACKEND_URL
npm install
npm run dev
```

Mais detalhes em [`bot/README.md`](./bot/README.md).

---

## Backend (`backend/`)

API REST que classifica os gastos com IA e cuida de toda a regra de negócio. Padrão Controller → Service → Repository.

**Funcionalidades**
- **Gastos por texto e por foto de recibo**, classificados pelo Google Gemini (`gemini-flash-lite-latest`) com as categorias e os cartões do próprio usuário, a data no fuso dele e detecção de compras parceladas.
- **Cartões de crédito** com fatura calculada pelo dia de fechamento e vencimento, e **parcelamentos** distribuídos nas faturas seguintes.
- **Gastos fixos e renda mensal** gerados automaticamente todo mês.
- **Categorias** de gasto e de renda gerenciadas pelo usuário (com orçamento mensal), **metas** de economia e **dashboard** do mês.
- Banco **PostgreSQL no [Supabase](https://supabase.com/)**, com o schema versionado via **Flyway** em [`db/migration`](./backend/src/main/resources/db/migration/) e RLS ligado em todas as tabelas.

**API**

Todas as rotas identificam o usuário pelo cabeçalho `X-Telegram-User-Id` (provisório, até existir login no painel). Erros seguem o formato `{ "code", "message", "fields" }`.

| Rota | Uso |
|------|-----|
| `PUT /api/me` | Cadastro (cria as categorias padrão); idempotente |
| `PUT /api/me/phone` | Salva o telefone (obrigatório para registrar gastos) |
| `POST /api/expenses/text` · `POST /api/expenses/photo` | Registro de gasto pelo bot |
| `/api/expenses` | Listagem paginada com filtros (`from`, `to`, `categoryId`, `creditCardId`, `billingMonth`) e CRUD |
| `/api/installment-plans` | Compras parceladas |
| `/api/expense-categories` · `/api/income-categories` | Categorias |
| `/api/credit-cards` · `/api/credit-cards/{id}/statements/{yyyy-MM}` | Cartões e fatura do mês |
| `/api/incomes` | Rendas |
| `/api/recurring-expenses` · `/api/recurring-incomes` | Gastos fixos e renda fixa |
| `/api/goals` | Metas |
| `GET /api/dashboard?month=yyyy-MM` | Resumo do mês: totais, gastos por categoria vs. orçamento, faturas |
| `GET /actuator/health` | Healthcheck |

**Como rodar sem Docker**
```bash
cd backend
cp .env.example .env   # defina GEMINI_API_KEY e as variáveis DB_*
./mvnw spring-boot:run # sobe em http://localhost:8080
```

> Rode a partir da pasta `backend/`: o Spring lê o `.env` relativo ao diretório de execução. O `./mvnw` (Maven Wrapper) dispensa ter o Maven instalado.

**Exemplo**
```bash
curl -X PUT http://localhost:8080/api/me \
  -H 'X-Telegram-User-Id: 123' -H 'Content-Type: application/json' \
  -d '{"name":"Paulo"}'

curl -X PUT http://localhost:8080/api/me/phone \
  -H 'X-Telegram-User-Id: 123' -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"+5511987654321"}'

curl -X POST http://localhost:8080/api/expenses/text \
  -H 'X-Telegram-User-Id: 123' -H 'Content-Type: application/json' \
  -d '{"text":"comprei um tênis de 600 em 6x no nubank"}'
```

**Testes**
```bash
cd backend
./mvnw test   # precisa do Docker rodando: os testes sobem um PostgreSQL real com Testcontainers
```

---

## Requisitos

- **Docker** com Docker Compose — para rodar tudo junto.
- Para desenvolvimento sem Docker: **Node.js** 24+ (bot) e **Java** 21+ (backend).
- Token do Telegram via [@BotFather](https://t.me/BotFather) e chave do Gemini via [Google AI Studio](https://aistudio.google.com/apikey).

## Licença

Veja [LICENSE](./LICENSE).
