#!/usr/bin/env bash
# reject.sh <issue_number> "<motivo>"
#
# Tool do REVISOR — veredito AJUSTES NECESSÁRIOS / BLOQUEADO.
# NÃO destrói nada. Escreve o motivo em REVIEW.md na worktree e devolve
# o controle ao Codificador, que retoma a MESMA worktree (iteração
# incremental — não recomeça do zero).
set -euo pipefail

ISSUE="${1:?uso: reject <numero-da-issue> \"<motivo>\"}"
ISSUE="${ISSUE#\#}"
MOTIVO="${2:?informe o motivo da devolução entre aspas}"

REPO_ROOT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
WT_BASE="${REPO_ROOT}/.opencode/worktrees"

WT_PATH="$(find "$WT_BASE" -maxdepth 1 -type d -name "*-${ISSUE}-*" | head -n1 || true)"
[ -n "$WT_PATH" ] || { echo "ERRO: worktree da issue #${ISSUE} não encontrada." >&2; exit 1; }

REVIEW="${WT_PATH}/REVIEW.md"
TS="$(date '+%Y-%m-%d %H:%M')"

cat >> "$REVIEW" <<EOF

## Devolução — ${TS}
**Veredito:** AJUSTES NECESSÁRIOS

${MOTIVO}

---
EOF

cat <<EOF
=== TAREFA DEVOLVIDA AO CODIFICADOR ===
Issue:    #${ISSUE}
Worktree: ${WT_PATH}  (PRESERVADA — não recomece do zero)
Motivo registrado em: ${REVIEW}

>>> Codificador: leia ${REVIEW}, corrija na mesma worktree,
    e chame finish_task ${ISSUE} de novo quando pronto.
EOF
