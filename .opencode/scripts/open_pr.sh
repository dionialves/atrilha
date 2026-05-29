#!/usr/bin/env bash
# open_pr.sh <US_CODE>
#
# Tool do REVISOR — fecha a DEMANDA inteira (modelo 1 US = 1 PR).
# Só roda quando TODAS as subtasks foram aprovadas (cada uma já é 1 commit local).
#   1. Confere que cada spec .opencode/tasks/<CODE>-*.md tem commit aprovado.
#   2. Re-valida testes verdes (trava de segurança).
#   3. push da branch (1ª e única vez).
#   4. Abre UM PR draft com "Closes #<issue>".
#
# Draft de propósito: o humano revisa, converte para ready e faz o merge.
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

CODE="${1:?uso: open_pr <US_CODE>   ex.: open_pr US-042}"
CODE="$(oc_us_code "$(oc_strip_hash "$CODE")")"
WT_PATH="$(oc_wt_path "$CODE")"
CODE_LOWER="$(echo "$CODE" | tr '[:upper:]' '[:lower:]')"

[[ -d "$WT_PATH" ]] || { echo "ERRO: worktree não encontrada para $CODE em $WT_PATH." >&2; exit 1; }

ISSUE="$(oc_resolve_issue "$CODE" || true)"
[[ -n "$ISSUE" ]] || { echo "ERRO: Issue OPEN da demanda $CODE não encontrada." >&2; exit 1; }

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
MERGE_BASE="$(git merge-base origin/main HEAD)"

# --- exige que cada spec tenha commit aprovado ---
shopt -s nullglob
SPECS=("$OC_TASKS/$CODE"-[a-z].md "$OC_TASKS/$CODE.md")
shopt -u nullglob
(( ${#SPECS[@]} > 0 )) || { echo "ERRO: nenhum spec de subtask em $OC_TASKS/ para $CODE." >&2; exit 1; }

LOG_SUBJECTS="$(git log --pretty=%s "${MERGE_BASE}"..HEAD)"
MISSING=()
for spec in "${SPECS[@]}"; do
  tcode="$(basename "${spec%.md}")"           # US-042-a  ou  US-042
  letter="$(oc_task_letter "$tcode")"
  if [[ -n "$letter" ]]; then
    pat="(${CODE_LOWER}-${letter}):"
  else
    pat="(${CODE_LOWER}):"
  fi
  if ! grep -qF "$pat" <<<"$LOG_SUBJECTS"; then
    MISSING+=("$tcode")
  fi
done

if (( ${#MISSING[@]} > 0 )); then
  {
    echo "ERRO: subtasks sem commit aprovado — PR não será aberto:"
    for m in "${MISSING[@]}"; do echo "  - $m"; done
    echo "Implemente/aprove as faltantes (finish_task → load_review → approve) antes de open_pr."
  } >&2
  exit 1
fi

# --- trava de segurança: testes verdes ---
# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"
echo "=== Validação final de testes antes de abrir PR (${OPENCODE_TEST_CMD}) ===" >&2
if ! bash -c "$OPENCODE_TEST_CMD" >/dev/null 2>&1; then
  echo "ERRO: suíte VERMELHA. PR não será criado." >&2
  exit 1
fi

# --- push (1ª e única vez) ---
git push -u origin "$BRANCH" --quiet

# --- monta corpo do PR ---
ISSUE_TITLE="$(gh issue view "$ISSUE" --json title -q .title)"
COMMITS_LIST="$(git log --pretty='- %s' "${MERGE_BASE}"..HEAD)"
PR_BODY="$(cat <<EOF
Closes #${ISSUE}

Demanda **${CODE}** — ${#SPECS[@]} subtask(s), revisão automatizada concluída (Codificador → Revisor).
Testes: VERDE. PR aberto como **draft** para revisão final do humano.

### Subtasks (1 commit cada)
${COMMITS_LIST}
EOF
)"

PR_URL="$(gh pr create --draft --base main --head "$BRANCH" \
  --title "${ISSUE_TITLE}" --body "$PR_BODY")"

cat <<EOF
=== PR DRAFT CRIADO (demanda ${CODE}) ===
Issue:  #${ISSUE}
Branch: ${BRANCH}  (push feito)
PR:     ${PR_URL}  (DRAFT)
Subtasks no PR: ${#SPECS[@]}

>>> Humano: revise o PR, converta para "Ready for review" e faça o merge.
>>> A Issue #${ISSUE} fecha automaticamente no merge (Closes #${ISSUE}).
EOF
