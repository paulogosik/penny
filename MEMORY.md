# PennyBot — memória do projeto

Diário do projeto, usado como contexto externo entre sessões. O [`CLAUDE.md`](./CLAUDE.md) guarda o que é permanente (arquitetura, decisões, padrões); este arquivo guarda **o momento atual, o histórico de cada PR, pendências e armadilhas já encontradas**.

**Como manter:** atualizar a cada PR criado. Entradas novas no topo de cada seção. Datas absolutas.

---

## Onde paramos (13/10/2026)

- **Backend pronto** para o bot e para o painel: API completa, integrada ao Supabase, 44 testes passando, validado ao vivo com o Gemini real (texto, foto de recibo, parcelado no cartão, data relativa, fuso horário).
- **Bot** ainda não chama o backend: `services/api.ts` lança `BackendIndisponivelError`.
- **Painel** não iniciado (stack e autenticação indefinidos).
- PRs abertos: **#2** (schema) e **#3** (backend, empilhado sobre o #2). Mergear o #2 primeiro.
- Próxima ação sugerida: **integrar o bot ao backend**.

---

## Pendências e decisões em aberto

### Integração do bot (próximo PR)
- `/start` → `PUT /api/me` com `{ name }` e cabeçalho `X-Telegram-User-Id: <ctx.from.id>`.
- Texto → `POST /api/expenses/text` `{ text }`; foto → `POST /api/expenses/photo` `{ imageBase64, mimeType }`.
- Atualizar `bot/src/types.ts` para o `ExpenseResponse` novo: `categoryName` (era `category`), `occurredOn` (era `date`), `amount` (número com 2 casas), `creditCardName`, `billingMonth`, `installment { planId, number, count, totalAmount }`.
- Tratar os `code` de erro no bot:
  - `USER_NOT_REGISTERED` (404) → chamar `PUT /api/me` e repetir.
  - `AMOUNT_NOT_FOUND`, `INSTALLMENTS_REQUIRE_CREDIT_CARD`, `INVALID_IMAGE` (422) → repassar a `message` ao usuário.
  - `AI_UNAVAILABLE` (502) → pedir para tentar de novo.
- No Docker, o bot já recebe `BACKEND_URL=http://backend:8080` pelo Compose.

### Antes de expor o backend fora da máquina
- **Autenticação:** hoje qualquer um que alcance o backend pode se passar por qualquer usuário via `X-Telegram-User-Id`. Trocar o resolver (`web/CurrentUserArgumentResolver`) quando o login do painel existir.
- **Chave de API entre bot e backend** (segredo compartilhado num cabeçalho).
- **Quem pode usar o bot** (lista de liberados) — o usuário decidiu tratar depois, junto com a criação de contas.

### Decisões em aberto
- **Stack do painel** e **forma de login** (perguntar ao usuário, não assumir).
- **Dashboard por data da compra ou por fatura:** hoje soma por `occurred_on`; o banco já guarda `billing_month` para a visão por fatura.
- **Recibos no Supabase Storage:** coluna `expenses.receipt_path` existe, upload não implementado (exigiria bucket privado + service role key).
- **Deploy** (onde hospedar backend e bot).

### Melhorias pequenas anotadas
- Extrair o `formatarGasto` duplicado em `bot/src/handlers/{expense,photo}.ts`.
- Geração de recorrentes cobre só o mês atual (se o app ficar fora do ar um mês inteiro, aquele mês não é gerado retroativamente).

---

## Histórico de PRs

### PR #3 — Backend completo integrado ao banco (`feat/backend-core`) — aberto em 13/10/2026
- Entidades JPA e repositórios para as 10 tabelas; `ddl-auto=validate` confirma que batem com o schema.
- Fluxo do bot: cadastro idempotente com categorias padrão (acentos corrigidos: Saúde, Educação, Serviços), gasto por texto e por foto, parcelamento no cartão detectado pela IA, fallback para "Outros".
- API do painel: categorias (gasto/renda), gastos (listagem paginada com filtros), rendas, cartões + fatura do mês, parcelamentos, recorrências, metas, dashboard.
- Geração automática de recorrentes (hora em hora + na subida); migration **V2** adiciona `last_generated_month` para não recriar lançamento apagado.
- Gemini: categorias e cartões do usuário como enum no schema, data no fuso do usuário, chave no cabeçalho, timeout, nova tentativa em 429/5xx.
- Erros padronizados `{ code, message, fields? }`; validação com mensagens em português.
- 44 testes (unitários + integração com Testcontainers); dois bugs plantados de propósito foram pegos pelos testes (checagem de que os testes realmente testam).
- Smoke test ao vivo no Supabase com usuário fictício `999999001`, apagado em seguida.

### PR #2 — Schema do banco no Supabase (`feat/database-schema`) — aberto em 13/10/2026
- Supabase escolhido como Postgres gerenciado; acesso só pelo backend.
- `V1__create_initial_schema.sql` com as 10 tabelas, constraints e índices; RLS em tudo; callback `afterMigrate.sql` para o RLS da tabela do Flyway.
- `compose.yaml` com `env_file` em `format: raw`.
- **Incidente:** a senha do banco apareceu em texto puro na conversa (erro ao exibir a estrutura do `DB_URL`); a senha foi resetada no Supabase. Regra desde então: nunca imprimir valores do `.env`.

### PR #1 — Docker e limpeza (`chore/docker-and-cleanup`) — mergeado em 14/10/2026 01:24 UTC
- Removidas sobras (package.json da raiz, Dockerfile/.dockerignore de template, `.gitignore` de Visual Studio, `logs/` de um MCP).
- Dockerfiles multi-stage por serviço + `compose.yaml`; Actuator para healthcheck.
- Modelo do Gemini trocado para `gemini-flash-lite-latest`.
- Todos os comentários removidos do código (pedido do usuário).
- Configurado `attribution` vazio em `~/.claude/settings.json` (commits sem coautoria do Claude).

---

## Armadilhas e lições

- **Gemini (13/10/2026):** linha Flash principal com 503 constante; Flash-Lite estável; Pro e 3.1-pro sem cota no plano gratuito (429); linha 2.5 bloqueada para chaves novas (404); Gemma 4 com erro 500. Reavaliar se o Lite começar a falhar.
- **Validar `.env` sem expor segredos:** script Python que imprime só OK/FALHA; separar URL pelo **último** `@`; cuidado com `$` e `\`.
- **Supabase:** usar o Session pooler; muitas falhas de senha seguidas podem bloquear o IP — parar o backend na hora.
- **Testes de integração exigem Docker rodando** (Testcontainers). Rodar com `cd backend && ./mvnw test` (~1 min).
- **Smoke test ao vivo:** usar um `telegram_user_id` fictício e apagar o usuário no fim (`delete from users where telegram_user_id = ...` remove tudo em cascata).
- **Shell do usuário é fish**, mas os comandos do Claude rodam em zsh: em zsh, `$var:...` é tratado como modificador; usar `${var}`.
- **Python da máquina sem certificados raiz** (SSL falha em `urllib`): usar `curl` para chamadas HTTPS em scripts.

---

## Ambiente
- macOS com Docker Desktop, JDK 25, Node 26 (imagem Docker usa Node 24 LTS e Java 21).
- Supabase: Postgres 17.11, região `us-east-1`, conexão via Session pooler.
- Repositório: `github.com/paulogosik/penny`, branch principal `main`, trabalho em branches + PR mergeado pelo usuário.
