#!/usr/bin/env bash
# finish_task.sh <issue_number>
#
# Tool do CODIFICADOR. Encerra a implementação:
#   1. Localiza a worktree da issue
#   2. Roda o test runner do projeto (auto-detectado via _project.sh ou QWEN_TEST_CMD)
#   3. Conta warnings (regex configurável)
#   4. Gera SUMMARY.md com diff stat + testes + blocos para narrativa/compliance
#
# Se os testes falham, a tarefa NÃO é finalizada.
set -euo pipefail

ISSUE="${1:?uso: finish_task <numero-da-issue>}"
ISSUE="${ISSUE#\#}"

REPO_ROOT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
WT_BASE="${REPO_ROOT}/.qwen/worktrees"

WT_PATH="$(find "$WT_BASE" -maxdepth 1 -type d -name "*-${ISSUE}-*" | head -n1 || true)"
if [ -z "$WT_PATH" ]; then
  echo "ERRO: nenhuma worktree encontrada para a issue #${ISSUE}. Rode start_task primeiro." >&2
  exit 1
fi

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# --- detectar comando de teste do projeto (env / AGENTS.md / auto-detect) ---
# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"

echo "=== Rodando testes em $WT_PATH ($BRANCH) ===" >&2
echo "=== Comando: $QWEN_TEST_CMD ===" >&2
TEST_LOG="$(mktemp)"
if ! bash -c "$QWEN_TEST_CMD" >"$TEST_LOG" 2>&1; then
  echo "=== TESTES VERMELHOS — tarefa NÃO finalizada ===" >&2
  tail -n 40 "$TEST_LOG" >&2
  echo "" >&2
  echo ">>> Corrija os testes/código e chame finish_task ${ISSUE} novamente." >&2
  rm -f "$TEST_LOG"
  exit 1
fi

# --- contar warnings (se regex foi definido) ---
if [[ -n "${QWEN_WARNINGS_REGEX:-}" ]]; then
  WARN_COUNT="$(grep -ciE -- "$QWEN_WARNINGS_REGEX" "$TEST_LOG" || true)"
else
  WARN_COUNT="(N/A — defina QWEN_WARNINGS_REGEX)"
fi

# --- detectar se o projeto exige checagem de compliance (marcador no AGENTS.md) ---
COMPLIANCE_REQUIRED=false
COMPLIANCE_LABEL="compliance"
if [[ -f "$REPO_ROOT/AGENTS.md" ]]; then
  if grep -q '<!-- *qwen:compliance-required *-->' "$REPO_ROOT/AGENTS.md" 2>/dev/null; then
    COMPLIANCE_REQUIRED=true
    COMPLIANCE_LABEL="$(grep -m1 -oE '<!-- *qwen:compliance-label: *[^>]*-->' "$REPO_ROOT/AGENTS.md" 2>/dev/null \
      | sed -E 's|<!-- *qwen:compliance-label: *(.*) *-->|\1|' | sed -E 's/ *$//' || true)"
    COMPLIANCE_LABEL="${COMPLIANCE_LABEL:-compliance}"
  fi
fi

# --- montar SUMMARY.md ---
SUMMARY="${WT_PATH}/SUMMARY.md"
DIFF_STAT="$(git diff origin/main --stat)"
FILES_CHANGED="$(git diff origin/main --name-only)"
TEST_TOTALS="$(tail -n 5 "$TEST_LOG" | tr -d '\r' || echo '(ver log)')"

{
  cat <<EOF
# Resumo de execução — Issue #${ISSUE}

**Branch:** ${BRANCH}
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Comando de teste:** \`${QWEN_TEST_CMD}\`
**Resultado:** VERDE
**Warnings:** ${WARN_COUNT}

## Arquivos alterados
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
<!-- AGENTE: preencha aqui em 3-6 linhas. O QUE mudou e POR QUÊ.
     Decisões implícitas tomadas durante a execução.
     Pontos de atenção / dúvidas para o Revisor.
     Autoavaliação dos critérios de aceitação da issue. -->
EOF

  if $COMPLIANCE_REQUIRED; then
    cat <<EOF

## ⚠️ Checagem de ${COMPLIANCE_LABEL}
<!-- AGENTE: declare como as restrições de ${COMPLIANCE_LABEL} listadas no
     AGENTS.md raiz foram respeitadas, OU escreva "N/A — sem superfície
     afetada" se o diff não toca nenhuma área relevante. Esta seção é
     obrigatória conforme AGENTS.md; sua ausência = reprovação automática
     do Revisor. -->
EOF
  fi
} > "$SUMMARY"

rm -f "$TEST_LOG"

EXTRA_HINT=""
if $COMPLIANCE_REQUIRED; then
  EXTRA_HINT=" e \"Checagem de ${COMPLIANCE_LABEL}\""
fi

cat <<EOF
=== IMPLEMENTAÇÃO FINALIZADA ===
Issue:    #${ISSUE}
Branch:   ${BRANCH}
Worktree: ${WT_PATH}
Testes:   VERDE (${QWEN_TEST_CMD})
Warnings: ${WARN_COUNT}
Summary:  ${SUMMARY}  <-- PREENCHA "O que foi feito"${EXTRA_HINT}

>>> Próximo passo: edite ${SUMMARY} com a narrativa, depois a tarefa
    está pronta para o Revisor (sessão separada): load_review ${ISSUE}
EOF
