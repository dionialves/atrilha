# Briefs — handoff Scout → Arquiteto

Um **brief** é a fotografia factual do estado atual do repo + objetivo + critérios
de uma demanda. É escrito pelo **scout** (fase 1) e lido pelo **arquiteto** (que
não reabre código). **Nunca contém solução** — só munição factual.

## Convenção de nome

`<CODE>` em **MAIÚSCULAS**, sem espaços. O número de arquivos depende do tamanho da
demanda (decisão numérica do §4 do scout):

| Tier | Quando | Arquivos |
|---|---|---|
| **Tier 1** (pequena) | nenhum cap do §4 estourado | `<CODE>.md` (brief único) |
| **Tier 2** (grande) | algum cap duro estourado | `<CODE>-slicing.md` (log de auditoria) + `<CODE>-a.md`, `<CODE>-b.md`, … (um brief por subtask) |

```
# Tier 1
US-042.md     FIX-017.md     REF-003.md     CHORE-009.md

# Tier 2
US-042-slicing.md   US-042-a.md   US-042-b.md   US-042-c.md
```

> **N briefs, 1 PR.** Diferente do `.qwen`, aqui as subtasks do Tier 2 **não** viram
> N Issues/PRs. O arquiteto consolida os N briefs de subtask numa **única
> Issue-resumo** (lista a/b/c) e detalha cada uma em `.opencode/tasks/<CODE>-<letra>.md`;
> tudo vive numa só worktree/branch/PR. O scout fatia o **planejamento**, não a entrega.

Sufixo de subtask: letras minúsculas sequenciais sem pular (`-a`, `-b`, `-c`, …).
Limite prático: 8 subtasks; 9+ = a demanda é uma EPIC e o scout devolve ao humano.

## Ciclo de vida

1. `Use o scout para preparar o brief de <CODE>` → gera `.opencode/briefs/<CODE>.md`
   (Tier 1) **ou** `<CODE>-slicing.md` + `<CODE>-<letra>.md` (Tier 2), numa única passada.
2. O scout roda `validate_brief.sh` em cada arquivo até passar (`✅`).
3. `Use o arquiteto para gerar a Issue de <CODE>` → consolida em 1 Issue-resumo
   (mesma invocação para Tier 1 ou Tier 2 — sempre `<CODE>`, sem sufixo).
4. Os briefs permanecem como referência/auditoria do que foi planejado.

## Validação determinística

```bash
bash .opencode/scripts/validate_brief.sh .opencode/briefs/<CODE>.md          # Tier 1
bash .opencode/scripts/validate_brief.sh .opencode/briefs/<CODE>-slicing.md  # Tier 2
bash .opencode/scripts/validate_brief.sh .opencode/briefs/<CODE>-a.md        # Tier 2 (por subtask)
```

Checa: filename em MAIÚSCULAS (aceita `-<letra>` e `-slicing`); H2 obrigatórios
(`## Metadados`, `## Demanda`, `## 1. Contexto`, `## 2. Objetivo`, `## 3. Critérios`,
`## 4. Observações`, `## Checklist`); zero vocabulário proibido fora de blockquotes;
checklist ≥ 12 itens. **Regras relaxadas** para `*-slicing.md` (sem H2/checklist
obrigatórios — só filename + vocabulário).

Estrutura completa: ver template **§7-A** (brief) e **§7-B** (slicing log) em
[`../agent/scout.md`](../agent/scout.md).
