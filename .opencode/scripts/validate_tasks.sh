#!/usr/bin/env bash
# validate_tasks.sh — Lint determinístico dos specs de subtask do arquiteto (fase 2).
#
# No fluxo novo, o arquiteto é invocado uma 2ª vez para detalhar a Issue em
# 1 arquivo por subtask, salvos em .opencode/tasks/<US_CODE>-<letra>.md
# (ou .opencode/tasks/<US_CODE>.md se a demanda tiver task única).
#
# Uso:
#   bash .opencode/scripts/validate_tasks.sh <US_CODE>
#   ex.:  bash .opencode/scripts/validate_tasks.sh US-042
#
# Saída:
#   exit 0  → todos os specs OK (resumo em stdout)
#   exit 1  → violações em stderr
#   exit 64 → uso incorreto
#   exit 65 → nenhum spec encontrado para o US_CODE
#
# Cheques por arquivo:
#   1. Filename — ^<US_CODE>(-[a-z])?\.md$
#   2. H2 obrigatórios: ## Objetivo / ## Passo a passo / ## Testes / ## Critérios de aceitação
#   3. Passo a passo com ≥ 1 item numerado (^[0-9]+\.)

set -uo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

CODE="${1:-}"
if [[ -z "$CODE" ]]; then
  echo "uso: $0 <US_CODE>   ex.: $0 US-042" >&2
  exit 64
fi
CODE="$(oc_us_code "$CODE")"

shopt -s nullglob
FILES=("$OC_TASKS/$CODE"-[a-z].md "$OC_TASKS/$CODE.md")
shopt -u nullglob

# Filter to only existing files (literal "$CODE.md" may not exist for Tier 2)
EXISTING_FILES=()
for candidate in "${FILES[@]}"; do
  [[ -f "$candidate" ]] && EXISTING_FILES+=("$candidate")
done

if (( ${#EXISTING_FILES[@]} == 0 )); then
  cat >&2 <<EOF
ERRO: nenhum spec de subtask encontrado para $CODE em $OC_TASKS/
O arquiteto (fase 2) precisa escrever: $OC_TASKS/${CODE}-a.md, ${CODE}-b.md, ...
(ou $OC_TASKS/${CODE}.md para task única).
EOF
  exit 65
fi

declare -a REQUIRED_H2=(
  "^## Objetivo"
  "^## Passo a passo"
  "^## Testes"
  "^## Critérios de aceitação"
)

TOTAL_VIOLATIONS=0
for f in "${EXISTING_FILES[@]}"; do
  base="$(basename "$f")"
  V=()

  if ! [[ "$base" =~ ^[A-Z]+-[0-9]+(-[a-z])?\.md$ ]]; then
    V+=("filename '$base' fora do padrão <US_CODE>-<letra>.md")
  fi
  for H2 in "${REQUIRED_H2[@]}"; do
    if ! grep -qE "$H2" "$f"; then
      HUMAN="$(echo "$H2" | sed -E 's/^\^//')"
      V+=("falta heading '$HUMAN'")
    fi
  done
  if ! grep -qE '^[0-9]+\.' "$f"; then
    V+=("'## Passo a passo' sem itens numerados (^1. ^2. ...)")
  fi

  LINES="$(wc -l < "$f" | tr -d ' ')"
  if (( ${#V[@]} == 0 )); then
    echo "✅ $base ($LINES linhas)"
  else
    TOTAL_VIOLATIONS=$(( TOTAL_VIOLATIONS + ${#V[@]} ))
    {
      echo "❌ $base:"
      for v in "${V[@]}"; do echo "   - $v"; done
    } >&2
  fi
done

if (( TOTAL_VIOLATIONS > 0 )); then
  echo "" >&2
  echo "Conserte os specs (via Edit) e re-rode validate_tasks $CODE até sair tudo ✅." >&2
  exit 1
fi

echo ""
echo ">>> ${#EXISTING_FILES[@]} spec(s) de subtask válidos para $CODE."
