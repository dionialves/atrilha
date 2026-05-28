---
name: orquestrar-pipeline-us
description: Orquestra implementação de US via pipeline Codificador → Revisor → PR Draft, seguindo protocolo AGENTS.md
source: auto-skill
extracted_at: '2026-05-28T12:03:32.707Z'
---

# Orquestrar Pipeline de Implementação US (Codificador → Revisor → PR Draft)

## Quando usar
Quando o usuário pede para implementar uma US/Issue via pipeline de agentes (Codificador → Revisor), conforme descrito em `AGENTS.md` seção "Workflow — Canonical Source vs. Execução Local".

## Procedimento

### 1. Verificar estado do pipeline
Antes de delegar, inspecione o disco para determinar onde o pipeline está:

```bash
# Brief existe?
ls .qwen/briefs/*<CODE>*.md

# Issue existe no GitHub?
gh issue list --search "<CODE> in:title"

# Worktree existe?
ls .qwen/worktrees/*-<N>-*
```

**Decisão por estado (AGENTS.md):**

| Brief | Issue | Worktree | Próximo passo |
|-------|-------|----------|---------------|
| ❌ | — | — | Delegar **scout** para criar brief |
| ✅ | ❌ | — | Delegar **arquiteto** para gerar Issue |
| ✅ | ✅ | ❌ | Delegar **codificador** para implementar |
| ✅ | ✅ | ✅ (SUMMARY.md) | Delegar **revisor** para auditar |

### 2. Delegar ao agente apropriado
Use o subagent correspondente via tool `Agent`. Frases canônicas:

- **Scout:** `Use o scout para preparar o brief de <CODE>`
- **Arquiteto:** `Use o arquiteto para gerar a Issue de <CODE>`
- **Codificador:** `Use o codificador para iniciar a implementação da issue #N`
- **Revisor:** `Use o revisor para auditar a issue #N`

### 3. Validar entrega do Codificador
Ao retornar, verificar:
- Testes unitários verdes (`BUILD SUCCESS`)
- Zero warnings no build
- `SUMMARY.md` preenchido na worktree

### 4. Delegar Revisor
```bash
Use o revisor para auditar a issue #N
```

O Revisor executa `load_review` → audita 3 camadas (aderência ao plano, qualidade técnica, critérios de aceitação) → `approve` ou `reject`.

### 5. Confirmar PR Draft
```bash
gh pr view <N> --json title,state,url,headRefName,body
```

Verificar:
- `state` = `OPEN`
- Título no formato Conventional Commits pt-BR
- Body menciona "Closes #<issue>" e "PR aberto como draft"

## Regras não negociáveis
- **Só o Revisor** abre PR (como draft). A sessão raiz nunca executa `gh pr create`.
- **Só o humano** converte PR draft para ready e faz merge.
- Nunca `--force` (use `--force-with-lease` se rebase exigir).
- Uma task = uma branch = um PR = um commit squash.

## Exceções comuns
- **Testcontainers falham localmente:** se Docker não está disponível, os ITs com Testcontainers podem falhar. Verificar se testes unitários passam e se o CI rodaria verde. Documentar no SUMMARY.md.
- **`_project.sh` não tracked:** scripts do pipeline podem precisar copiar `_project.sh` manualmente para a worktree.
