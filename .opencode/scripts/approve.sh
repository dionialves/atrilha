#!/usr/bin/env bash
# approve.sh <TASK_CODE>
#
# Tool do REVISOR — só após veredito APROVADO de UMA subtask.
# Fechamento LOCAL (sem push, sem PR — isso é no fim, via open_pr):
#   1. Re-valida testes verdes (trava de segurança).
#   2. Squash dos commits WIP da subtask em 1 commit limpo, a partir do tip ref.
#   3. Avança o tip ref para o novo HEAD (base da próxima subtask).
#
# O push da branch e a abertura do PR único acontecem só quando TODAS as
# subtasks estiverem aprovadas — chame open_pr <US_CODE>.
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

TASK="${1:?uso: approve <TASK_CODE>   ex.: approve US-042-a}"
TASK="$(oc_strip_hash "$TASK")"
CODE="$(oc_us_code "$TASK")"
LETTER="$(oc_task_letter "$TASK")"
WT_PATH="$(oc_wt_path "$CODE")"
TIP_REF="$(oc_tip_ref "$CODE")"

[[ -d "$WT_PATH" ]] || { echo "ERRO: worktree não encontrada para $CODE em $WT_PATH." >&2; exit 1; }

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"

echo "=== Validação final de testes da subtask $TASK (${OPENCODE_TEST_CMD}) ===" >&2
if ! bash -c "$OPENCODE_TEST_CMD" >/dev/null 2>&1; then
  echo "ERRO: suíte VERMELHA. Subtask não será commitada. Devolva ao Codificador." >&2
  exit 1
fi

TYPE="$(oc_type_from_prefix "$CODE")" || { echo "ERRO: prefixo desconhecido em '$CODE'." >&2; exit 1; }
CODE_LOWER="$(echo "$CODE" | tr '[:upper:]' '[:lower:]')"

SPEC="$OC_TASKS/$TASK.md"
SPEC_TITLE="$(grep -m1 -E '^# ' "$SPEC" 2>/dev/null | sed -E 's/^# *//' || true)"
SLUG="$(oc_slug "${SPEC_TITLE:-$TASK}")"

if [[ -n "$LETTER" ]]; then
  COMMIT_MSG="${TYPE}(${CODE_LOWER}-${LETTER}): ${SLUG}"
else
  COMMIT_MSG="${TYPE}(${CODE_LOWER}): ${SLUG}"
fi

BASE="$(git rev-parse "$TIP_REF" 2>/dev/null || git merge-base origin/main HEAD)"

# Nada para commitar? (ex.: subtask sem diff) — aborta sem mexer no tip.
if git diff --quiet "$BASE" HEAD && git diff --quiet && git diff --cached --quiet; then
  echo "ERRO: nenhuma mudança desde o tip (${BASE:0:12}) para a subtask $TASK." >&2
  exit 1
fi

# Squash local: colapsa os WIP commits da subtask em 1 commit sobre o tip.
git reset --soft "$BASE"
git add -A
git commit -m "$COMMIT_MSG" --quiet

# Avança o tip ref para o commit recém-criado (base da próxima subtask).
git -C "$OC_REPO_ROOT" update-ref "$TIP_REF" "$(git rev-parse HEAD)"

# Limpa artefatos de revisão fora da worktree.
rm -f "$OC_TMP/$TASK-SUMMARY.md" "$OC_TMP/$TASK-REVIEW.md"

# Quantas subtasks faltam? (specs vs commits já aprovados na branch)
shopt -s nullglob
SPECS=("$OC_TASKS/$CODE"-[a-z].md "$OC_TASKS/$CODE.md")
shopt -u nullglob
TOTAL_SPECS=${#SPECS[@]}
APPROVED="$(git log --pretty=%s "$(git merge-base origin/main HEAD)"..HEAD \
  | grep -cE "\((${CODE_LOWER})(-[a-z])?\):" || true)"

cat <<EOF
=== SUBTASK APROVADA (commit local) ===
Subtask:  ${TASK}
Demanda:  ${CODE}
Branch:   ${BRANCH}   (NÃO foi feito push)
Commit:   ${COMMIT_MSG}
Tip ref:  ${TIP_REF} → $(git rev-parse --short HEAD)
Progresso: ${APPROVED}/${TOTAL_SPECS} subtask(s) aprovada(s).

EOF
if (( APPROVED >= TOTAL_SPECS && TOTAL_SPECS > 0 )); then
  echo ">>> TODAS as subtasks aprovadas. Feche a demanda: open_pr ${CODE}"
else
  echo ">>> Faltam subtasks. Codificador implementa a próxima; repita finish_task/load_review/approve."
fi
