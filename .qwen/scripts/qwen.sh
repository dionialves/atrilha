#!/usr/bin/env bash
#
# Wrapper que invoca o `qwen` com NODE_OPTIONS configurado para zerar os
# timeouts default do undici (5 min), que causam "Body Timeout Error" /
# "Client disconnected" em sessões longas com modelo local grande.
#
# Pré-requisito (rodar uma vez):
#   cd .qwen/scripts && npm init -y && npm install undici
#
# Uso (em vez de chamar `qwen` direto):
#   bash .qwen/scripts/qwen.sh                 # sessão interativa
#   bash .qwen/scripts/qwen.sh -p "minha pergunta"
#   bash .qwen/scripts/qwen.sh --bare ...      # qualquer flag do qwen
#
# Diagnóstico:
#   QWEN_DEBUG_PRELOAD=1 bash .qwen/scripts/qwen.sh ...
#   (imprime no stderr quando o dispatcher é patched)
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRELOAD="$HERE/no-undici-timeout.cjs"

if [[ ! -f "$PRELOAD" ]]; then
  echo "[qwen.sh] preload não encontrado: $PRELOAD" >&2
  exit 1
fi

if [[ ! -d "$HERE/node_modules/undici" ]]; then
  echo "[qwen.sh] undici não instalado em $HERE/node_modules" >&2
  echo "[qwen.sh] rode uma vez:" >&2
  echo "[qwen.sh]   cd $HERE && npm init -y && npm install undici" >&2
  echo "[qwen.sh] (node_modules/ já está em .qwen/.gitignore)" >&2
  exit 1
fi

# --require para preload + --preserve-symlinks por se asdf usar symlinks
export NODE_OPTIONS="${NODE_OPTIONS:-} --require $PRELOAD"

exec qwen "$@"
