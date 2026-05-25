#!/usr/bin/env bash
# load_review.sh <issue_number>
#
# Tool do REVISOR. Monta o dossiê completo de auditoria:
#   1. Localiza a worktree da issue
#   2. Puxa a issue original (plano + critérios de aceitação)
#   3. Re-roda mvn test (Revisor NUNCA aprova sem testar)
#   4. Mostra o SUMMARY do Codificador
#   5. Mostra o diff completo contra main
#
# O Revisor lê tudo isto, audita em 3 camadas (plano / qualidade /
# critérios) e então chama approve OU reject. Esta tool é READ-ONLY.
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
MVN="./mvnw"; [ -x "./mvnw" ] || MVN="mvn"

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
echo "--- 3. RE-EXECUÇÃO DOS TESTES (mvn test) ---"
TEST_LOG="$(mktemp)"
if $MVN -q test >"$TEST_LOG" 2>&1; then
  echo "RESULTADO: VERDE"
  grep -E 'Tests run:' "$TEST_LOG" | tail -n1 || true
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
echo ">>> Audite em 3 camadas: (A) aderência ao plano, (B) qualidade técnica, (C) critérios de aceitação."
echo ">>> ATENÇÃO LGPD: se o diff toca consentimento/compartilhamento/dados de menor, verifique ADR-005/006/007."
echo ">>> Decisão: approve ${ISSUE}  |  reject ${ISSUE} \"<motivo>\""
