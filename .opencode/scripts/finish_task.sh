#!/usr/bin/env bash
# finish_task.sh <TASK_CODE>
#
# Tool do CODIFICADOR. Encerra a implementação de UMA subtask:
#   1. Localiza a worktree da demanda (nomeada pelo US_CODE).
#   2. Confere que o spec da subtask existe em .opencode/tasks/<TASK_CODE>.md.
#   3. Roda o test runner do projeto (auto-detectado via _project.sh).
#   4. Conta warnings.
#   5. Gera SUMMARY da subtask em .opencode/tmp/<TASK_CODE>-SUMMARY.md
#      (fora da worktree, p/ não sujar o squash), com diff DESDE o tip ref.
#
# Testes vermelhos = subtask NÃO finalizada.
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "$0")/_common.sh"

TASK="${1:?uso: finish_task <TASK_CODE>   ex.: finish_task US-042-a}"
TASK="$(oc_strip_hash "$TASK")"
CODE="$(oc_us_code "$TASK")"
WT_PATH="$(oc_wt_path "$CODE")"
TIP_REF="$(oc_tip_ref "$CODE")"

[[ -d "$WT_PATH" ]] || { echo "ERRO: worktree não encontrada para $CODE em $WT_PATH. Rode start_us $CODE primeiro." >&2; exit 1; }

SPEC="$OC_TASKS/$TASK.md"
[[ -f "$SPEC" ]] || { echo "ERRO: spec da subtask não encontrado: $SPEC (arquiteto fase 2)." >&2; exit 1; }

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"

echo "=== Rodando testes da subtask $TASK em $WT_PATH ($BRANCH) ===" >&2
echo "=== Comando: $OPENCODE_TEST_CMD ===" >&2
TEST_LOG="$(mktemp)"
if ! bash -c "$OPENCODE_TEST_CMD" >"$TEST_LOG" 2>&1; then
  echo "=== TESTES VERMELHOS — subtask $TASK NÃO finalizada ===" >&2
  tail -n 40 "$TEST_LOG" >&2
  echo "" >&2
  echo ">>> Corrija e chame finish_task $TASK novamente." >&2
  rm -f "$TEST_LOG"
  exit 1
fi

if [[ -n "${OPENCODE_WARNINGS_REGEX:-}" ]]; then
  WARN_COUNT="$(grep -ciE -- "$OPENCODE_WARNINGS_REGEX" "$TEST_LOG" || true)"
else
  WARN_COUNT="(N/A — defina OPENCODE_WARNINGS_REGEX)"
fi

# --- compliance (marcador opencode: com fallback qwen:) ---
COMPLIANCE_REQUIRED=false
COMPLIANCE_LABEL="compliance"
if [[ -f "$OC_REPO_ROOT/AGENTS.md" ]]; then
  for prefix in opencode qwen; do
    if grep -q "<!-- *${prefix}:compliance-required *-->" "$OC_REPO_ROOT/AGENTS.md" 2>/dev/null; then
      COMPLIANCE_REQUIRED=true
      L="$(grep -m1 -oE "<!-- *${prefix}:compliance-label: *[^>]*-->" "$OC_REPO_ROOT/AGENTS.md" 2>/dev/null \
            | sed -E "s|<!-- *${prefix}:compliance-label: *(.*) *-->|\1|" | sed -E 's/ *$//' || true)"
      [[ -n "$L" ]] && COMPLIANCE_LABEL="$L"
      break
    fi
  done
fi

# --- SUMMARY da subtask (fora da worktree p/ não sujar o squash) ---
SUMMARY="$OC_TMP/$TASK-SUMMARY.md"
mkdir -p "$OC_TMP"
BASE="$(git rev-parse "$TIP_REF" 2>/dev/null || git merge-base origin/main HEAD)"
DIFF_STAT="$(git diff "$BASE" --stat)"
FILES_CHANGED="$(git diff "$BASE" --name-only)"
TEST_TOTALS="$(tail -n 5 "$TEST_LOG" | tr -d '\r' || echo '(ver log)')"

{
  cat <<EOF
# Resumo de execução — Subtask ${TASK} (Issue da demanda ${CODE})

**Branch:** ${BRANCH}
**Estado:** subtask pronta para revisão (sem PR, sem push)
**Spec:** ${SPEC}
**Comando de teste:** \`${OPENCODE_TEST_CMD}\`
**Resultado:** VERDE
**Warnings:** ${WARN_COUNT}

## Arquivos alterados (desde o tip da subtask anterior)
\`\`\`
${FILES_CHANGED}
\`\`\`

## Diff (stat)
\`\`\`
${DIFF_STAT}
\`\`\`

## Resumo do test runner
\`\`\`
${TEST_TOTALS}
\`\`\`

## O que foi feito
<!-- AGENTE: 3-6 linhas. O QUE mudou e POR QUÊ. Decisões implícitas.
     Autoavaliação dos critérios de aceitação do spec da subtask. -->
EOF

  if $COMPLIANCE_REQUIRED; then
    cat <<EOF

## ⚠️ Checagem de ${COMPLIANCE_LABEL}
<!-- AGENTE: declare como as restrições de ${COMPLIANCE_LABEL} do AGENTS.md raiz
     foram respeitadas, OU "N/A — sem superfície afetada". Obrigatória; ausência
     = reprovação automática do Revisor. -->
EOF
  fi
} > "$SUMMARY"

rm -f "$TEST_LOG"

EXTRA_HINT=""
$COMPLIANCE_REQUIRED && EXTRA_HINT=" e \"Checagem de ${COMPLIANCE_LABEL}\""

cat <<EOF
=== SUBTASK FINALIZADA ===
Subtask:  ${TASK}
Demanda:  ${CODE}
Branch:   ${BRANCH}
Testes:   VERDE (${OPENCODE_TEST_CMD})
Warnings: ${WARN_COUNT}
Summary:  ${SUMMARY}  <-- PREENCHA "O que foi feito"${EXTRA_HINT}

>>> Próximo: edite ${SUMMARY}, depois a subtask está pronta para o Revisor:
    load_review ${TASK}   →   approve ${TASK}  |  reject ${TASK} "<motivo>"
EOF
