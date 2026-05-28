#!/usr/bin/env bash
#
# Cria uma GitHub Issue a partir de um brief preexistente + body gerado pelo arquiteto.
#
# Motivação:
#   Antes, o arquiteto compunha um heredoc bash inline:
#     cat > $BODY_FILE <<'EOF'
#     <corpo de ~8k tokens>
#     EOF
#     gh issue create --body-file "$BODY_FILE"
#   Isso forçava o modelo a regenerar o body inteiro dentro da bash command
#   (dobra de tokens), além de causar warnings de shell-mapping em bodies
#   grandes e ocasionalmente falhar/levar retry — observado em US-008-a que
#   levou 42 minutos no 27B.
#
#   Este script elimina o heredoc: o arquiteto escreve o body uma única vez
#   via Write tool em .qwen/tmp/<CODE>-body.md, depois invoca este script.
#
# Uso:
#   bash .qwen/scripts/create_issue.sh <CODE>
#   ex.: bash .qwen/scripts/create_issue.sh US-008-a
#
# Pré-requisitos:
#   1. .qwen/briefs/<CODE>.md — escrito pelo scout (fornece título, label gh, prioridade)
#   2. .qwen/tmp/<CODE>-body.md — escrito pelo arquiteto via Write tool (body da Issue)
#
# Saída em caso de sucesso (stdout, uma linha):
#   CREATED #<N> <URL>
#
# Códigos de erro (exit code):
#   0  — sucesso
#   64 — uso incorreto (sem argumento)
#   65 — pré-requisito ausente (brief ou body file)
#   66 — Issue já existe para esse CODE (idempotência)
#   67 — metadado inválido no brief (label gh ou prioridade malformados)
#   70 — gh issue create falhou (rede, auth, etc.)
#

set -euo pipefail

CODE="${1:-}"
if [[ -z "$CODE" ]]; then
  cat >&2 <<EOF
uso: $0 <CODE>
ex.:  $0 US-008-a
      $0 FIX-017

Pré-requisitos no disco:
  - .qwen/briefs/<CODE>.md      (escrito pelo scout)
  - .qwen/tmp/<CODE>-body.md    (escrito pelo arquiteto via Write tool)
EOF
  exit 64
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
BRIEF="$REPO_ROOT/.qwen/briefs/$CODE.md"
BODY="$REPO_ROOT/.qwen/tmp/$CODE-body.md"

# === Validações de pré-requisito ===

if [[ ! -f "$BRIEF" ]]; then
  cat >&2 <<EOF
ERRO: brief não encontrado em $BRIEF
O scout precisa gerar o brief antes. Invoque:
  > Use o scout para preparar o brief de $CODE
EOF
  exit 65
fi

# === Idempotência ANTES de exigir body: protege contra retry após "falsa falha" ===
# Comportamento:
#   - OPEN para este CODE     → BLOCKING (exit 66, evita duplicata viva)
#   - apenas CLOSED para CODE → AVISO + continua (retrabalho legítimo ou Issue cancelada)

EXISTING_OPEN="$(
  gh issue list --state open --search "$CODE in:title" \
    --json number,title \
    --jq ".[] | select(.title | test(\"^${CODE}[: ]\")) | .number" \
    | head -1
)"

if [[ -n "$EXISTING_OPEN" ]]; then
  EXISTING_URL="$(gh issue view "$EXISTING_OPEN" --json url --jq .url 2>/dev/null || echo '<unknown>')"
  cat >&2 <<EOF
ABORTADO: Issue #$EXISTING_OPEN (OPEN) já existe para $CODE
URL: $EXISTING_URL

Se quiser recriar, feche/apague a Issue existente no GitHub primeiro e re-invoque.
Se for retry de uma criação anterior que pareceu falhar mas sucedeu, apenas use esta Issue (#$EXISTING_OPEN).
EOF
  # Cleanup body file se ainda existir (lixo de tentativa anterior)
  rm -f "$BODY"
  exit 66
fi

# Aviso se existem Issues fechadas com mesmo CODE (não bloqueia)
EXISTING_CLOSED="$(
  gh issue list --state closed --search "$CODE in:title" \
    --json number,title \
    --jq ".[] | select(.title | test(\"^${CODE}[: ]\")) | .number" \
    | tr '\n' ' '
)"
if [[ -n "${EXISTING_CLOSED// }" ]]; then
  echo "AVISO: Issues fechadas existem para $CODE: #${EXISTING_CLOSED// /, #} — criando nova mesmo assim." >&2
fi

# === Agora exige body (só faz sentido se a Issue ainda não existe) ===

if [[ ! -f "$BODY" ]]; then
  cat >&2 <<EOF
ERRO: body não encontrado em $BODY
O arquiteto precisa escrever o body via Write tool antes de invocar este script:
  Write tool:
    file_path: .qwen/tmp/$CODE-body.md
    content: <corpo formatado da Issue, conforme template do arquiteto>
EOF
  exit 65
fi

# === Extração de metadados do brief ===

# Título — esperado em H1 no formato "# Brief — <CODE> · <título>"
RAW_H1="$(grep -m1 -E '^# Brief —' "$BRIEF" || true)"
if [[ -z "$RAW_H1" ]]; then
  echo "ERRO: brief não tem H1 no formato esperado '# Brief — <CODE> · <título>'" >&2
  exit 67
fi
# Remove "# Brief — " prefix
TITLE_BODY="${RAW_H1#\# Brief — }"
# Remove "<CODE> · " prefix → resta o título
TITLE_SUFFIX="${TITLE_BODY#${CODE} · }"
if [[ "$TITLE_SUFFIX" == "$TITLE_BODY" ]]; then
  echo "ERRO: H1 do brief não bate com CODE='$CODE'. H1 lido: '$RAW_H1'" >&2
  exit 67
fi
TITLE="${CODE}: ${TITLE_SUFFIX}"

# Label gh — esperado em linha "- **Label gh:** <label>"
LABEL_GH="$(
  grep -m1 -E '^- \*\*Label gh:\*\*' "$BRIEF" \
    | sed -E 's/.*\*\*Label gh:\*\*[[:space:]]*//' \
    | tr -d '`' \
    | awk '{print $1}'
)"
case "$LABEL_GH" in
  user-story|bug-fix|refactor|chore) ;;
  *)
    echo "ERRO: Label gh inválido extraído do brief: '$LABEL_GH'" >&2
    echo "       Esperado: user-story | bug-fix | refactor | chore" >&2
    exit 67
    ;;
esac

# Prioridade — esperado em linha "- **Prioridade sugerida:** <alta|media|baixa> ..."
PRIORITY_RAW="$(
  grep -m1 -E '^- \*\*Prioridade sugerida:\*\*' "$BRIEF" \
    | sed -E 's/.*\*\*Prioridade sugerida:\*\*[[:space:]]*//' \
    | awk '{print $1}' \
    | tr -d '[]' \
    | tr '[:upper:]' '[:lower:]'
)"
# Normaliza "média" → "media"
PRIORITY="${PRIORITY_RAW//média/media}"
case "$PRIORITY" in
  alta|media|baixa) ;;
  *)
    echo "AVISO: prioridade '$PRIORITY_RAW' inválida no brief — usando 'media' como default" >&2
    PRIORITY="media"
    ;;
esac

# === Resolve label de prioridade contra labels reais do repo ===
# (Repo do atrilha usa labels diretos: "alta", "baixa", "média". Não usa prefixo "priority:".)

PRIORITY_LABEL=""
ALL_LABELS="$(gh label list --limit 200 --json name --jq '.[].name' 2>/dev/null || echo '')"

# Tenta exato (alta/baixa)
if echo "$ALL_LABELS" | grep -qx "$PRIORITY"; then
  PRIORITY_LABEL="$PRIORITY"
# Tenta com acento para "media" (repo tem "média")
elif [[ "$PRIORITY" == "media" ]] && echo "$ALL_LABELS" | grep -qx "média"; then
  PRIORITY_LABEL="média"
fi

# === Monta args dinamicamente (label de prioridade só se existir no repo) ===

LABEL_ARGS=("--label" "$LABEL_GH")
if [[ -n "$PRIORITY_LABEL" ]]; then
  LABEL_ARGS+=("--label" "$PRIORITY_LABEL")
  PRIORITY_DISPLAY="$PRIORITY_LABEL"
else
  PRIORITY_DISPLAY="(label '$PRIORITY' não existe no repo — omitido)"
fi

# === Resumo antes de criar ===

BODY_CHARS="$(wc -c < "$BODY" | tr -d ' ')"
BODY_LINES="$(wc -l < "$BODY" | tr -d ' ')"
cat >&2 <<EOF
Criando Issue para $CODE:
  Título:    $TITLE
  Labels:    $LABEL_GH, $PRIORITY_DISPLAY
  Body:      $BODY ($BODY_CHARS chars, $BODY_LINES linhas)
  Brief:     $BRIEF
EOF

# === Cria a Issue ===

if ! URL="$(
  gh issue create \
    --title "$TITLE" \
    "${LABEL_ARGS[@]}" \
    --body-file "$BODY"
)"; then
  echo "ERRO: gh issue create falhou. Body file preservado em $BODY para inspeção." >&2
  echo "Saída do gh acima. Possíveis causas: rede, auth, label inexistente, body inválido." >&2
  exit 70
fi

NUMBER="$(echo "$URL" | grep -oE '[0-9]+$' || true)"
if [[ -z "$NUMBER" ]]; then
  echo "ERRO: gh issue create retornou URL não-parseável: '$URL'" >&2
  echo "Body file preservado em $BODY para inspeção." >&2
  exit 70
fi

# === Cleanup: apaga o body file (Issue é o contrato agora) ===

rm -f "$BODY"

# === Saída final (parseável: 1 linha, prefixo CREATED) ===

echo "CREATED #$NUMBER $URL"
