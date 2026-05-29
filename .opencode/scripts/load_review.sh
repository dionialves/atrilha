#!/usr/bin/env bash
# load_review.sh <TASK_CODE>
#
# Tool do REVISOR. Monta o dossiê de auditoria de UMA subtask:
#   1. Issue da demanda (resumo) + spec específico da subtask.
#   2. SUMMARY da subtask (.opencode/tmp/<TASK_CODE>-SUMMARY.md).
#   3. Re-roda o test runner (Revisor NUNCA aprova sem testar).
#   4. Diff da subtask: SOMENTE o que foi feito desde o tip ref (subtask anterior).
#
# READ-ONLY. Em seguida: approve <TASK_CODE>  OU  reject <TASK_CODE> "<motivo>".
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

TASK="${1:?uso: load_review <TASK_CODE>   ex.: load_review US-042-a}"
TASK="$(oc_strip_hash "$TASK")"
CODE="$(oc_us_code "$TASK")"
WT_PATH="$(oc_wt_path "$CODE")"
TIP_REF="$(oc_tip_ref "$CODE")"

[[ -d "$WT_PATH" ]] || { echo "ERRO: worktree não encontrada para $CODE em $WT_PATH." >&2; exit 1; }

ISSUE="$(oc_resolve_issue "$CODE" || true)"
SPEC="$OC_TASKS/$TASK.md"
SUMMARY="$OC_TMP/$TASK-SUMMARY.md"

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"

BASE="$(git rev-parse "$TIP_REF" 2>/dev/null || git merge-base origin/main HEAD)"

echo "=== DOSSIÊ DE REVISÃO — Subtask ${TASK} (demanda ${CODE} · ${BRANCH}) ==="
echo ""
echo "--- 1a. ISSUE DA DEMANDA (resumo + lista de subtasks) ---"
if [[ -n "$ISSUE" ]]; then
  gh issue view "$ISSUE" --json number,title,body,labels \
    --jq '"#\(.number) · \(.title)\nLabels: \([.labels[].name] | join(", "))\n\n\(.body)"'
else
  echo "AVISO: Issue da demanda não encontrada no GitHub."
fi
echo ""
echo "--- 1b. SPEC DA SUBTASK (${TASK}) ---"
if [[ -f "$SPEC" ]]; then cat "$SPEC"; else echo "AVISO: spec ausente em $SPEC."; fi
echo ""
echo "--- 2. RESUMO DO CODIFICADOR (SUMMARY) ---"
if [[ -f "$SUMMARY" ]]; then cat "$SUMMARY"; else echo "AVISO: SUMMARY ausente — Codificador não finalizou a subtask."; fi
echo ""
echo "--- 3. RE-EXECUÇÃO DO TEST RUNNER (${OPENCODE_TEST_CMD}) ---"
TEST_LOG="$(mktemp)"
if bash -c "$OPENCODE_TEST_CMD" >"$TEST_LOG" 2>&1; then
  echo "RESULTADO: VERDE"
  tail -n 5 "$TEST_LOG" | tr -d '\r' || true
else
  echo "RESULTADO: VERMELHO — NÃO APROVE. Devolva ao Codificador."
  tail -n 30 "$TEST_LOG"
fi
rm -f "$TEST_LOG"
echo ""
echo "--- 4. DIFF DA SUBTASK (desde o tip: ${BASE:0:12}) ---"
git diff "$BASE"
echo ""
echo "=== FIM DO DOSSIÊ ==="
echo ">>> Audite em 4 camadas:"
echo ">>>   (A) aderência ao spec da subtask"
echo ">>>   (B) qualidade técnica conforme AGENTS.md"
echo ">>>   (C) critérios de aceitação observáveis"
echo ">>>   (D) coerência com padrões IMPLÍCITOS do projeto — explore 2-3 análogos por arquivo novo"
echo ">>> Cheque restrições de compliance declaradas em AGENTS.md (raiz)."
echo ">>> Decisão: approve ${TASK}  |  reject ${TASK} \"<motivo>\""
