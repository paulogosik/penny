# PennyBot — Telegram

Bot do Telegram do PennyBot: a pessoa registra gastos conversando, por texto livre ou foto de recibo. O bot repassa ao backend, que classifica com IA e salva.

## Como a conversa funciona

1. A pessoa toca em **Iniciar** (`/start`): o bot cria a conta e pede o número de telefone pelo botão **📱 Compartilhar meu número**, explicando que ele serve para entrar no painel do site sem senha.
2. O bot só aceita o contato se ele for da própria pessoa.
3. Com o número salvo, é só mandar mensagens como "gastei 45 no mercado", "uber ontem 23,50", "tênis de 600 em 6x no nubank" ou uma foto do recibo.

Não há outros comandos: qualquer `/comando` recebe uma dica de como usar.

## Configuração

```bash
cp .env.example .env
```

- `BOT_TOKEN`: token do bot, obtido com o @BotFather.
- `BACKEND_URL`: endereço do backend (no Docker Compose já vem como `http://backend:8080`).

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
├── bot.ts               # entrypoint: lê o .env, cria o bot e inicia
├── app.ts               # createBot(): registra os handlers
├── messages.ts          # textos, formatação do gasto e botão de compartilhar número
├── types.ts             # contrato de req/res com o backend
├── handlers/
│   ├── start.ts         # /start
│   ├── onboarding.ts    # cadastro e pedido de telefone
│   ├── contact.ts       # contato compartilhado → salva o telefone
│   ├── expense.ts       # gasto por texto
│   ├── photo.ts         # gasto por foto de recibo
│   └── errors.ts        # código de erro do backend → mensagem para o usuário
└── services/
    └── api.ts           # cliente HTTP do backend
```
