#!/usr/bin/env bash
# create_issue.sh <US_CODE>
#
# Cria UMA GitHub Issue por demanda (US/FIX/REF/CHORE), contendo o RESUMO de tudo
# que será feito (visão geral + lista de subtasks). Não cria sub-issues — as
# subtasks são detalhadas em arquivos locais .opencode/tasks/<CODE>-<letra>.md
# (arquiteto fase 2) e implementadas numa única worktree/branch/PR.
#
# Uso:
#   bash .opencode/scripts/create_issue.sh <US_CODE>
#   ex.: bash .opencode/scripts/create_issue.sh US-042
#
# Pré-requisitos (metadados — título, label gh, prioridade):
#   Tier 1: .opencode/briefs/<CODE>.md          — escrito pelo scout
#   Tier 2: .opencode/briefs/<CODE>-slicing.md  — escrito pelo scout (quando não há <CODE>.md)
#   + .opencode/tmp/<CODE>-body.md              — escrito pelo arquiteto (fase 1) via Write tool
#
# Saída (stdout, 1 linha):
#   CREATED #<N> <URL>
#
# Exit codes:
#   0 sucesso | 64 uso | 65 pré-requisito ausente | 66 Issue OPEN já existe
#   67 metadado inválido no brief | 70 gh issue create falhou

set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

CODE="${1:-}"
if [[ -z "$CODE" ]]; then
  cat >&2 <<EOF
uso: $0 <US_CODE>
ex.:  $0 US-042
      $0 FIX-017

Pré-requisitos no disco:
  - .opencode/briefs/<CODE>.md      (escrito pelo scout)
  - .opencode/tmp/<CODE>-body.md    (escrito pelo arquiteto fase 1 via Write tool)
EOF
  exit 64
fi
CODE="$(oc_us_code "$CODE")"

BODY="$OC_TMP/$CODE-body.md"

# === Fonte de metadados: Tier 1 (<CODE>.md) ou Tier 2 (<CODE>-slicing.md) ===
if [[ -f "$OC_BRIEFS/$CODE.md" ]]; then
  BRIEF="$OC_BRIEFS/$CODE.md"          # Tier 1
elif [[ -f "$OC_BRIEFS/$CODE-slicing.md" ]]; then
  BRIEF="$OC_BRIEFS/$CODE-slicing.md"  # Tier 2 — slicing log carrega os metadados da demanda
else
  cat >&2 <<EOF
ERRO: nenhum brief encontrado para $CODE em $OC_BRIEFS
Esperado um de:
  - $OC_BRIEFS/$CODE.md          (Tier 1)
  - $OC_BRIEFS/$CODE-slicing.md  (Tier 2)
O scout precisa gerar o brief antes. Invoque:
  > Use o scout para preparar o brief de $CODE
EOF
  exit 65
fi

# === Idempotência ANTES de exigir body ===
EXISTING_OPEN="$(oc_resolve_issue "$CODE" || true)"
if [[ -n "$EXISTING_OPEN" ]]; then
  EXISTING_URL="$(gh issue view "$EXISTING_OPEN" --json url --jq .url 2>/dev/null || echo '<unknown>')"
  cat >&2 <<EOF
ABORTADO: Issue #$EXISTING_OPEN (OPEN) já existe para $CODE
URL: $EXISTING_URL

Se quiser recriar, feche/apague a Issue existente no GitHub primeiro e re-invoque.
Se for retry de uma criação que pareceu falhar mas sucedeu, apenas use esta Issue (#$EXISTING_OPEN).
EOF
  rm -f "$BODY"
  exit 66
fi

EXISTING_CLOSED="$(
  gh issue list --state closed --search "$CODE in:title" --json number,title \
    --jq ".[] | select(.title | test(\"^${CODE}[: ]\")) | .number" | tr '\n' ' '
)"
if [[ -n "${EXISTING_CLOSED// }" ]]; then
  echo "AVISO: Issues fechadas existem para $CODE: #${EXISTING_CLOSED// /, #} — criando nova mesmo assim." >&2
fi

if [[ ! -f "$BODY" ]]; then
  cat >&2 <<EOF
ERRO: body não encontrado em $BODY
O arquiteto (fase 1) precisa escrever o body via Write tool antes de invocar este script:
  Write tool:
    file_path: .opencode/tmp/$CODE-body.md
    content: <resumo da demanda: visão geral + lista de subtasks (a, b, c...)>
EOF
  exit 65
fi

# === Metadados do brief ===
# Aceita H1 do brief único ('# Brief — …') ou do slicing log ('# Slicing decision log — …').
RAW_H1="$(grep -m1 -E '^# (Brief|Slicing decision log) —' "$BRIEF" || true)"
if [[ -z "$RAW_H1" ]]; then
  echo "ERRO: $BRIEF não tem H1 no formato esperado '# Brief — <CODE> · <título>' ou '# Slicing decision log — <CODE> · <título>'" >&2
  exit 67
fi
TITLE_BODY="${RAW_H1#\# Brief — }"
TITLE_BODY="${TITLE_BODY#\# Slicing decision log — }"
TITLE_SUFFIX="${TITLE_BODY#${CODE} · }"
if [[ "$TITLE_SUFFIX" == "$TITLE_BODY" ]]; then
  echo "ERRO: H1 do brief não bate com CODE='$CODE'. H1 lido: '$RAW_H1'" >&2
  exit 67
fi
TITLE="${CODE}: ${TITLE_SUFFIX}"

LABEL_GH="$(
  grep -m1 -E '^- \*\*Label gh:\*\*' "$BRIEF" \
    | sed -E 's/.*\*\*Label gh:\*\*[[:space:]]*//' | tr -d '`' | awk '{print $1}'
)"
case "$LABEL_GH" in
  user-story|bug-fix|refactor|chore) ;;
  *)
    echo "ERRO: Label gh inválido extraído do brief: '$LABEL_GH'" >&2
    echo "       Esperado: user-story | bug-fix | refactor | chore" >&2
    exit 67 ;;
esac

PRIORITY_RAW="$(
  grep -m1 -E '^- \*\*Prioridade sugerida:\*\*' "$BRIEF" \
    | sed -E 's/.*\*\*Prioridade sugerida:\*\*[[:space:]]*//' | awk '{print $1}' \
    | tr -d '[]' | tr '[:upper:]' '[:lower:]'
)"
PRIORITY="${PRIORITY_RAW//média/media}"
case "$PRIORITY" in
  alta|media|baixa) ;;
  *) echo "AVISO: prioridade '$PRIORITY_RAW' inválida no brief — usando 'media' como default" >&2; PRIORITY="media" ;;
esac

# === Resolve label de prioridade contra labels reais do repo ===
PRIORITY_LABEL=""
ALL_LABELS="$(gh label list --limit 200 --json name --jq '.[].name' 2>/dev/null || echo '')"
if echo "$ALL_LABELS" | grep -qx "$PRIORITY"; then
  PRIORITY_LABEL="$PRIORITY"
elif [[ "$PRIORITY" == "media" ]] && echo "$ALL_LABELS" | grep -qx "média"; then
  PRIORITY_LABEL="média"
fi

LABEL_ARGS=("--label" "$LABEL_GH")
if [[ -n "$PRIORITY_LABEL" ]]; then
  LABEL_ARGS+=("--label" "$PRIORITY_LABEL")
  PRIORITY_DISPLAY="$PRIORITY_LABEL"
else
  PRIORITY_DISPLAY="(label '$PRIORITY' não existe no repo — omitido)"
fi

BODY_CHARS="$(wc -c < "$BODY" | tr -d ' ')"
BODY_LINES="$(wc -l < "$BODY" | tr -d ' ')"
cat >&2 <<EOF
Criando Issue (resumo da demanda) para $CODE:
  Título:    $TITLE
  Labels:    $LABEL_GH, $PRIORITY_DISPLAY
  Body:      $BODY ($BODY_CHARS chars, $BODY_LINES linhas)
  Brief:     $BRIEF
EOF

if ! URL="$(gh issue create --title "$TITLE" "${LABEL_ARGS[@]}" --body-file "$BODY")"; then
  echo "ERRO: gh issue create falhou. Body file preservado em $BODY para inspeção." >&2
  exit 70
fi

NUMBER="$(echo "$URL" | grep -oE '[0-9]+$' || true)"
if [[ -z "$NUMBER" ]]; then
  echo "ERRO: gh issue create retornou URL não-parseável: '$URL'" >&2
  echo "Body file preservado em $BODY para inspeção." >&2
  exit 70
fi

rm -f "$BODY"
echo "CREATED #$NUMBER $URL"
