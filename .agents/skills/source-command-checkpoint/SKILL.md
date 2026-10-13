---
name: "source-command-checkpoint"
description: "Migrated source command `checkpoint`"
---

# source-command-checkpoint

Use this skill when the user asks to run the migrated source command `checkpoint`.

## Command Template

Ritual de encerramento de sessão: atualize o arquivo `AGENTS.md` na raiz do projeto com um resumo das decisões importantes tomadas nessa sessão.

Siga estas regras:

1. **Leia o `AGENTS.md` atual** antes de editar.
2. **Não apague seções existentes** — apenas atualize ou acrescente informações.
3. Adicione ou atualize as seguintes seções conforme o que foi discutido/decidido nessa sessão:
   - **Estado atual** — reflita mudanças no que foi implementado.
   - **Decisões de arquitetura** — novas decisões ou mudanças nas existentes.
   - **Bibliotecas e dependências** — adições com o motivo da escolha.
   - **Padrões de código** — convenções ou padrões definidos.
   - **Problemas e soluções** — problemas encontrados e como foram resolvidos.
   - **Próximos passos** — o que ficou planejado para a próxima sessão.
4. Se alguma seção não tiver conteúdo novo nessa sessão, **não a inclua**.
5. Use português para todo o conteúdo adicionado.
6. Seja conciso: prefira bullet points a parágrafos longos.
7. **Replique as mesmas alterações no `CLAUDE.md`** — ele espelha o `AGENTS.md` para o Claude Code (só muda "Codex" → "Claude" no título e em "Instruções para o ...").
8. Após atualizar, confirme ao usuário o que foi adicionado/alterado.
