#!/usr/bin/env bash
# reject.sh <TASK_CODE> "<motivo>"
#
# Tool do REVISOR — veredito AJUSTES NECESSÁRIOS para UMA subtask.
# NÃO destrói nada. Registra o motivo em .opencode/tmp/<TASK_CODE>-REVIEW.md
# (fora da worktree, p/ não sujar o squash) e devolve ao Codificador, que
# retoma a MESMA worktree (iteração incremental — não recomeça do zero).
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

TASK="${1:?uso: reject <TASK_CODE> \"<motivo>\"}"
TASK="$(oc_strip_hash "$TASK")"
MOTIVO="${2:?informe o motivo da devolução entre aspas}"
CODE="$(oc_us_code "$TASK")"
WT_PATH="$(oc_wt_path "$CODE")"

[[ -d "$WT_PATH" ]] || { echo "ERRO: worktree não encontrada para $CODE em $WT_PATH." >&2; exit 1; }

mkdir -p "$OC_TMP"
REVIEW="$OC_TMP/$TASK-REVIEW.md"
TS="$(date '+%Y-%m-%d %H:%M')"

cat >> "$REVIEW" <<EOF

## Devolução — ${TS}
**Subtask:** ${TASK}
**Veredito:** AJUSTES NECESSÁRIOS

${MOTIVO}

---
EOF

cat <<EOF
=== SUBTASK DEVOLVIDA AO CODIFICADOR ===
Subtask:  ${TASK}
Worktree: ${WT_PATH}  (PRESERVADA — não recomece do zero)
Motivo registrado em: ${REVIEW}

>>> Codificador: leia ${REVIEW}, corrija na mesma worktree,
    e chame finish_task ${TASK} de novo quando pronto.
EOF
