#!/usr/bin/env bash
# _common.sh — helpers compartilhados pelo pipeline .opencode/.
#
# Modelo de trabalho (NOVO, difere do .qwen):
#   1 demanda (US/FIX/REF/CHORE)  =  1 worktree  =  1 branch  =  1 PR
#   - O worktree é nomeado pelo CÓDIGO da demanda:  .opencode/worktrees/<CODE>/
#   - A branch existe localmente desde o início (criada de origin/main).
#   - push + PR acontecem UMA única vez, no fim, via open_pr.sh.
#   - Cada subtask aprovada vira 1 commit squashed LOCAL (sem push).
#
# Identificadores:
#   US_CODE   — código da demanda                ex.: US-042, FIX-017, REF-003
#   TASK_CODE — subtask específica               ex.: US-042-a  (ou US-042 se task única)
#
# Convenções derivadas:
#   branch        <type>/<issue>-<slug>          ex.: feat/42-login-social
#   worktree      .opencode/worktrees/<US_CODE>  ex.: .opencode/worktrees/US-042
#   tip ref       refs/opencode/<us_code_lower>/tip   (base do squash por task)
#   commit/task   <type>(<us_code_lower>-<letter>): <slug>
#
# Source este arquivo nos demais scripts:
#   source "$(dirname "$0")/_common.sh"

set -uo pipefail

# Raiz do repositório principal (funciona mesmo chamado de dentro de uma worktree).
OC_REPO_ROOT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
OC_DIR="${OC_REPO_ROOT}/.opencode"
OC_BRIEFS="${OC_DIR}/briefs"
OC_TASKS="${OC_DIR}/tasks"
OC_TMP="${OC_DIR}/tmp"
OC_WT_BASE="${OC_DIR}/worktrees"

export OC_REPO_ROOT OC_DIR OC_BRIEFS OC_TASKS OC_TMP OC_WT_BASE

# Normaliza "#42" → "42".
oc_strip_hash() { printf '%s' "${1#\#}"; }

# US_CODE a partir de um TASK_CODE: remove sufixo "-<letra>" se houver.
#   US-042-a → US-042   |   US-042 → US-042   |   FIX-017-b → FIX-017
oc_us_code() { printf '%s' "$1" | sed -E 's/-[a-z]$//'; }

# Letra da subtask (vazio se for task única).  US-042-a → a   |   US-042 → ""
oc_task_letter() {
  local t="$1" base
  base="$(oc_us_code "$t")"
  if [[ "$t" != "$base" ]]; then printf '%s' "${t##*-}"; fi
}

# Prefixo do código (US|FIX|REF|CHORE), sempre MAIÚSCULO.
oc_prefix() { printf '%s' "${1%%-*}" | tr '[:lower:]' '[:upper:]'; }

# Mapeia prefixo → tipo de branch/commit. Retorna 1 se desconhecido.
oc_type_from_prefix() {
  case "$(oc_prefix "$1")" in
    US)    printf 'feat' ;;
    FIX)   printf 'fix' ;;
    REF)   printf 'refactor' ;;
    CHORE) printf 'chore' ;;
    *)     return 1 ;;
  esac
}

# Mapeia prefixo → label gh. Retorna 1 se desconhecido.
oc_label_from_prefix() {
  case "$(oc_prefix "$1")" in
    US)    printf 'user-story' ;;
    FIX)   printf 'bug-fix' ;;
    REF)   printf 'refactor' ;;
    CHORE) printf 'chore' ;;
    *)     return 1 ;;
  esac
}

# tip ref de uma demanda.  US-042 → refs/opencode/us-042/tip
oc_tip_ref() {
  local code; code="$(oc_us_code "$1" | tr '[:upper:]' '[:lower:]')"
  printf 'refs/opencode/%s/tip' "$code"
}

# Caminho da worktree de uma demanda.
oc_wt_path() { printf '%s/%s' "$OC_WT_BASE" "$(oc_us_code "$1")"; }

# Resolve o número da Issue OPEN cujo título começa com "<US_CODE>:" ou "<US_CODE> ".
# Echoa o número no stdout; retorna 1 e nada se não achar.
oc_resolve_issue() {
  local code="$1" n
  n="$(gh issue list --state open --search "$code in:title" --json number,title \
        --jq ".[] | select(.title | test(\"^${code}[: ]\")) | .number" 2>/dev/null | head -1)"
  [[ -n "$n" ]] || return 1
  printf '%s' "$n"
}

# Slug kebab-case sem acentos (≤ 50 chars). uso: oc_slug "Título da Coisa"
oc_slug() {
  python3 -c "
import sys, unicodedata, re
t = unicodedata.normalize('NFKD', sys.argv[1]).encode('ascii','ignore').decode()
print(re.sub(r'[^a-zA-Z0-9]+','-', t).strip('-').lower()[:50].rstrip('-'))
" "$1"
}
