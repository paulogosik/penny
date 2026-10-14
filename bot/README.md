# PennyBot — Telegram

Serviço do bot no Telegram: recebe gastos por texto ou foto de recibo, prepara os dados e (fase 2) os envia ao backend para classificação por IA.

## Configuração

1. Copie o exemplo de ambiente:

   ```bash
   cp .env.example .env
   ```

2. Defina `BOT_TOKEN` no `.env` (token obtido com o @BotFather).

## Executar em desenvolvimento

```bash
npm install
npm run dev
```

## Build de produção

```bash
npm run build   # compila src/ para dist/
npm start       # roda dist/bot.js
```

No Docker, o `Dockerfile` faz exatamente isso em duas etapas (build e runtime só com dependências de produção). Para subir junto com o backend, use o `compose.yaml` da raiz — ver o [README principal](../README.md).

## Stack

- TypeScript (ESM)
- [Grammy](https://grammy.dev/)
- `dotenv`

## Estrutura

```
src/
├── bot.ts               # orquestrador — só registra handlers
├── types.ts             # contrato de req/res com o backend
├── handlers/
│   ├── start.ts         # comando /start
│   ├── expense.ts       # mensagem de texto
│   └── photo.ts         # foto de recibo (baixa e converte p/ base64)
└── services/
    └── api.ts           # cliente HTTP do backend
```

## Estado atual

- `/start`, handler de texto e handler de foto implementados (a foto é baixada do Telegram e convertida em base64).
- A chamada HTTP ao backend é **fase 2**: `services/api.ts` já expõe as funções e marca com `TODO` o ponto exato da integração; enquanto isso, os handlers respondem que a categorização por IA vem em seguida.
- Requer `BACKEND_URL` no `.env` (ver `.env.example`) quando a fase 2 for implementada.
