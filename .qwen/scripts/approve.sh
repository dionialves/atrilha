#!/usr/bin/env bash
# approve.sh <issue_number>
#
# Tool do REVISOR — só deve ser chamada APÓS o veredito APROVADO.
# Executa a parte mecânica do fechamento (workflow.md §3.4 passo 6):
#   1. Re-valida testes verdes (trava de segurança)
#   2. Squash dos commits da branch em um único commit limpo
#   3. Push da branch
#   4. Cria PR como DRAFT via gh, com "Closes #<N>" no body
#
# O PR sai como DRAFT de propósito: o humano (Dioni) revisa, converte
# para ready e faz o merge. Modelo local aprova leniente demais.
set -euo pipefail

ISSUE="${1:?uso: approve <numero-da-issue>}"
ISSUE="${ISSUE#\#}"

REPO_ROOT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
WT_BASE="${REPO_ROOT}/.qwen/worktrees"

WT_PATH="$(find -L "$WT_BASE" -maxdepth 1 -type d -name "*-${ISSUE}-*" | head -n1 || true)"
[ -n "$WT_PATH" ] || { echo "ERRO: worktree da issue #${ISSUE} não encontrada." >&2; exit 1; }

cd "$WT_PATH"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
MVN="./mvnw"; [ -x "./mvnw" ] || MVN="mvn"

# --- trava de segurança: testes têm que estar verdes ---
echo "=== Validação final de testes antes de abrir PR ===" >&2
if ! $MVN -q test >/dev/null 2>&1; then
  echo "ERRO: suíte VERMELHA. PR não será criado. Devolva ao Codificador." >&2
  exit 1
fi

# --- derivar tipo/código a partir da branch ---
TYPE="${BRANCH%%/*}"                    # feat | fix | refactor | chore
case "$TYPE" in
  feat) CODE_PREFIX="us" ;;
  fix)  CODE_PREFIX="fix" ;;
  refactor) CODE_PREFIX="ref" ;;
  chore) CODE_PREFIX="chore" ;;
  *) echo "ERRO: tipo de branch desconhecido: $TYPE" >&2; exit 1 ;;
esac
ISSUE_TITLE="$(gh issue view "$ISSUE" --json title -q .title)"
TITLE_SLUG="$(python3 -c "
import sys, unicodedata, re
t = unicodedata.normalize('NFKD', sys.argv[1]).encode('ascii','ignore').decode()
print(re.sub(r'[^a-zA-Z0-9]+','-', t).strip('-').lower()[:50].rstrip('-'))
" "$ISSUE_TITLE")"
COMMIT_MSG="${TYPE}(${CODE_PREFIX}-${ISSUE}): ${TITLE_SLUG}"

# --- squash: reset soft até o ponto de origin/main e recommit único ---
git reset --soft "$(git merge-base origin/main HEAD)"
git add -A
git commit -m "$COMMIT_MSG" --quiet

# --- push ---
git push -u origin "$BRANCH" --quiet

# --- criar PR DRAFT ---
PR_BODY="$(cat <<EOF
Closes #${ISSUE}

Revisão automatizada concluída (Codificador → Revisor).
Testes: VERDE. PR aberto como **draft** para revisão final do humano.

Resumo de execução: ver SUMMARY.md na branch.
EOF
)"

PR_URL="$(gh pr create \
  --draft \
  --base main \
  --head "$BRANCH" \
  --title "${COMMIT_MSG}" \
  --body "$PR_BODY")"

cat <<EOF
=== PR DRAFT CRIADO ===
Issue:  #${ISSUE}
Branch: ${BRANCH}
Commit: ${COMMIT_MSG}
PR:     ${PR_URL}  (DRAFT)

>>> Dioni: revise o PR, converta para "Ready for review" e faça o merge.
>>> A Issue #${ISSUE} fecha automaticamente no merge (Closes #${ISSUE}).
EOF
