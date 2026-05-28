#!/usr/bin/env bash
# validate_brief.sh — Lint determinístico para briefs produzidos pelo scout.
#
# Uso:
#   bash .qwen/scripts/validate_brief.sh <caminho>
#   ex.:  bash .qwen/scripts/validate_brief.sh .qwen/briefs/US-009.md
#
# Saída:
#   exit 0  → brief OK (mostra resumo em stdout)
#   exit 1  → violações listadas em stderr, scout deve editar e re-rodar
#   exit 64 → uso incorreto
#   exit 65 → arquivo não existe
#
# Cheques aplicados:
#   1. Filename em MAIÚSCULAS — regex ^[A-Z]+-[0-9]+(-[a-z])?(-slicing)?\.md$
#   2. H2 obrigatórios (apenas para briefs, não slicing log nem README):
#        ## Metadados / ## Demanda / ## 1. Contexto / ## 2. Objetivo /
#        ## 3. Critérios / ## 4. Observações / ## Checklist
#   3. Vocabulário PROIBIDO (case-insensitive):
#        "esperado/esperada/esperados", "deve seguir/conter/ser",
#        "implementar como", "criar com", "padrão a (seguir|usar)",
#        "stub no-op", "recomenda-se"
#   4. Checklist com ≥ 12 itens marcados (descarta versão antiga de 8 itens)
#
# Nota: `README.md` e `*-slicing.md` têm regras relaxadas (sem cheque de H2
# nem de checklist, pois seguem template diferente).

set -uo pipefail

BRIEF="${1:-}"
if [[ -z "$BRIEF" ]]; then
  echo "uso: $0 <caminho-do-brief>" >&2
  echo "ex.:  $0 .qwen/briefs/US-009.md" >&2
  exit 64
fi

if [[ ! -f "$BRIEF" ]]; then
  echo "ERRO: arquivo não encontrado: $BRIEF" >&2
  exit 65
fi

BASENAME="$(basename "$BRIEF")"
VIOLATIONS=()

# ─── 1. Filename MAIÚSCULAS ────────────────────────────────────────────────
# Permitidos:
#   - README.md (sempre OK)
#   - <CODE>.md                ex.: US-042.md, FIX-017.md
#   - <CODE>-<letra>.md        ex.: US-042-a.md
#   - <CODE>-slicing.md        ex.: US-042-slicing.md
if [[ "$BASENAME" != "README.md" ]]; then
  if ! [[ "$BASENAME" =~ ^[A-Z]+-[0-9]+(-[a-z])?(-slicing)?\.md$ ]]; then
    VIOLATIONS+=("filename: '$BASENAME' não está em MAIÚSCULAS (esperado: US-042.md / US-042-a.md / US-042-slicing.md). Renomeie via 'mv'.")
  fi
fi

# ─── 2. H2 obrigatórios (só para briefs principais) ────────────────────────
SKIP_STRUCTURE_CHECK=false
case "$BASENAME" in
  README.md|*-slicing.md) SKIP_STRUCTURE_CHECK=true ;;
esac

if ! $SKIP_STRUCTURE_CHECK; then
  declare -a REQUIRED_H2=(
    "^## Metadados$"
    "^## Demanda"
    "^## 1\. Contexto"
    "^## 2\. Objetivo"
    "^## 3\. Critérios"
    "^## 4\. Observações"
    "^## Checklist"
  )
  for H2 in "${REQUIRED_H2[@]}"; do
    if ! grep -qE "$H2" "$BRIEF"; then
      # Mensagem amigável (mostra o heading esperado sem ^ nem $)
      HUMAN="$(echo "$H2" | sed -E 's/^\^//; s/\$$//; s/\\\././g')"
      VIOLATIONS+=("estrutura: falta heading top-level '$HUMAN' (deve ser '##' direto, NÃO aninhado em outra seção)")
    fi
  done
fi

# ─── 3. Vocabulário PROIBIDO (case-insensitive) ────────────────────────────
# Regex juntando todas as proibições. Cada match vira 1 violação com linha.
# Filtros aplicados (todos excluem falsos positivos legítimos):
#   a) Linhas em blockquote (`>`) — preservam fala literal do humano (demanda
#      original, citações). Scout não controla o que o humano escreveu.
#   b) Linhas-meta que mencionam as palavras intencionalmente (cabeçalho
#      "Resultado funcional alvo", item de checklist "Zero vocabulário
#      proibido", seção "Vocabulário PROIBIDO" do prompt do scout).
PROHIBITED='esperad[ao]s?|deve seguir|deve conter|deve ser|deve usar|implementar como|criar com|padrão a (seguir|usar)|stub no-op|recomenda-se|novo arquivo com|método com assinatura'

HITS="$(grep -inE -- "$PROHIBITED" "$BRIEF" 2>/dev/null \
  | grep -vE "Resultado funcional alvo|Zero vocabulário proibido|PROIBIDO no brief|❌ NÃO escreva|vocabulário proibido aqui:" \
  | awk -F: '{
      # Reconstrói o conteúdo (pode ter ":" no texto, pega tudo após linha:)
      line_num = $1;
      content = $0;
      sub(/^[^:]+:/, "", content);
      # Remove leading whitespace
      gsub(/^[[:space:]]+/, "", content);
      # Skip se a linha começa com ">" (blockquote — fala do humano)
      if (content ~ /^>/) next;
      # Skip se a linha é dentro de um item de lista citando blockquote
      # (ex.: "  > sub-quote")
      if (content ~ /^[\*\-][[:space:]]+>/) next;
      print $0;
    }' \
  || true)"

if [[ -n "$HITS" ]]; then
  while IFS= read -r line; do
    VIOLATIONS+=("vocabulário proibido: $line")
  done <<< "$HITS"
fi

# ─── 4. Checklist com ≥ 12 itens (só para briefs principais) ───────────────
if ! $SKIP_STRUCTURE_CHECK; then
  N_CHECKS="$(grep -cE '^- \[x\]' "$BRIEF" 2>/dev/null || echo 0)"
  if (( N_CHECKS < 12 )); then
    VIOLATIONS+=("checklist: apenas $N_CHECKS itens marcados (esperado ≥ 12 — provavelmente está usando checklist antigo de 8 itens; veja §7-A do scout.md)")
  fi
fi

# ─── Saída ─────────────────────────────────────────────────────────────────
TOTAL="${#VIOLATIONS[@]}"
LINES="$(wc -l < "$BRIEF" | tr -d ' ')"
CHARS="$(wc -c < "$BRIEF" | tr -d ' ')"

if (( TOTAL == 0 )); then
  echo "✅ $BRIEF passou ($LINES linhas, $CHARS chars)"
  exit 0
fi

# Listar violações em stderr
{
  echo "❌ $BRIEF falhou com $TOTAL violação(ões):"
  for v in "${VIOLATIONS[@]}"; do
    echo "  - $v"
  done
  echo ""
  echo "Conserte o brief (via Edit) e re-rode este validate até sair com ✅."
} >&2

exit 1
