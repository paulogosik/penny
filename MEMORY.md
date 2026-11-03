# PennyBot — memória do projeto

Diário do projeto, usado como contexto externo entre sessões. O [`CLAUDE.md`](./CLAUDE.md) guarda o que é permanente (arquitetura, decisões, padrões); este arquivo guarda **o momento atual, o histórico de cada PR, pendências e armadilhas já encontradas**.

**Como manter:** atualizar a cada PR criado. Entradas novas no topo de cada seção. Datas absolutas.

---

## Onde paramos (02/11/2026)

- **Revisão de segurança e suíte de testes** (PR #6, aberto): backend com 76 testes (eram 50) e bot com 37 testes (não tinha nenhum), mais as correções dos achados. Os achados que ficaram abertos estão em "Pendências".
- **Bot integrado ao backend** desde o PR #4: `/start` cadastra e pede o telefone (obrigatório), depois conversa livre por texto e foto. Agora só responde em conversa privada.
- **CI** (PR #5, mergeado em 19/10/2026): jobs `backend`, `bot` (agora com `npm test`) e `docker`; `main` protegida exigindo os três verdes.
- **Painel** fica para um segundo momento (decisão do usuário). Login já desenhado: código pelo Telegram.
- **CD** fica para quando chegarmos no deploy (decisão do usuário).
- PRs #1 a #5 mergeados; #6 aberto.
- Próxima ação sugerida: mergear o #6, testar no Telegram real e então escolher entre cadastrar cartão pelo bot ou começar o painel.

---

## Pendências e decisões em aberto

### Login do painel por código no Telegram (desenhado, não implementado)
- `POST /api/auth/code` `{ phoneNumber }` → se não existe conta com o número, responde para a pessoa abrir o bot; se existe, gera código de 6 dígitos e envia com `sendMessage` (Bot API, `chat_id` = `telegram_user_id`) usando o mesmo `BOT_TOKEN`.
- `POST /api/auth/verify` `{ phoneNumber, code }` → sessão do painel; trocar `CurrentUserArgumentResolver` para aceitar a sessão.
- Regras: validade de 5 min, uso único, até 5 tentativas, limite de pedidos por telefone, código guardado com hash, mensagem "se não foi você, ignore".
- O backend vai precisar do `BOT_TOKEN` no `.env` dele.

### CD (deploy contínuo) — adiado
- Falta escolher a hospedagem. O bot usa long polling, então precisa de um processo sempre ligado (VPS, Railway, Fly.io...; quase todas com custo mensal).
- Plano: a cada merge na `main`, gerar as imagens, publicar no GHCR e atualizar o servidor. Segredos no GitHub Secrets ou no painel da hospedagem.
- Rodando bot + backend juntos (como no `compose.yaml`), o backend não precisa ficar exposto na internet, o que contorna a falta de autenticação até o painel existir.

### Cartões sem painel
- Hoje cartões só são criados pela API (`POST /api/credit-cards`). Sem cartão, "comprei X em 6x" responde `INSTALLMENTS_REQUIRE_CREDIT_CARD` mandando cadastrar "no painel", que ainda não existe. Opções: cadastrar o cartão pelo bot ("meu cartão Nubank fecha dia 3 e vence dia 10") ou esperar o painel.

### Antes de expor o backend fora da máquina
- **Autenticação:** hoje qualquer um que alcance o backend pode se passar por qualquer usuário via `X-Telegram-User-Id`. Trocar o resolver (`web/CurrentUserArgumentResolver`) quando o login do painel existir.
- **Chave de API entre bot e backend** (segredo compartilhado num cabeçalho).
- **Quem pode usar o bot** (lista de liberados) — o usuário decidiu tratar depois, junto com a criação de contas.
- **Limite de uso (rate limit):** qualquer pessoa que achar o bot pode se cadastrar e gastar a cota do Gemini (cada mensagem faz até 3 chamadas).
- **Limite de tamanho da requisição:** o Tomcat não limita corpo JSON; o maior campo legítimo é a foto (até 14 milhões de caracteres em base64).

### Achados da revisão de segurança de 02/11/2026 ainda abertos
- **Mensagens processadas uma por vez:** o `bot.start()` do grammY é sequencial; uma chamada à IA (até ~90 s) segura as mensagens de todo mundo. Solução: `@grammyjs/runner` + `sequentialize` por chat.
- **Telefone pode aparecer no log:** o handler de `DataIntegrityViolationException` loga a causa do Postgres, que inclui o valor duplicado. A checagem de número já usado antes evita o caso comum; só uma corrida chega lá.
- **`.env` com permissão 644** (legível por outros usuários da máquina): `chmod 600 bot/.env backend/.env`.
- **Actions fixadas por versão principal** (`@v7`), não por SHA: aceitável enquanto o CI não tem segredos; trocar por SHA quando entrar o CD.
- **Baixo risco:** data vinda da IA sem limite (pode cair em 1900 ou 2100); tipo da imagem não é conferido com o conteúdo (o Gemini recusa o que não for imagem); comparação de nome de cartão por "contém" (um cartão chamado "C" casa com qualquer texto com "c").

### Decisões em aberto
- **Stack do painel** e **forma de login** (perguntar ao usuário, não assumir).
- **Dashboard por data da compra ou por fatura:** hoje soma por `occurred_on`; o banco já guarda `billing_month` para a visão por fatura.
- **Recibos no Supabase Storage:** coluna `expenses.receipt_path` existe, upload não implementado (exigiria bucket privado + service role key).
- **Deploy** (onde hospedar backend e bot).

### Melhorias pequenas anotadas
- Geração de recorrentes cobre só o mês atual (se o app ficar fora do ar um mês inteiro, aquele mês não é gerado retroativamente).
- Uma mensagem de gasto enviada antes do cadastro é descartada (o bot cadastra e pede o número; a pessoa reenvia).
- A legenda de uma foto de recibo é ignorada (poderia virar dica para a IA).
- Pedidos em linguagem natural além de registrar gasto ("quanto gastei esse mês?", "apaga o último gasto") — ideia do usuário de manter tudo por conversa, sem menus.

---

## Histórico de PRs

### PR #6 — Revisão de segurança e suíte de testes (`test/security-and-test-suite`) — aberto em 02/11/2026
- Revisão de backend, bot, Docker, CI e histórico do Git: nenhum segredo commitado; `npm audit` limpo depois de atualizar o `diff` (dependência de desenvolvimento do `ts-node`).
- Correções: backend publicado só em `127.0.0.1:8080` (antes aberto para a rede, sem login); bot só em conversa privada (em grupo mostrava os gastos para todos); 405, 415 e 500 no formato padrão, com o 500 sem detalhes internos; valor gigante vindo da IA → `AMOUNT_TOO_LARGE` (antes virava 409 `DATA_CONFLICT`); `X-Telegram-User-Id` 0 ou negativo → `INVALID_HEADER`; timeout de 30 s no download da foto.
- Backend: 26 testes novos (76 no total) em `TenantIsolationTest`, `RequestHardeningTest`, `AiOutputHandlingTest`, `PlatformSecurityTest` e `GeminiExpenseClassifierTest`.
- Bot: testes com `node:test` (`npm test`, 37 testes) em `bot/test/`; o job `bot` do CI passou a rodar os testes.
- Checagem com bugs plantados: 6 no backend (filtro por usuário removido em gasto e em cartão, header ≤ 0 aceito, sem tratamento de 500, sem limite de valor, chave do Gemini na URL) e 1 no bot (sem filtro de conversa privada). Todos pegos pelos testes.

### PR #5 — CI no GitHub Actions (`ci/github-actions`) — mergeado em 19/10/2026 02:08 UTC
- `.github/workflows/ci.yml`: jobs paralelos `backend` (`./mvnw -B -ntp verify`, relatórios do Surefire como artefato em caso de falha), `bot` (`npm ci` + `npm run build`) e `docker` (build das imagens), em PR, push na `main` e execução manual.
- Sem segredos; `permissions: contents: read`; cancela runs antigos do mesmo PR (mas não os da `main`).
- Selo de CI e seção "Integração contínua" no README.
- Proteção da `main`: exige `backend`, `bot` e `docker` verdes; sem review obrigatório; admin pode fazer bypass.

### PR #4 — Integração do bot + telefone obrigatório (`feat/bot-integration`) — mergeado em 19/10/2026 02:04 UTC
- Backend: migration **V3** (`users.phone_number`, E.164, único), `PUT /api/me/phone` com normalização, regra `PHONE_NUMBER_REQUIRED` nas rotas de gasto do bot, 6 testes novos (50 no total).
- Bot: cliente HTTP real (`ApiError` com `status`/`code`, timeout de 90s), cadastro no `/start` com pedido de telefone e explicação do motivo (login no painel sem senha), botão nativo de compartilhar contato, recusa de contato de outra pessoa, gastos por texto e foto, mensagens centralizadas em `messages.ts` (fim do `formatarGasto` duplicado), `createBot()` separado do entrypoint, desligamento limpo no `SIGTERM`.
- Validação: simulador com `bot.handleUpdate` cobrindo 13 cenários (cadastro, contato próprio/alheio, gasto simples, parcelado, cartão, sem valor, comando inexistente, foto) contra backend + Supabase + Gemini reais; usuário fictício `999999002` apagado no fim.

### PR #3 — Backend completo integrado ao banco (`feat/backend-core`) — mergeado em 14/10/2026 02:26 UTC
- Entidades JPA e repositórios para as 10 tabelas; `ddl-auto=validate` confirma que batem com o schema.
- Fluxo do bot: cadastro idempotente com categorias padrão (acentos corrigidos: Saúde, Educação, Serviços), gasto por texto e por foto, parcelamento no cartão detectado pela IA, fallback para "Outros".
- API do painel: categorias (gasto/renda), gastos (listagem paginada com filtros), rendas, cartões + fatura do mês, parcelamentos, recorrências, metas, dashboard.
- Geração automática de recorrentes (hora em hora + na subida); migration **V2** adiciona `last_generated_month` para não recriar lançamento apagado.
- Gemini: categorias e cartões do usuário como enum no schema, data no fuso do usuário, chave no cabeçalho, timeout, nova tentativa em 429/5xx.
- Erros padronizados `{ code, message, fields? }`; validação com mensagens em português.
- 44 testes (unitários + integração com Testcontainers); dois bugs plantados de propósito foram pegos pelos testes (checagem de que os testes realmente testam).
- Smoke test ao vivo no Supabase com usuário fictício `999999001`, apagado em seguida.

### PR #2 — Schema do banco no Supabase (`feat/database-schema`) — mergeado em 14/10/2026 02:23 UTC
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
- **Testar o bot sem o Telegram:** `createBot(token, { botInfo })` + `bot.api.config.use(...)` para capturar `sendMessage`/`getFile` + `bot.handleUpdate(update)` com mensagens fabricadas; interceptar o `fetch` de `api.telegram.org/file/` para devolver uma imagem local. Funciona com token falso, sem conflitar com o bot real rodando. Desde o PR #6 isso está pronto em `bot/test/support/ambiente.ts`.
- **Conferir se o teste testa:** desfazer a correção (plantar o bug), ver o teste falhar e restaurar. Usado nos PRs #3 e #6.
- **Forçar um 500 em teste do backend:** apagar a categoria "Outros" do usuário direto no banco (`JdbcTemplate`) e fazer a IA falsa devolver uma categoria desconhecida.
- **CORS no MockMvc:** sem configuração de CORS, o preflight de outra origem recebe 403 sem `Access-Control-Allow-Origin` (padrão do Spring, e é o que queremos).
- **`node --test` com glob:** o padrão vai entre aspas no script (`"build-test/test/**/*.test.js"`) para o Node expandir, não o shell.
- **Plugins da Cloud Humans desligados neste projeto** (18/10/2026): `.claude/settings.local.json` (local, fora do Git) com `syncClaudeAiPlugins: false`, `syncClaudeAiSkills: false` e `disableClaudeAiConnectors: true`. Vale só para sessões nesta pasta; os projetos do trabalho seguem iguais. O hook do plugin `ch-engineering` ligava um monitor de comentários de PR que é inútil aqui (ignora comentários do próprio usuário). O app desktop ignora essas chaves (conferido em 20/10 e 02/11/2026): os plugins continuam carregados, então o Claude simplesmente não usa nada deles neste projeto.
- **Imagem de recibo para teste:** `qlmanage -t -s 900 -o <dir> recibo.txt` gera um PNG a partir de um texto (macOS).
- **PR empilhado:** se o PR de base for mergeado sem apagar o branch, o GitHub não redireciona o PR de cima para a `main`; conferir com `gh pr view <n> --json baseRefName` e corrigir com `gh pr edit <n> --base main`.
- **Shell do usuário é fish**, mas os comandos do Claude rodam em zsh: em zsh, `$var:...` é tratado como modificador; usar `${var}`.
- **Python da máquina sem certificados raiz** (SSL falha em `urllib`): usar `curl` para chamadas HTTPS em scripts.

---

## Ambiente
- macOS com Docker Desktop, JDK 25, Node 26 (imagem Docker usa Node 24 LTS e Java 21).
- Supabase: Postgres 17.11, região `us-east-1`, conexão via Session pooler.
- Repositório: `github.com/paulogosik/penny`, branch principal `main`, trabalho em branches + PR mergeado pelo usuário.
