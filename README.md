# PennyBot

Monorepo do **PennyBot**: assistente de finanças pessoais com cadastro de gastos pelo **Telegram** e gestão (planejado) por um **painel administrativo**.

## Estrutura do repositório

| Pasta   | Descrição |
|---------|-----------|
| [`bot/`](./bot/) | Bot do Telegram (Node.js + TypeScript + [Grammy](https://grammy.dev/)). |
| [`panel/`](./panel/) | Painel web para renda, gastos fixos, categorias, dashboards e edição de lançamentos — **a implementar**. |

Cada subprojeto evolui de forma independente; a raiz só organiza o conjunto.

## Requisitos

- **Node.js** 18+ (recomendado LTS atual)
- Conta no Telegram e token do [@BotFather](https://t.me/BotFather) para rodar o bot

## Desenvolvimento rápido

### Bot

```bash
cd bot
cp .env.example .env
# Edite .env e defina BOT_TOKEN

npm install
npm run dev
```

Detalhes e escopo do código do bot: [`bot/README.md`](./bot/README.md).

### Painel

A pasta `panel/` está reservada para o frontend (e, se fizer sentido, API próxima ao painel). Quando o stack for definido, as instruções de build e deploy entram nesse diretório.

## Licença

Veja [LICENSE](./LICENSE).
