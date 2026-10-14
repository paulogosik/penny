# PennyBot

Assistente de finanças pessoais: você registra gastos pelo **Telegram** (texto ou foto de recibo), uma **IA** classifica automaticamente e (planejado) um **painel web** mostra dashboards e gestão.

```
Telegram → bot (Node.js) → backend (Java/Spring Boot) → banco de dados
                                   ↕
                               Gemini AI
```

## Estrutura do repositório

| Pasta | Descrição |
|-------|-----------|
| [`bot/`](./bot/) | Bot do Telegram (Node.js + TypeScript + [Grammy](https://grammy.dev/)). |
| [`backend/`](./backend/) | API REST em Java (Spring Boot) — classificação por IA e persistência. |
| [`compose.yaml`](./compose.yaml) | Orquestração dos serviços com Docker Compose. |

Um **painel admin** (renda, gastos fixos, categorias, dashboards) está planejado, mas ainda não foi iniciado.

---

## Rodando com Docker (recomendado)

Cada serviço tem o próprio `Dockerfile` (build multi-stage) e o `compose.yaml` na raiz sobe tudo junto, já com a rede interna entre bot e backend.

```bash
cp bot/.env.example bot/.env           # defina BOT_TOKEN
cp backend/.env.example backend/.env   # defina GEMINI_API_KEY
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

Camada fina: recebe mensagens do Telegram, faz validações técnicas mínimas e repassa ao backend. Sem lógica de negócio.

**Funcionalidades**
- Comando `/start`.
- Registro de gasto por **texto**.
- Registro de gasto por **foto de recibo** (baixa do Telegram e converte para base64).
- Integração HTTP com o backend — *em construção (fase 2)*.

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

API REST que classifica os gastos com IA e os persiste. Padrão Controller → Service → Repository.

**Funcionalidades**
- `POST /api/expenses/text` — recebe um texto e devolve `{ category, amount, date, description }`, classificado pelo Google Gemini (`gemini-flash-lite-latest`) com saída JSON estruturada.
- `GET /actuator/health` — healthcheck (Spring Boot Actuator).
- `POST /api/expenses/photo` e persistência (PostgreSQL) — *planejado*.

**Como rodar sem Docker**
```bash
cd backend
cp .env.example .env   # defina GEMINI_API_KEY
./mvnw spring-boot:run # sobe em http://localhost:8080
```

> Rode a partir da pasta `backend/`: o Spring lê o `.env` relativo ao diretório de execução. O `./mvnw` (Maven Wrapper) dispensa ter o Maven instalado.

**Exemplo**
```bash
curl -X POST http://localhost:8080/api/expenses/text \
  -H 'Content-Type: application/json' \
  -d '{"text":"gastei 45 reais no mercado"}'
```

---

## Requisitos

- **Docker** com Docker Compose — para rodar tudo junto.
- Para desenvolvimento sem Docker: **Node.js** 24+ (bot) e **Java** 21+ (backend).
- Token do Telegram via [@BotFather](https://t.me/BotFather) e chave do Gemini via [Google AI Studio](https://aistudio.google.com/apikey).

## Licença

Veja [LICENSE](./LICENSE).
