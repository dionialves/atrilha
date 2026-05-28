#!/usr/bin/env bash
# load_review.sh <issue_number>
#
# Tool do REVISOR. Monta o dossiê completo de auditoria:
#   1. Localiza a worktree da issue
#   2. Puxa a issue original (plano + critérios de aceitação)
#   3. Re-roda o test runner do projeto (Revisor NUNCA aprova sem testar)
#   4. Mostra o SUMMARY do Codificador
#   5. Mostra o diff completo contra main
#
# READ-ONLY. Revisor audita e chama approve OU reject em seguida.
set -euo pipefail

ISSUE="${1:?uso: load_review <numero-da-issue>}"
ISSUE="${ISSUE#\#}"

REPO_ROOT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
WT_BASE="${REPO_ROOT}/.qwen/worktrees"

WT_PATH="$(find -L "$WT_BASE" -maxdepth 1 -type d -name "*-${ISSUE}-*" | head -n1 || true)"
if [ -z "$WT_PATH" ]; then
  echo "ERRO: nenhuma worktree para a issue #${ISSUE}." >&2
  exit 1
fi

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# shellcheck disable=SC1091
source "$(dirname "$0")/_project.sh"

echo "=== DOSSIÊ DE REVISÃO — Issue #${ISSUE} (${BRANCH}) ==="
echo ""
echo "--- 1. ISSUE ORIGINAL (plano + critérios de aceitação) ---"
gh issue view "$ISSUE" --json number,title,body,labels \
  --jq '"#\(.number) · \(.title)\nLabels: \([.labels[].name] | join(", "))\n\n\(.body)"'
echo ""
echo "--- 2. RESUMO DO CODIFICADOR (SUMMARY.md) ---"
if [ -f "${WT_PATH}/SUMMARY.md" ]; then
  cat "${WT_PATH}/SUMMARY.md"
else
  echo "AVISO: SUMMARY.md ausente — Codificador não finalizou corretamente."
fi
echo ""
echo "--- 3. RE-EXECUÇÃO DO TEST RUNNER (${QWEN_TEST_CMD}) ---"
TEST_LOG="$(mktemp)"
if bash -c "$QWEN_TEST_CMD" >"$TEST_LOG" 2>&1; then
  echo "RESULTADO: VERDE"
  tail -n 5 "$TEST_LOG" | tr -d '\r' || true
else
  echo "RESULTADO: VERMELHO — NÃO APROVE. Devolva ao Codificador."
  tail -n 30 "$TEST_LOG"
fi
rm -f "$TEST_LOG"
echo ""
echo "--- 4. DIFF COMPLETO vs origin/main ---"
git diff origin/main
echo ""
echo "=== FIM DO DOSSIÊ ==="
echo ">>> Audite em 4 camadas:"
echo ">>>   (A) aderência ao plano da Issue"
echo ">>>   (B) qualidade técnica conforme AGENTS.md"
echo ">>>   (C) critérios de aceitação observáveis"
echo ">>>   (D) coerência com padrões IMPLÍCITOS do projeto — explore 2-3 análogos por arquivo novo"
echo ">>>       (Grep/Glob/Read em ~5-15 chamadas; veja revisor.md §2 D)"
echo ">>> Cheque restrições de compliance declaradas em AGENTS.md (raiz)."
echo ">>> Decisão: approve ${ISSUE}  |  reject ${ISSUE} \"<motivo>\""
