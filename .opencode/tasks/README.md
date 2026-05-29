# Tasks — specs de subtask (Arquiteto fase 2 → Codificador)

Cada **spec de subtask** é o plano executável extremamente detalhado de UMA
subtask de uma demanda: RED→GREEN, código literal, zero ambiguidade. É escrito
pelo **arquiteto** (fase 2) e implementado pelo **codificador**, uma subtask por
vez, dentro da mesma worktree da demanda.

## Convenção de nome

`<CODE>-<letra>.md` (uma letra por subtask, ordem topológica), ou `<CODE>.md`
para demanda de task única:

```
US-042-a.md   US-042-b.md   US-042-c.md      # demanda grande, 3 subtasks
FIX-017.md                                   # demanda pequena, task única
```

A letra casa com a lista de subtasks da Issue-resumo (`<CODE>-a`, `<CODE>-b`, …)
e com o commit local que cada subtask vira ao ser aprovada: `feat(us-042-a): <slug>`.

## Ciclo de vida

1. `Use o arquiteto para gerar a Issue de <CODE>` → Issue-resumo com a lista de subtasks.
2. `Use o arquiteto para detalhar as subtasks de <CODE>` → gera `.opencode/tasks/<CODE>-<letra>.md` (um por subtask) e roda `validate_tasks.sh`.
3. `Use o codificador para implementar <CODE>` → `start_us` abre 1 worktree; implementa subtask a subtask, `finish_task <CODE>-<letra>` em cada.
4. Revisor `load_review`/`approve` por subtask (1 commit local cada); `open_pr <CODE>` quando todas aprovadas.

## Validação determinística

```bash
bash .opencode/scripts/validate_tasks.sh <CODE>
```

Checa, em cada `<CODE>-*.md`: filename no padrão; H2 obrigatórios (`## Objetivo`,
`## Passo a passo`, `## Testes`, `## Critérios de aceitação`); passo a passo com
itens numerados.

Estrutura completa do spec: ver template **§2.6** em [`../agent/arquiteto.md`](../agent/arquiteto.md).
