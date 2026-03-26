# PennyBot — Telegram

Serviço do bot no Telegram: recebe mensagens de texto (futuramente fotos e interpretação por IA) e responde ao usuário.

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

## Stack

- TypeScript (ESM)
- [Grammy](https://grammy.dev/)
- `dotenv`

## Estado atual

- Comando `/start` e eco de mensagens de texto; persistência, categorias e integração com IA ainda não estão implementadas.
