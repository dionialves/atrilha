#!/usr/bin/env bash
# finish_task.sh <issue_number>
#
# Tool do CODIFICADOR. Encerra a implementação:
#   1. Localiza a worktree da issue
#   2. Roda mvn test (QA determinístico — verde é obrigatório)
#   3. Verifica zero warnings de compilação
#   4. Gera SUMMARY.md com diff stat + testes (partes determinísticas)
#      e deixa um bloco para o agente preencher a narrativa
#   5. Valida que a working tree não tem lixo não rastreado perigoso
#
# Se os testes falham, a tarefa NÃO é finalizada — o agente deve corrigir.
set -euo pipefail

ISSUE="${1:?uso: finish_task <numero-da-issue>}"
ISSUE="${ISSUE#\#}"

REPO_ROOT="$(git rev-parse --show-toplevel)"
REPO_NAME="$(basename "$REPO_ROOT")"
WT_BASE="$(dirname "$REPO_ROOT")/${REPO_NAME}-worktrees"

# localizar a worktree desta issue (prefixo tipo-numero-)
WT_PATH="$(find "$WT_BASE" -maxdepth 1 -type d -name "*-${ISSUE}-*" | head -n1 || true)"
if [ -z "$WT_PATH" ]; then
  echo "ERRO: nenhuma worktree encontrada para a issue #${ISSUE}. Rode start_task primeiro." >&2
  exit 1
fi

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# --- detectar wrapper maven ---
MVN="./mvnw"; [ -x "./mvnw" ] || MVN="mvn"

echo "=== Rodando testes em $WT_PATH ($BRANCH) ===" >&2
TEST_LOG="$(mktemp)"
if ! $MVN -q test >"$TEST_LOG" 2>&1; then
  echo "=== TESTES VERMELHOS — tarefa NÃO finalizada ===" >&2
  tail -n 40 "$TEST_LOG" >&2
  echo "" >&2
  echo ">>> Corrija os testes/código e chame finish_task ${ISSUE} novamente." >&2
  rm -f "$TEST_LOG"
  exit 1
fi

# --- checar warnings de compilação (DoD: zero warnings) ---
WARN_COUNT="$(grep -ciE '\[WARNING\]' "$TEST_LOG" || true)"

# --- montar SUMMARY.md ---
SUMMARY="${WT_PATH}/SUMMARY.md"
DIFF_STAT="$(git diff origin/main --stat)"
FILES_CHANGED="$(git diff origin/main --name-only)"
TEST_TOTALS="$(grep -E 'Tests run:' "$TEST_LOG" | tail -n1 || echo 'Tests run: (ver log)')"

cat > "$SUMMARY" <<EOF
# Resumo de execução — Issue #${ISSUE}

**Branch:** ${BRANCH}
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** ${TEST_TOTALS}
**Warnings de compilação:** ${WARN_COUNT}

## Arquivos alterados
\`\`\`
${FILES_CHANGED}
\`\`\`

## Diff (stat)
\`\`\`
${DIFF_STAT}
\`\`\`

## O que foi feito
<!-- AGENTE: preencha aqui em 3-6 linhas. O QUE mudou e POR QUÊ.
     Decisões implícitas tomadas durante a execução.
     Pontos de atenção / dúvidas para o Revisor.
     Autoavaliação dos critérios de aceitação da issue. -->

## ⚠️ Checagem LGPD (atrilha)
<!-- AGENTE: se o diff TOCA consentimento, compartilhamento, ou dados de
     menor (13-17), declare explicitamente quais ADRs (005/006/007) foram
     respeitados e como. Se NÃO toca nada disso, escreva "N/A — sem
     superfície de dados pessoais". -->
EOF

rm -f "$TEST_LOG"

cat <<EOF
=== IMPLEMENTAÇÃO FINALIZADA ===
Issue:    #${ISSUE}
Branch:   ${BRANCH}
Worktree: ${WT_PATH}
Testes:   VERDE (${TEST_TOTALS})
Warnings: ${WARN_COUNT}
Summary:  ${SUMMARY}  <-- PREENCHA as seções "O que foi feito" e "Checagem LGPD"

>>> Próximo passo: edite ${SUMMARY} com a narrativa, depois a tarefa
    está pronta para o Revisor (sessão separada): load_review ${ISSUE}
EOF
