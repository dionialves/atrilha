#!/usr/bin/env bash
# start_task.sh <issue_number>
#
# Tool do CODIFICADOR. Faz o trabalho mecânico de Git que o LLM erra:
#   1. Valida que a issue existe no GitHub
#   2. Deriva tipo/slug/branch de forma determinística a partir das labels
#   3. Garante main atualizada
#   4. Cria a worktree isolada em ../<repo>-worktrees/<branch>
#   5. Devolve ao agente o corpo da issue + caminho da worktree
#
# O LLM NUNCA monta comandos git crus. Ele só chama esta tool com o número.
set -euo pipefail

ISSUE="${1:?uso: start_task <numero-da-issue>}"
ISSUE="${ISSUE#\#}"  # remove # se vier "#42"

# --- localizar raiz do repo ---
REPO_ROOT="$(git rev-parse --show-toplevel)"
REPO_NAME="$(basename "$REPO_ROOT")"
WT_BASE="$(dirname "$REPO_ROOT")/${REPO_NAME}-worktrees"

# --- validar issue e puxar dados em JSON ---
if ! command -v gh >/dev/null 2>&1; then
  echo "ERRO: 'gh' CLI não encontrado. Instale o GitHub CLI." >&2
  exit 1
fi

ISSUE_JSON="$(gh issue view "$ISSUE" --json number,title,body,labels,state 2>/dev/null)" || {
  echo "ERRO: Issue #${ISSUE} não encontrada no GitHub. Sem issue → sem código." >&2
  exit 1
}

STATE="$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["state"])')"
if [ "$STATE" != "OPEN" ]; then
  echo "ERRO: Issue #${ISSUE} não está aberta (estado: ${STATE})." >&2
  exit 1
fi

TITLE="$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["title"])')"
LABELS="$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(" ".join(l["name"] for l in json.load(sys.stdin)["labels"]))')"

# --- derivar tipo da branch a partir das labels (determinístico) ---
# Mapeia labels de user story (us-NNN) para o tipo correspondente
LABELS="$(echo "$LABELS" | sed -E 's/us-[0-9]+/user-story/g')"
case " $LABELS " in
  *" user-story "*) TYPE="feat" ;;
  *" bug-fix "*)    TYPE="fix" ;;
  *" refactor "*)   TYPE="refactor" ;;
  *" chore "*)      TYPE="chore" ;;
  *) echo "ERRO: Issue #${ISSUE} sem label de tipo (user-story|bug-fix|refactor|chore)." >&2; exit 1 ;;
esac

# --- slug kebab-case sem acentos, max 50 chars (transliteração robusta) ---
SLUG="$(python3 -c "
import sys, unicodedata, re
t = unicodedata.normalize('NFKD', sys.argv[1]).encode('ascii','ignore').decode()
print(re.sub(r'[^a-zA-Z0-9]+','-', t).strip('-').lower()[:50].rstrip('-'))
" "$TITLE")"

BRANCH="${TYPE}/${ISSUE}-${SLUG}"
WT_PATH="${WT_BASE}/${TYPE}-${ISSUE}-${SLUG}"

# --- atualizar main sem sair da worktree atual ---
git -C "$REPO_ROOT" fetch origin main --quiet

# --- criar worktree (idempotente: se já existe, reusa) ---
if [ -d "$WT_PATH" ]; then
  echo "AVISO: worktree já existe, reutilizando: $WT_PATH" >&2
else
  mkdir -p "$WT_BASE"
  # cria branch a partir de origin/main e já a coloca na worktree
  if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git -C "$REPO_ROOT" worktree add "$WT_PATH" "$BRANCH" --quiet
  else
    git -C "$REPO_ROOT" worktree add -b "$BRANCH" "$WT_PATH" origin/main --quiet
  fi
fi

# --- saída estruturada para o agente ---
cat <<EOF
=== TAREFA PRONTA ===
Issue:    #${ISSUE} · ${TITLE}
Tipo:     ${TYPE}   Labels: ${LABELS}
Branch:   ${BRANCH}
Worktree: ${WT_PATH}

>>> Trabalhe DENTRO de: ${WT_PATH}
>>> NÃO crie branches, NÃO faça push, NÃO crie PR.
>>> Ao terminar, chame finish_task ${ISSUE}

=== CORPO DA ISSUE (plano + critérios de aceitação) ===
$(echo "$ISSUE_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["body"])')
EOF
