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

Um **painel admin** (renda, gastos fixos, categorias, dashboards) está planejado, mas ainda não foi iniciado.

---

## Bot (`bot/`)

Camada fina: recebe mensagens do Telegram, faz validações técnicas mínimas e repassa ao backend. Sem lógica de negócio.

**Funcionalidades**
- Comando `/start`.
- Registro de gasto por **texto**.
- Registro de gasto por **foto de recibo** (baixa do Telegram e converte para base64).
- Integração HTTP com o backend — *em construção (fase 2)*.

**Como rodar**
```bash
cd bot
cp .env.example .env   # defina BOT_TOKEN (e, na fase 2, BACKEND_URL)
npm install
npm run dev
```

Mais detalhes em [`bot/README.md`](./bot/README.md).

---

## Backend (`backend/`)

API REST que classifica os gastos com IA e os persiste. Padrão Controller → Service → Repository.

**Funcionalidades**
- `POST /api/expenses/text` — recebe um texto e devolve `{ category, amount, date, description }` *(hoje: retorno fixo, para validar o ciclo)*.
- Classificação por IA (Google Gemini `gemini-1.5-flash`) — *planejado*.
- `POST /api/expenses/photo` e persistência (PostgreSQL) — *planejado*.

**Como rodar**
```bash
cd backend
./mvnw spring-boot:run   # sobe em http://localhost:8080
```

> O `./mvnw` (Maven Wrapper) dispensa ter o Maven instalado. Se você já tem o Maven no sistema, `mvn spring-boot:run` funciona igual.

---

## Requisitos

- **Node.js** 18+ — para o bot.
- **Java** 21+ — para o backend (o Maven Wrapper `./mvnw` cuida do Maven).
- Token do Telegram via [@BotFather](https://t.me/BotFather).

## Licença

Veja [LICENSE](./LICENSE).
