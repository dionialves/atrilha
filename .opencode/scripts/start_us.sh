#!/usr/bin/env bash
# start_us.sh <US_CODE>
#
# Tool do CODIFICADOR. Abre a ÚNICA worktree da demanda (modelo 1 US = 1 worktree
# = 1 branch = 1 PR) e prepara o terreno para implementar as subtasks uma a uma.
#
#   1. Resolve a Issue OPEN da demanda pelo código (título "<CODE>: ...").
#   2. Deriva tipo/slug/branch de forma determinística.
#   3. Cria a worktree em .opencode/worktrees/<US_CODE>/ (nomeada pelo CÓDIGO).
#   4. Cria a branch local a partir de origin/main (push só no fim, via open_pr).
#   5. Inicializa o tip ref (base do squash por subtask) em origin/main.
#   6. Devolve o corpo da Issue + lista dos specs de subtask.
#
# O LLM NUNCA monta git cru. Chama esta tool só com o código.
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

CODE="${1:?uso: start_us <US_CODE>   ex.: start_us US-042}"
CODE="$(oc_us_code "$(oc_strip_hash "$CODE")")"

command -v gh >/dev/null 2>&1 || { echo "ERRO: 'gh' CLI não encontrado." >&2; exit 1; }

ISSUE="$(oc_resolve_issue "$CODE" || true)"
if [[ -z "$ISSUE" ]]; then
  echo "ERRO: nenhuma Issue OPEN encontrada com título '$CODE: ...'. Crie via arquiteto/create_issue primeiro." >&2
  exit 1
fi

ISSUE_JSON="$(gh issue view "$ISSUE" --json number,title,body,labels,state)"
STATE="$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["state"])')"
[[ "$STATE" == "OPEN" ]] || { echo "ERRO: Issue #${ISSUE} não está aberta (estado: ${STATE})." >&2; exit 1; }

TITLE="$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["title"])')"
TYPE="$(oc_type_from_prefix "$CODE")" || { echo "ERRO: prefixo de código desconhecido em '$CODE' (use US|FIX|REF|CHORE)." >&2; exit 1; }
SLUG="$(oc_slug "$TITLE")"

BRANCH="${TYPE}/${ISSUE}-${SLUG}"
WT_PATH="$(oc_wt_path "$CODE")"
TIP_REF="$(oc_tip_ref "$CODE")"

git -C "$OC_REPO_ROOT" fetch origin main --quiet

if [[ -d "$WT_PATH" ]]; then
  echo "AVISO: worktree já existe, reutilizando: $WT_PATH" >&2
else
  mkdir -p "$OC_WT_BASE"
  if git -C "$OC_REPO_ROOT" show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git -C "$OC_REPO_ROOT" worktree add "$WT_PATH" "$BRANCH" --quiet
  else
    git -C "$OC_REPO_ROOT" worktree add -b "$BRANCH" "$WT_PATH" origin/main --quiet
  fi
fi

# Inicializa o tip ref (base do 1º squash) em origin/main, se ainda não existir.
if ! git -C "$OC_REPO_ROOT" show-ref --verify --quiet "$TIP_REF"; then
  git -C "$OC_REPO_ROOT" update-ref "$TIP_REF" "$(git -C "$OC_REPO_ROOT" rev-parse origin/main)"
fi

# Lista de specs de subtask (arquiteto fase 2).
shopt -s nullglob
TASK_FILES=("$OC_TASKS/$CODE"-[a-z].md "$OC_TASKS/$CODE.md")
shopt -u nullglob
if (( ${#TASK_FILES[@]} == 0 )); then
  TASK_LIST="(NENHUM spec encontrado em $OC_TASKS/ — invoque o arquiteto fase 2 para detalhar as subtasks antes de codificar)"
else
  TASK_LIST="$(for f in "${TASK_FILES[@]}"; do echo "  - $(basename "${f%.md}")  ($f)"; done)"
fi

cat <<EOF
=== DEMANDA PRONTA ===
Demanda:  $CODE  (Issue #${ISSUE} · ${TITLE})
Tipo:     ${TYPE}
Branch:   ${BRANCH}   (local; push só no fim via open_pr)
Worktree: ${WT_PATH}
Tip ref:  ${TIP_REF}  (base do squash da próxima subtask)

Subtasks a implementar (1 commit squashed por subtask, aprovado pelo Revisor):
${TASK_LIST}

>>> Trabalhe DENTRO de: ${WT_PATH}
>>> Implemente UMA subtask por vez, leia o spec em .opencode/tasks/<TASK_CODE>.md.
>>> Ao terminar UMA subtask, chame finish_task <TASK_CODE>   (ex.: finish_task ${CODE}-a)
>>> NÃO crie branch, NÃO faça push, NÃO crie PR.

=== CORPO DA ISSUE (resumo da demanda + lista de subtasks) ===
$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["body"])')
EOF
